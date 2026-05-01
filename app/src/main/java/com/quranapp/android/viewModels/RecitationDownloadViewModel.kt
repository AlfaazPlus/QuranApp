package com.quranapp.android.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import com.quranapp.android.R
import com.quranapp.android.api.models.mediaplayer.RecitationAudioKind
import com.quranapp.android.api.models.recitation2.RecitationModelBase
import com.quranapp.android.api.models.recitation2.RecitationQuranModel
import com.quranapp.android.api.models.recitation2.RecitationTranslationModel
import com.quranapp.android.compose.utils.DataLoadError
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.mediaplayer.RecitationAudioRepository
import com.quranapp.android.utils.mediaplayer.RecitationDownloadProgressBus
import com.quranapp.android.utils.mediaplayer.RecitationModelManager
import com.quranapp.android.utils.quran.QuranMeta
import com.quranapp.android.utils.receivers.NetworkStateReceiver
import com.quranapp.android.utils.workers.RecitationAudioDownloadWorker
import com.quranapp.android.utils.workers.RecitationBulkDownloadWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** WorkManager emits often during batching; debounce expensive per-reciter disk scans. */
private const val WORK_INFO_DEBOUNCE_MS = 300L

private const val RECITER_STATS_PARALLELISM = 4

private val WORK_ACTIVE_STATES = setOf(
    WorkInfo.State.RUNNING,
    WorkInfo.State.ENQUEUED,
    WorkInfo.State.BLOCKED,
)


private class ParsedWorkState(val infos: List<WorkInfo>) {
    val activeChapters = mutableMapOf<String, MutableSet<Int>>()
    val activeBulks = mutableSetOf<String>()
    val inProgressCounts = mutableMapOf<String, Int>()

    init {
        for (info in infos) {
            if (info.state !in WORK_ACTIVE_STATES) continue

            var reciterId: String? = null
            var chapterNo: Int? = null
            var isBulk = false
            var isAudio = false

            for (tag in info.tags) {
                if (tag == RecitationAudioDownloadWorker.TAG) {
                    isAudio = true
                }

                RecitationAudioDownloadWorker.parseChapterWorkTag(tag)?.let {
                    reciterId = it.first
                    chapterNo = it.second
                }

                RecitationBulkDownloadWorker.parseBulkReciterTag(tag)?.let {
                    reciterId = it.second
                    isBulk = true
                }
            }

            if (reciterId != null) {
                inProgressCounts[reciterId] = (inProgressCounts[reciterId] ?: 0) + 1

                if (isBulk) {
                    activeBulks.add(reciterId)
                } else if (chapterNo != null) {
                    activeChapters.getOrPut(reciterId) { mutableSetOf() }.add(chapterNo)
                }
            }

            if (isAudio && reciterId != null && chapterNo != null) {
                val p = info.progress
                val bytes = p.getLong(RecitationAudioDownloadWorker.KEY_PROGRESS_BYTES, -1L)
                val total = p.getLong(RecitationAudioDownloadWorker.KEY_PROGRESS_TOTAL, -1L)

                if (bytes >= 0L) {
                    RecitationDownloadProgressBus.set(reciterId, chapterNo, bytes, total)
                }
            }
        }
    }
}

data class RecitationBatchDownloadState(
    val downloadedCount: Int,
    val inProgressCount: Int,
    val totalChapters: Int = QuranMeta.chapterRange.last,
) {
    val isComplete: Boolean get() = downloadedCount >= totalChapters
    val hasActiveWork: Boolean get() = inProgressCount > 0
}

data class RecitationSelectedReciter(
    val kind: RecitationAudioKind,
    val id: String,
    val name: String,
)

data class RecitationChapterSheetData(
    val reciter: RecitationSelectedReciter,
    val downloadedChapters: Set<Int> = emptySet(),
    val activeChapters: Set<Int> = emptySet(),
    val bulkDownloadActive: Boolean = false,
)

data class RecitationDownloadUiState(
    val isLoading: Boolean = true,
    val quranReciters: List<RecitationQuranModel> = emptyList(),
    val translationReciters: List<RecitationTranslationModel> = emptyList(),
    val error: DataLoadError? = null,
    val downloadStates: Map<String, RecitationBatchDownloadState> = emptyMap(),
    val chapterSheet: RecitationChapterSheetData? = null,
)

sealed interface RecitationDownloadEvent {
    object Refresh : RecitationDownloadEvent
    data class StartDownload(val kind: RecitationAudioKind, val reciterId: String) :
        RecitationDownloadEvent

    data class CancelDownload(val kind: RecitationAudioKind, val reciterId: String) :
        RecitationDownloadEvent

    data class OpenChapterSheet(
        val kind: RecitationAudioKind,
        val reciterId: String,
        val name: String,
    ) : RecitationDownloadEvent

    object CloseChapterSheet : RecitationDownloadEvent

    data class DownloadChapter(
        val kind: RecitationAudioKind,
        val reciterId: String,
        val chapterNo: Int,
    ) : RecitationDownloadEvent

    data class CancelChapter(val reciterId: String, val chapterNo: Int) : RecitationDownloadEvent

    data class DeleteChapter(
        val kind: RecitationAudioKind,
        val reciterId: String,
        val chapterNo: Int,
    ) : RecitationDownloadEvent
}

sealed interface RecitationDownloadUiEvent {
    data class ShowMessage(val title: String, val message: String?) : RecitationDownloadUiEvent
}

@OptIn(FlowPreview::class)
class RecitationDownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>().applicationContext
    private val modelManager = RecitationModelManager.get(context)
    private val workManager = WorkManager.getInstance(context)

    private val recomputeMutex = Mutex()

    private val _uiState = MutableStateFlow(RecitationDownloadUiState())
    val uiState: StateFlow<RecitationDownloadUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<RecitationDownloadUiEvent>()
    val events = _events.asSharedFlow()

    private val downloadedCache = ConcurrentHashMap<String, Set<Int>>()

    init {
        viewModelScope.launch {
            loadReciters(forceRefresh = false)
        }

        viewModelScope.launch {
            combine(
                workManager.getWorkInfosByTagLiveData(RecitationAudioDownloadWorker.TAG).asFlow(),
                workManager.getWorkInfosByTagLiveData(RecitationBulkDownloadWorker.TAG).asFlow(),
                ::mergeWorkInfos,
            )
                .debounce(WORK_INFO_DEBOUNCE_MS)
                .collect { infos ->
                    recomputeStates(infos)
                }
        }
    }

    fun onEvent(event: RecitationDownloadEvent) {
        when (event) {
            is RecitationDownloadEvent.Refresh -> viewModelScope.launch {
                downloadedCache.clear()
                loadReciters(forceRefresh = true)
            }

            is RecitationDownloadEvent.StartDownload ->
                startBatchDownload(event.kind, event.reciterId)

            is RecitationDownloadEvent.CancelDownload ->
                cancelBatchDownload(event.kind, event.reciterId)

            is RecitationDownloadEvent.OpenChapterSheet -> {
                _uiState.update {
                    it.copy(
                        chapterSheet = RecitationChapterSheetData(
                            reciter = RecitationSelectedReciter(
                                kind = event.kind,
                                id = event.reciterId,
                                name = event.name,
                            ),
                        ),
                    )
                }
                viewModelScope.launch(Dispatchers.IO) {
                    triggerRecompute()
                }
            }

            is RecitationDownloadEvent.CloseChapterSheet -> {
                _uiState.update { it.copy(chapterSheet = null) }
            }

            is RecitationDownloadEvent.DownloadChapter ->
                downloadChapter(event.kind, event.reciterId, event.chapterNo)

            is RecitationDownloadEvent.CancelChapter ->
                cancelChapterDownload(event.reciterId, event.chapterNo)

            is RecitationDownloadEvent.DeleteChapter ->
                deleteChapter(event.kind, event.reciterId, event.chapterNo)
        }
    }

    private suspend fun loadReciters(forceRefresh: Boolean) {
        if (forceRefresh && !NetworkStateReceiver.isNetworkConnected(context)) {
            _uiState.update { it.copy(isLoading = false, error = DataLoadError.NoConnection) }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        try {
            if (forceRefresh) {
                modelManager.forceRefreshQuran = true
                modelManager.forceRefreshTranslation = true
            }

            val quran = modelManager.getAllQuranModel(forceRefresh = forceRefresh)
            val translation = modelManager.getAllTranslationModel(forceRefresh = forceRefresh)

            val qList = quran?.reciters.orEmpty()
            val tList = translation?.reciters.orEmpty()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    quranReciters = qList,
                    translationReciters = tList,
                    error = if (qList.isEmpty() && tList.isEmpty()) DataLoadError.Failed else null,
                )
            }

            triggerRecompute()
        } catch (e: Exception) {
            Log.saveError(e, "RecitationDownloadViewModel.loadReciters")
            _uiState.update {
                it.copy(isLoading = false, error = DataLoadError.Failed)
            }
        }
    }

    private fun recomputeStates(infos: List<WorkInfo>) {
        viewModelScope.launch(Dispatchers.Default) {
            recomputeMutex.withLock {
                val state = _uiState.value

                if (state.quranReciters.isEmpty() && state.translationReciters.isEmpty()) {
                    return@withLock
                }

                val parsedState = ParsedWorkState(infos)

                withContext(Dispatchers.IO) {
                    pruneRecitationProgressStaleEntries(parsedState)
                }

                val io = Dispatchers.IO.limitedParallelism(RECITER_STATS_PARALLELISM)

                coroutineScope {
                    val quranPairs = state.quranReciters.map { model ->
                        async(io) {
                            val key = stateKey(RecitationAudioKind.QURAN, model.id)
                            key to computeStateForReciter(
                                model.id,
                                key,
                                parsedState,
                            )
                        }
                    }

                    val transPairs = state.translationReciters.map { model ->
                        async(io) {
                            val key = stateKey(RecitationAudioKind.TRANSLATION, model.id)
                            key to computeStateForReciter(
                                model.id,
                                key,
                                parsedState,
                            )
                        }
                    }

                    val newMap = mutableMapOf<String, RecitationBatchDownloadState>()

                    quranPairs.forEach { deferred ->
                        val (k, v) = deferred.await()
                        newMap[k] = v
                    }

                    transPairs.forEach { deferred ->
                        val (k, v) = deferred.await()
                        newMap[k] = v
                    }

                    _uiState.update { prev ->
                        val refreshedSheet = prev.chapterSheet?.let { sheet ->
                            val r = sheet.reciter
                            val downloaded = getDownloadedChapters(r.id)
                            sheet.copy(
                                downloadedChapters = downloaded,
                                activeChapters = activeChaptersForReciter(
                                    r.id,
                                    parsedState,
                                    downloaded,
                                ),
                                bulkDownloadActive = parsedState.activeBulks.contains(r.id),
                            )
                        }
                        prev.copy(
                            downloadStates = newMap,
                            chapterSheet = refreshedSheet,
                        )
                    }
                }
            }
        }
    }


    private fun pruneRecitationProgressStaleEntries(parsedState: ParsedWorkState) {
        val activeChapterKeys = HashSet<String>()

        parsedState.activeChapters.forEach { (rid, chapters) ->
            chapters.forEach { ch ->
                activeChapterKeys.add(RecitationDownloadProgressBus.key(rid, ch))
            }
        }

        RecitationDownloadProgressBus.prune { key ->
            if (key in activeChapterKeys) return@prune false
            val (rid, ch) = RecitationDownloadProgressBus.parseBusKey(key) ?: return@prune true

            if (parsedState.activeBulks.contains(rid)) {
                if (ch !in getDownloadedChapters(rid)) {
                    return@prune false
                }
            }
            true
        }
    }

    private fun activeChaptersForReciter(
        reciterId: String,
        parsedState: ParsedWorkState,
        downloaded: Set<Int>,
    ): Set<Int> {
        if (parsedState.activeBulks.contains(reciterId)) {
            return QuranMeta.chapterRange.filter { it !in downloaded }.toSet()
        }
        return parsedState.activeChapters[reciterId] ?: emptySet()
    }

    private fun hasAnotherReciterActiveDownload(
        parsedState: ParsedWorkState,
        reciterId: String,
    ): Boolean {
        return parsedState.inProgressCounts.any { it.key != reciterId && it.value > 0 }
    }

    private fun computeStateForReciter(
        reciterId: String,
        stateKey: String,
        parsedState: ParsedWorkState,
    ): RecitationBatchDownloadState {
        val inProgress = parsedState.inProgressCounts[reciterId] ?: 0

        val prevState = _uiState.value.downloadStates[stateKey]
        val hadActiveWork = prevState?.hasActiveWork == true

        val forceRecheckDisk =
            hadActiveWork || inProgress > 0 || !downloadedCache.containsKey(reciterId)

        val downloaded = getDownloadedChapters(reciterId, forceRecheckDisk).size

        return RecitationBatchDownloadState(
            downloadedCount = downloaded,
            inProgressCount = inProgress,
        )
    }

    private fun getDownloadedChapters(reciterId: String, forceRecheck: Boolean = false): Set<Int> {
        if (forceRecheck) {
            val set = mutableSetOf<Int>()
            for (chapterNo in QuranMeta.chapterRange) {
                val f = modelManager.getRecitationAudioFile(reciterId, chapterNo)
                if (f.exists() && f.length() > 0L) {
                    set.add(chapterNo)
                }
            }
            downloadedCache[reciterId] = set
            return set
        }
        return downloadedCache.getOrPut(reciterId) {
            getDownloadedChapters(reciterId, forceRecheck = true)
        }
    }

    private fun downloadChapter(kind: RecitationAudioKind, reciterId: String, chapterNo: Int) {
        val model = findModel(kind, reciterId) ?: return

        if (!NetworkStateReceiver.canProceed(context)) {
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val infos = getMergedWorkInfos()

                val parsedState = ParsedWorkState(infos)

                if (hasAnotherReciterActiveDownload(parsedState, reciterId)) {
                    _events.emit(
                        RecitationDownloadUiEvent.ShowMessage(
                            title = context.getString(R.string.downloadRecitations),
                            message = context.getString(R.string.recitationDownloadWaitForOtherReciter),
                        ),
                    )
                    return@launch
                }

                if (parsedState.activeBulks.contains(reciterId)) {
                    return@launch
                }

                val url = RecitationAudioRepository.prepareAudioUrl(model.urlTemplate, chapterNo)
                    ?: return@launch

                val outputPath =
                    modelManager.getRecitationAudioFile(reciterId, chapterNo).absolutePath

                val request = RecitationAudioDownloadWorker.oneTimeRequest(
                    url = url,
                    outputPath = outputPath,
                    title = model.getReciterName(),
                    subtitle = context.getString(R.string.strTitleChapInfoChapterNo) + " $chapterNo",
                    reciterId = reciterId,
                    audioKind = kind,
                    chapterNo = chapterNo,
                )

                workManager.enqueueUniqueWork(
                    RecitationAudioDownloadWorker.uniqueWorkName(reciterId, chapterNo),
                    ExistingWorkPolicy.KEEP,
                    request,
                )
            } catch (e: Exception) {
                Log.saveError(e, "RecitationDownloadViewModel.downloadChapter")

                viewModelScope.launch {
                    _events.emit(
                        RecitationDownloadUiEvent.ShowMessage(
                            title = context.getString(R.string.strTitleFailed),
                            message = e.message,
                        ),
                    )
                }
            }
        }
    }

    private fun cancelChapterDownload(reciterId: String, chapterNo: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                workManager.cancelUniqueWork(
                    RecitationAudioDownloadWorker.uniqueWorkName(reciterId, chapterNo),
                )
                triggerRecompute()
            } catch (e: Exception) {
                Log.saveError(e, "RecitationDownloadViewModel.cancelChapterDownload")
            }
        }
    }

    private fun deleteChapter(
        @Suppress("UNUSED_PARAMETER") kind: RecitationAudioKind,
        reciterId: String,
        chapterNo: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val f = modelManager.getRecitationAudioFile(reciterId, chapterNo)
                if (f.exists()) {
                    f.delete()
                }
                downloadedCache.remove(reciterId)

                triggerRecompute()
            } catch (e: Exception) {
                Log.saveError(e, "RecitationDownloadViewModel.deleteChapter")
            }
        }
    }

    private fun startBatchDownload(kind: RecitationAudioKind, reciterId: String) {
        val model = findModel(kind, reciterId) ?: return

        if (!NetworkStateReceiver.canProceed(context)) {
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val infos = getMergedWorkInfos()

                val parsedState = ParsedWorkState(infos)
                val stateKey = stateKey(kind, reciterId)
                val myState = computeStateForReciter(reciterId, stateKey, parsedState)
                if (myState.hasActiveWork) return@launch

                if (hasAnotherReciterActiveDownload(parsedState, reciterId)) {
                    _events.emit(
                        RecitationDownloadUiEvent.ShowMessage(
                            title = context.getString(R.string.downloadRecitations),
                            message = context.getString(R.string.recitationDownloadWaitForOtherReciter),
                        ),
                    )
                    return@launch
                }

                val request = RecitationBulkDownloadWorker.oneTimeRequest(
                    reciterId = reciterId,
                    kind = kind,
                    urlTemplate = model.urlTemplate,
                    displayTitle = model.getReciterName(),
                )

                workManager.enqueueUniqueWork(
                    RecitationBulkDownloadWorker.uniqueWorkName(reciterId, kind),
                    ExistingWorkPolicy.KEEP,
                    request,
                )
            } catch (e: Exception) {
                Log.saveError(e, "RecitationDownloadViewModel.startBatchDownload")

                viewModelScope.launch {
                    _events.emit(
                        RecitationDownloadUiEvent.ShowMessage(
                            title = context.getString(R.string.strTitleFailed),
                            message = e.message,
                        ),
                    )
                }
            }
        }
    }

    private fun cancelBatchDownload(kind: RecitationAudioKind, reciterId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                workManager.cancelUniqueWork(
                    RecitationBulkDownloadWorker.uniqueWorkName(
                        reciterId,
                        kind
                    )
                )
                workManager.cancelAllWorkByTag(
                    RecitationAudioDownloadWorker.reciterTag(
                        reciterId,
                        kind
                    )
                )
                workManager.cancelAllWorkByTag(
                    RecitationBulkDownloadWorker.reciterTag(
                        reciterId,
                        kind
                    )
                )

                triggerRecompute()
            } catch (e: Exception) {
                Log.saveError(e, "RecitationDownloadViewModel.cancelBatchDownload")
            }
        }
    }

    private fun findModel(kind: RecitationAudioKind, reciterId: String): RecitationModelBase? {
        return when (kind) {
            RecitationAudioKind.QURAN ->
                _uiState.value.quranReciters.find { it.id == reciterId }

            RecitationAudioKind.TRANSLATION ->
                _uiState.value.translationReciters.find { it.id == reciterId }
        }
    }

    private suspend fun getMergedWorkInfos(): List<WorkInfo> {
        val audio = workManager.getWorkInfosByTag(RecitationAudioDownloadWorker.TAG).await()
        val bulk = workManager.getWorkInfosByTag(RecitationBulkDownloadWorker.TAG).await()
        return mergeWorkInfos(audio, bulk)
    }

    private suspend fun triggerRecompute() {
        recomputeStates(getMergedWorkInfos())
    }

    companion object {
        fun stateKey(kind: RecitationAudioKind, reciterId: String): String =
            "${kind.name}:$reciterId"
    }
}

private fun mergeWorkInfos(
    audio: List<WorkInfo>,
    bulk: List<WorkInfo>,
): List<WorkInfo> {
    val byId = LinkedHashMap<UUID, WorkInfo>()

    audio.forEach { byId[it.id] = it }
    bulk.forEach { byId[it.id] = it }

    return byId.values.toList()
}
