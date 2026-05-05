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
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.mediaplayer.WbwAudioDownloadProgressBus
import com.quranapp.android.utils.mediaplayer.WbwAudioRepository
import com.quranapp.android.utils.quran.QuranMeta
import com.quranapp.android.utils.receivers.NetworkStateReceiver
import com.quranapp.android.utils.workers.WbwAudioBulkDownloadWorker
import com.quranapp.android.utils.workers.WbwAudioDownloadWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
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
import java.util.UUID

private const val WORK_INFO_DEBOUNCE_MS = 300L

private val WORK_ACTIVE_STATES = setOf(
    WorkInfo.State.RUNNING,
    WorkInfo.State.ENQUEUED,
    WorkInfo.State.BLOCKED,
)

private class ParsedWbwWorkState(val infos: List<WorkInfo>) {
    val activeSingleChapters = mutableMapOf<String, MutableSet<Int>>()
    val activeBulks = mutableSetOf<String>()

    init {
        for (info in infos) {
            if (info.state !in WORK_ACTIVE_STATES) continue

            var bulkBusId: String? = null
            for (tag in info.tags) {
                WbwAudioBulkDownloadWorker.parseBulkTag(tag)?.let { bulkBusId = it }
            }

            if (bulkBusId != null) {
                activeBulks.add(bulkBusId)
                continue
            }

            var busId: String? = null
            var chapterNo: Int? = null

            for (tag in info.tags) {
                WbwAudioDownloadWorker.parseChapterWorkTag(tag)?.let { (bid, ch) ->
                    busId = bid
                    chapterNo = ch
                }
            }

            if (busId == null || chapterNo == null) continue

            activeSingleChapters.getOrPut(busId) { mutableSetOf() }.add(chapterNo)

            val p = info.progress
            val bytes = p.getLong(WbwAudioDownloadWorker.KEY_PROGRESS_BYTES, -1L)
            val total = p.getLong(WbwAudioDownloadWorker.KEY_PROGRESS_TOTAL, -1L)

            if (bytes >= 0L) {
                WbwAudioDownloadProgressBus.set(busId, chapterNo, bytes, total)
            }
        }
    }
}

data class WbwAudioDownloadUiState(
    val downloadedChapters: Set<Int> = emptySet(),
    val activeChapters: Set<Int> = emptySet(),
    val bulkDownloadActive: Boolean = false,
    val hasActiveSingleChapterWork: Boolean = false,
)

sealed interface WbwAudioDownloadUiEvent {
    data class ShowMessage(val message: String) : WbwAudioDownloadUiEvent
}

@OptIn(FlowPreview::class)
class WbwAudioDownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>().applicationContext
    private val workManager = WorkManager.getInstance(context)

    private val recomputeMutex = Mutex()

    private val _uiState = MutableStateFlow(WbwAudioDownloadUiState())
    val uiState: StateFlow<WbwAudioDownloadUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<WbwAudioDownloadUiEvent>()
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            combine(
                workManager.getWorkInfosByTagLiveData(WbwAudioDownloadWorker.TAG).asFlow(),
                workManager.getWorkInfosByTagLiveData(WbwAudioBulkDownloadWorker.TAG).asFlow(),
                ::mergeWorkInfos,
            )
                .debounce(WORK_INFO_DEBOUNCE_MS)
                .collect { infos ->
                    recomputeStates(infos)
                }
        }

        viewModelScope.launch {
            triggerRecompute()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            triggerRecompute()
        }
    }

    fun downloadChapter(audioId: String, chapterNo: Int) {
        if (!NetworkStateReceiver.canProceed(context)) {
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val infos = getMergedWorkInfos()
                val parsed = ParsedWbwWorkState(infos)

                if (parsed.activeBulks.contains(audioId)) {
                    emitBlockedDuringBulk()
                    return@launch
                }

                val url = WbwAudioRepository.prepareChapterAudioUrl(chapterNo) ?: return@launch
                val outputPath = WbwAudioRepository.getChapterAudioFile(context, chapterNo)
                    .absolutePath

                val request = WbwAudioDownloadWorker.oneTimeRequest(
                    url = url,
                    outputPath = outputPath,
                    chapterNo = chapterNo,
                    audioId = audioId,
                )

                workManager.enqueueUniqueWork(
                    WbwAudioDownloadWorker.uniqueWorkName(chapterNo),
                    ExistingWorkPolicy.KEEP,
                    request,
                )
            } catch (e: Exception) {
                Log.saveError(e, "WbwAudioDownloadViewModel.downloadChapter")
                _events.emit(
                    WbwAudioDownloadUiEvent.ShowMessage(
                        message = e.message ?: "Something went wrong!",
                    ),
                )
            }
        }
    }

    fun cancelChapter(chapterNo: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                workManager.cancelUniqueWork(WbwAudioDownloadWorker.uniqueWorkName(chapterNo))
                triggerRecompute()
            } catch (e: Exception) {
                Log.saveError(e, "WbwAudioDownloadViewModel.cancelChapter")
            }
        }
    }

    fun deleteChapter(chapterNo: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                WbwAudioRepository.deleteChapterAudio(context, chapterNo)
                triggerRecompute()
            } catch (e: Exception) {
                Log.saveError(e, "WbwAudioDownloadViewModel.deleteChapter")
            }
        }
    }

    fun startBulkDownload(audioId: String) {
        if (!NetworkStateReceiver.canProceed(context)) {
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val infos = getMergedWorkInfos()
                val parsed = ParsedWbwWorkState(infos)

                if (parsed.activeBulks.contains(audioId)) {
                    return@launch
                }

                val singles = parsed.activeSingleChapters[audioId]
                if (!singles.isNullOrEmpty()) {
                    _events.emit(
                        WbwAudioDownloadUiEvent.ShowMessage(
                            message = context.getString(R.string.wbwAudioBulkWaitForChapterDownloads),
                        ),
                    )
                    return@launch
                }

                val request = WbwAudioBulkDownloadWorker.oneTimeRequest(
                    audioId = audioId,
                )

                workManager.enqueueUniqueWork(
                    WbwAudioBulkDownloadWorker.uniqueWorkName(),
                    ExistingWorkPolicy.KEEP,
                    request,
                )
            } catch (e: Exception) {
                Log.saveError(e, "WbwAudioDownloadViewModel.startBulkDownload")
                _events.emit(
                    WbwAudioDownloadUiEvent.ShowMessage(
                        message = e.message ?: "Something went wrong!",
                    ),
                )
            }
        }
    }

    fun cancelBulkDownload(audioId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                workManager.cancelUniqueWork(WbwAudioBulkDownloadWorker.uniqueWorkName())
                workManager.cancelAllWorkByTag(WbwAudioBulkDownloadWorker.bulkTag(audioId))
                triggerRecompute()
            } catch (e: Exception) {
                Log.saveError(e, "WbwAudioDownloadViewModel.cancelBulkDownload")
            }
        }
    }

    private suspend fun emitBlockedDuringBulk() {
        _events.emit(
            WbwAudioDownloadUiEvent.ShowMessage(
                message = context.getString(R.string.wbwAudioChapterWaitForBulk),
            ),
        )
    }

    private fun recomputeStates(infos: List<WorkInfo>) {
        viewModelScope.launch(Dispatchers.IO) {
            recomputeMutex.withLock {
                val audioId = WbwAudioRepository.AUDIO_ID
                val parsed = ParsedWbwWorkState(infos)
                val downloaded = scanDownloadedChapters()

                pruneWbwProgressStaleEntries(audioId, parsed, downloaded)

                val bulkActive = parsed.activeBulks.contains(audioId)
                val activeSingles = parsed.activeSingleChapters[audioId] ?: emptySet()
                val activeChapters = if (bulkActive) {
                    QuranMeta.chapterRange.filter { it !in downloaded }.toSet()
                } else {
                    activeSingles
                }

                val hasSingle = parsed.activeSingleChapters[audioId]?.isNotEmpty() == true

                _uiState.update {
                    WbwAudioDownloadUiState(
                        downloadedChapters = downloaded,
                        activeChapters = activeChapters,
                        bulkDownloadActive = bulkActive,
                        hasActiveSingleChapterWork = hasSingle,
                    )
                }
            }
        }
    }

    private fun pruneWbwProgressStaleEntries(
        audioId: String,
        parsed: ParsedWbwWorkState,
        downloaded: Set<Int>,
    ) {
        val activeKeys = HashSet<String>()
        (parsed.activeSingleChapters[audioId] ?: emptySet()).forEach { ch ->
            activeKeys.add(WbwAudioDownloadProgressBus.key(audioId, ch))
        }

        WbwAudioDownloadProgressBus.prune { key ->
            if (key in activeKeys) return@prune false

            val (aId, ch) = WbwAudioDownloadProgressBus.parseBusKey(key) ?: return@prune true

            if (aId != audioId) return@prune true

            if (parsed.activeBulks.contains(aId)) {
                if (ch !in downloaded) {
                    return@prune false
                }
            }

            true
        }
    }

    private fun scanDownloadedChapters(): Set<Int> {
        val set = mutableSetOf<Int>()
        for (chapterNo in QuranMeta.chapterRange) {
            if (WbwAudioRepository.isChapterAudioDownloaded(context, chapterNo)) {
                set.add(chapterNo)
            }
        }
        return set
    }

    private suspend fun getMergedWorkInfos(): List<WorkInfo> {
        val audio = workManager.getWorkInfosByTag(WbwAudioDownloadWorker.TAG).await()
        val bulk = workManager.getWorkInfosByTag(WbwAudioBulkDownloadWorker.TAG).await()

        return mergeWorkInfos(audio, bulk)
    }

    private suspend fun triggerRecompute() {
        recomputeStates(getMergedWorkInfos())
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
