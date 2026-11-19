package com.quranapp.android.utils.workers

import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.recitationDownloadChaptersProgress
import com.quranapp.android.generated.resources.textDownloading
import com.quranapp.android.generated.resources.wbwAudio
import com.quranapp.android.task.ForegroundTaskInfo
import com.quranapp.android.task.TaskConstraints
import com.quranapp.android.task.TaskContext
import com.quranapp.android.task.TaskProgress
import com.quranapp.android.task.TaskRequest
import com.quranapp.android.task.TaskResult
import com.quranapp.android.task.TaskWorker
import com.quranapp.android.task.buildTaskParams
import com.quranapp.android.utils.app.AppFiles
import com.quranapp.android.utils.app.Logger
import com.quranapp.android.utils.mediaplayer.RecitationAudioFileDownloader
import com.quranapp.android.utils.mediaplayer.WbwAudioDownloadProgressBus
import com.quranapp.android.utils.mediaplayer.WbwAudioRepository
import com.quranapp.android.utils.quran.QuranMeta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.Path.Companion.toPath
import org.jetbrains.compose.resources.getString
import java.util.concurrent.atomic.AtomicInteger

class WbwAudioBulkDownloadWorker() : TaskWorker {

    override suspend fun doWork(context: TaskContext): TaskResult {
        val audioId = context.params.get<String>(KEY_AUDIO_ID) ?: return TaskResult.Failure()

        WbwAudioRepository.ensureTimingsAvailable()

        val pendingChapters = buildList {
            for (chapterNo in QuranMeta.chapterRange) {
                val audioFile = WbwAudioRepository.getChapterAudioFile(chapterNo)
                if (AppFiles.size(audioFile) > 0L) continue

                val url = WbwAudioRepository.prepareChapterAudioUrl(chapterNo) ?: continue

                add(
                    PendingChapter(
                        chapterNo = chapterNo,
                        url = url,
                        outputPath = audioFile.toString(),
                    ),
                )
            }
        }

        val total = pendingChapters.size
        context.setForeground(createEnqueueForegroundInfo(0, total.coerceAtLeast(1)))

        val completed = AtomicInteger(0)
        val limitedDispatcher = Dispatchers.IO.limitedParallelism(MAX_PARALLEL_DOWNLOADS)
        val notificationMutex = kotlinx.coroutines.sync.Mutex()
        var lastNotificationTime = 0L

        suspend fun updateForeground(done: Int) {
            notificationMutex.withLock {
                val now = System.currentTimeMillis()
                if (now - lastNotificationTime >= 2000L || done == total) {
                    lastNotificationTime = now
                    context.setForeground(createEnqueueForegroundInfo(done, total))
                    context.setProgress(TaskProgress.of(done, total.coerceAtLeast(1)))
                }
            }
        }

        return try {
            withContext(limitedDispatcher) {
                pendingChapters.map { pending ->
                    async {
                        currentCoroutineContext().ensureActive()
                        if (context.isCancelled) return@async

                        val outputFile = pending.outputPath.toPath()

                        try {
                            WbwAudioDownloadProgressBus.set(
                                audioId,
                                pending.chapterNo,
                                0L,
                                -1L,
                            )

                            RecitationAudioFileDownloader.downloadToFile(
                                pending.url,
                                outputFile,
                            ) { consumed, totalBytes ->
                                WbwAudioDownloadProgressBus.set(
                                    audioId,
                                    pending.chapterNo,
                                    consumed,
                                    totalBytes,
                                )
                            }
                        } catch (e: Exception) {
                            AppFiles.delete(outputFile)
                            Logger.saveError(e, "WbwAudioBulkDownload: ${pending.chapterNo}")
                        } finally {
                            WbwAudioDownloadProgressBus.clear(audioId, pending.chapterNo)
                        }

                        updateForeground(completed.incrementAndGet())
                    }
                }.awaitAll()
            }

            updateForeground(completed.get())
            TaskResult.Success()
        } catch (e: Exception) {
            TaskResult.Failure()
        }
    }

    private suspend fun createEnqueueForegroundInfo(
        completedCount: Int,
        chapterTotal: Int,
    ): ForegroundTaskInfo {
        val displayTitle = getString(Res.string.wbwAudio)
        val max = chapterTotal.coerceAtLeast(1)
        return ForegroundTaskInfo(
            notificationId = displayTitle.hashCode(),
            title = displayTitle,
            subText = getString(Res.string.textDownloading),
            body = getString(
                Res.string.recitationDownloadChaptersProgress,
                completedCount,
                chapterTotal,
            ),
            progressCurrent = completedCount.coerceAtMost(max),
            progressTotal = max,
            indeterminate = false,
        )
    }

    private data class PendingChapter(
        val chapterNo: Int,
        val url: String,
        val outputPath: String,
    )

    companion object {
        const val TAG = "wbw_audio_bulk_download"
        private const val BULK_TAG_PREFIX = "wbw_audio_bulk:"

        private const val MAX_PARALLEL_DOWNLOADS = 6

        private const val KEY_AUDIO_ID = "wbw_bulk_audio_id"

        fun uniqueWorkName(): String = "wbw-audio-bulk-all"

        fun bulkTag(audioId: String): String = "$BULK_TAG_PREFIX$audioId"

        fun parseBulkTag(tag: String): String? {
            if (!tag.startsWith(BULK_TAG_PREFIX)) return null
            return tag.removePrefix(BULK_TAG_PREFIX).takeIf { it.isNotBlank() }
        }

        fun oneTimeRequest(audioId: String): TaskRequest {
            return TaskRequest(
                workerClass = WbwAudioBulkDownloadWorker::class,
                params = buildTaskParams { put(KEY_AUDIO_ID, audioId) },
                constraints = TaskConstraints.NETWORK,
                tags = setOf(TAG, bulkTag(audioId)),
            )
        }
    }
}
