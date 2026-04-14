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
import com.quranapp.android.utils.mediaplayer.RecitationBulkDownloadNotificationHelper
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

/** WorkManager emits often during batching; debounce expensive per-reciter disk scans. */
private const val WORK_INFO_DEBOUNCE_MS = 300L

private const val RECITER_STATS_PARALLELISM = 4

data class RecitationBatchDownloadState(
    val downloadedCount: Int,
    val inProgressCount: Int,
    val totalChapters: Int = QuranMeta.chapterRange.last,
) {
    val isComplete: Boolean get() = downloadedCount >= totalChapters
    val hasActiveWork: Boolean get() = inProgressCount > 0
}

data class RecitationDownloadUiState(
    val isLoading: Boolean = true,
    val quranReciters: List<RecitationQuranModel> = emptyList(),
    val translationReciters: List<RecitationTranslationModel> = emptyList(),
    val error: DataLoadError? = null,
    val downloadStates: Map<String, RecitationBatchDownloadState> = emptyMap(),
)

sealed interface RecitationDownloadEvent {
    object Refresh : RecitationDownloadEvent
    data class StartDownload(val kind: RecitationAudioKind, val reciterId: String) :
        RecitationDownloadEvent

    data class CancelDownload(val kind: RecitationAudioKind, val reciterId: String) :
        RecitationDownloadEvent
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
                loadReciters(forceRefresh = true)
            }

            is RecitationDownloadEvent.StartDownload ->
                startBatchDownload(event.kind, event.reciterId)

            is RecitationDownloadEvent.CancelDownload ->
                cancelBatchDownload(event.kind, event.reciterId)
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

            recomputeStates(
                withContext(Dispatchers.IO) {
                    mergeWorkInfos(
                        workManager.getWorkInfosByTag(RecitationAudioDownloadWorker.TAG).await(),
                        workManager.getWorkInfosByTag(RecitationBulkDownloadWorker.TAG).await(),
                    )
                }
            )
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

                val io = Dispatchers.IO.limitedParallelism(RECITER_STATS_PARALLELISM)

                coroutineScope {
                    val quranPairs = state.quranReciters.map { model ->
                        async(io) {
                            val key = stateKey(RecitationAudioKind.QURAN, model.id)
                            key to computeStateForReciter(
                                model.id,
                                RecitationAudioKind.QURAN,
                                infos,
                            )
                        }
                    }
                    val transPairs = state.translationReciters.map { model ->
                        async(io) {
                            val key = stateKey(RecitationAudioKind.TRANSLATION, model.id)
                            key to computeStateForReciter(
                                model.id,
                                RecitationAudioKind.TRANSLATION,
                                infos,
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
                    _uiState.update { it.copy(downloadStates = newMap) }
                }
            }
        }
    }

    private suspend fun hasAnotherReciterActiveDownload(
        infos: List<WorkInfo>,
        kind: RecitationAudioKind,
        reciterId: String,
    ): Boolean {
        val myKey = stateKey(kind, reciterId)
        val state = _uiState.value

        for (m in state.quranReciters) {
            val k = stateKey(RecitationAudioKind.QURAN, m.id)
            if (k == myKey) continue

            if (computeStateForReciter(m.id, RecitationAudioKind.QURAN, infos).hasActiveWork) {
                return true
            }
        }

        for (m in state.translationReciters) {
            val k = stateKey(RecitationAudioKind.TRANSLATION, m.id)
            if (k == myKey) continue
            if (computeStateForReciter(
                    m.id,
                    RecitationAudioKind.TRANSLATION,
                    infos
                ).hasActiveWork
            ) {
                return true
            }
        }

        return false
    }

    private suspend fun computeStateForReciter(
        reciterId: String,
        kind: RecitationAudioKind,
        infos: List<WorkInfo>,
    ): RecitationBatchDownloadState = withContext(Dispatchers.IO) {
        val downloaded = countDownloadedChapters(reciterId)
        val chapterTag = RecitationAudioDownloadWorker.reciterTag(reciterId, kind)
        val bulkTag = RecitationBulkDownloadWorker.reciterTag(reciterId, kind)
        val myWorks = infos.filter {
            it.tags.contains(chapterTag) || it.tags.contains(bulkTag)
        }

        val inProgress = myWorks.count {
            it.state == WorkInfo.State.RUNNING ||
                    it.state == WorkInfo.State.ENQUEUED ||
                    it.state == WorkInfo.State.BLOCKED
        }

        RecitationBatchDownloadState(
            downloadedCount = downloaded,
            inProgressCount = inProgress,
        )
    }

    private fun countDownloadedChapters(reciterId: String): Int {
        var count = 0
        for (chapterNo in QuranMeta.chapterRange) {
            val f = modelManager.getRecitationAudioFile(reciterId, chapterNo)
            if (f.exists() && f.length() > 0L) count++
        }
        return count
    }

    private fun startBatchDownload(kind: RecitationAudioKind, reciterId: String) {
        val model = findModel(kind, reciterId) ?: return

        if (!NetworkStateReceiver.canProceed(context)) {
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val infos = mergeWorkInfos(
                    workManager.getWorkInfosByTag(RecitationAudioDownloadWorker.TAG).await(),
                    workManager.getWorkInfosByTag(RecitationBulkDownloadWorker.TAG).await(),
                )

                val myState = computeStateForReciter(reciterId, kind, infos)
                if (myState.hasActiveWork) return@launch

                if (hasAnotherReciterActiveDownload(infos, kind, reciterId)) {
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
                RecitationBulkDownloadNotificationHelper.cancelAllBulkWork(context, reciterId, kind)
                val audio = workManager
                    .getWorkInfosByTag(RecitationAudioDownloadWorker.TAG)
                    .await()
                val bulk = workManager
                    .getWorkInfosByTag(RecitationBulkDownloadWorker.TAG)
                    .await()

                recomputeStates(mergeWorkInfos(audio, bulk))
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
