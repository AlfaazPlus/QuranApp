package com.quranapp.android.viewmodel

import androidx.lifecycle.viewModelScope
import com.quranapp.android.extensions.canProceed
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.wbwAudioBulkWaitForChapterDownloads
import com.quranapp.android.generated.resources.wbwAudioChapterWaitForBulk
import com.quranapp.android.platform.PlatformServices
import com.quranapp.android.task.ExistingTaskPolicy
import com.quranapp.android.task.TaskState
import com.quranapp.android.utils.app.Logger
import com.quranapp.android.utils.mediaplayer.WbwAudioDownloadProgressBus
import com.quranapp.android.utils.mediaplayer.WbwAudioRepository
import com.quranapp.android.utils.quran.QuranMeta
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.getString
import kotlin.time.Duration.Companion.milliseconds

private val WORK_INFO_DEBOUNCE_MS = 300.milliseconds

private class ParsedWbwWorkState {
    val activeSingleChapters = mutableMapOf<String, MutableSet<Int>>()
    val activeBulks = mutableSetOf<String>()
}

private fun TaskState.isActive(): Boolean = isEnqueued || isRunning

private suspend fun loadParsedWbwWorkState(audioId: String): ParsedWbwWorkState {
    val tm = PlatformServices.system.taskManager
    val parsed = ParsedWbwWorkState()

    val bulkActive = tm.infosByTag(WbwAudioBulkDownloadWorker.bulkTag(audioId)).first()
        .any { it.state.isActive() }
    if (bulkActive) {
        parsed.activeBulks.add(audioId)
    }

    WbwAudioDownloadProgressBus.state.value.keys.forEach { key ->
        val (busId, chapterNo) = WbwAudioDownloadProgressBus.parseBusKey(key) ?: return@forEach
        if (busId == audioId) {
            parsed.activeSingleChapters.getOrPut(audioId) { mutableSetOf() }.add(chapterNo)
        }
    }

    return parsed
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
class WbwAudioDownloadViewModel() : AppViewModel() {

    private val taskManager get() = PlatformServices.system.taskManager

    private val recomputeMutex = Mutex()

    private val _uiState = MutableStateFlow(WbwAudioDownloadUiState())
    val uiState: StateFlow<WbwAudioDownloadUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<WbwAudioDownloadUiEvent>()
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            combine(
                taskManager.isRunningByTag(WbwAudioDownloadWorker.TAG),
                taskManager.isRunningByTag(WbwAudioBulkDownloadWorker.TAG),
                WbwAudioDownloadProgressBus.state,
            ) { _, _, _ -> }
                .debounce(WORK_INFO_DEBOUNCE_MS)
                .collect {
                    triggerRecompute()
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
        viewModelScope.launch {
            if (!PlatformServices.networkMonitor.canProceed()) return@launch

            launch(Dispatchers.IO) {
                try {
                    val parsed = loadParsedWbwWorkState(WbwAudioRepository.AUDIO_ID)

                    if (parsed.activeBulks.contains(audioId)) {
                        emitBlockedDuringBulk()
                        return@launch
                    }

                    val url = WbwAudioRepository.prepareChapterAudioUrl(chapterNo) ?: return@launch
                    val outputPath = WbwAudioRepository.getChapterAudioFile(chapterNo).toString()

                    val request = WbwAudioDownloadWorker.oneTimeRequest(
                        url = url,
                        outputPath = outputPath,
                        chapterNo = chapterNo,
                        audioId = audioId,
                    )

                    taskManager.enqueueUnique(
                        WbwAudioDownloadWorker.uniqueWorkName(chapterNo),
                        ExistingTaskPolicy.KEEP,
                        request,
                    )
                } catch (e: Exception) {
                    Logger.saveError(e, "WbwAudioDownloadViewModel.downloadChapter")
                    _events.emit(
                        WbwAudioDownloadUiEvent.ShowMessage(
                            message = e.message ?: "Something went wrong!",
                        ),
                    )
                }
            }
        }
    }

    fun cancelChapter(chapterNo: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                taskManager.cancelUnique(WbwAudioDownloadWorker.uniqueWorkName(chapterNo))
                triggerRecompute()
            } catch (e: Exception) {
                Logger.saveError(e, "WbwAudioDownloadViewModel.cancelChapter")
            }
        }
    }

    fun deleteChapter(chapterNo: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                WbwAudioRepository.deleteChapterAudio(chapterNo)
                triggerRecompute()
            } catch (e: Exception) {
                Logger.saveError(e, "WbwAudioDownloadViewModel.deleteChapter")
            }
        }
    }

    fun startBulkDownload(audioId: String) {
        viewModelScope.launch {
            if (!PlatformServices.networkMonitor.canProceed()) return@launch

            launch(Dispatchers.IO) {
                try {
                    val parsed = loadParsedWbwWorkState(WbwAudioRepository.AUDIO_ID)

                    if (parsed.activeBulks.contains(audioId)) {
                        return@launch
                    }

                    val singles = parsed.activeSingleChapters[audioId]
                    if (!singles.isNullOrEmpty()) {
                        _events.emit(
                            WbwAudioDownloadUiEvent.ShowMessage(
                                message = getString(Res.string.wbwAudioBulkWaitForChapterDownloads),
                            ),
                        )
                        return@launch
                    }

                    val request = WbwAudioBulkDownloadWorker.oneTimeRequest(
                        audioId = audioId,
                    )

                    taskManager.enqueueUnique(
                        WbwAudioBulkDownloadWorker.uniqueWorkName(),
                        ExistingTaskPolicy.KEEP,
                        request,
                    )
                } catch (e: Exception) {
                    Logger.saveError(e, "WbwAudioDownloadViewModel.startBulkDownload")
                    _events.emit(
                        WbwAudioDownloadUiEvent.ShowMessage(
                            message = e.message ?: "Something went wrong!",
                        ),
                    )
                }
            }
        }
    }

    fun cancelBulkDownload(audioId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                taskManager.cancelUnique(WbwAudioBulkDownloadWorker.uniqueWorkName())
                taskManager.cancelByTag(WbwAudioBulkDownloadWorker.bulkTag(audioId))
                triggerRecompute()
            } catch (e: Exception) {
                Logger.saveError(e, "WbwAudioDownloadViewModel.cancelBulkDownload")
            }
        }
    }

    private suspend fun emitBlockedDuringBulk() {
        _events.emit(
            WbwAudioDownloadUiEvent.ShowMessage(
                message = getString(Res.string.wbwAudioChapterWaitForBulk),
            ),
        )
    }

    private fun recomputeStates() {
        viewModelScope.launch(Dispatchers.IO) {
            recomputeMutex.withLock {
                val audioId = WbwAudioRepository.AUDIO_ID
                val parsed = loadParsedWbwWorkState(audioId)
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
            if (WbwAudioRepository.isChapterAudioDownloaded(chapterNo)) {
                set.add(chapterNo)
            }
        }
        return set
    }

    private suspend fun triggerRecompute() {
        recomputeStates()
    }
}
