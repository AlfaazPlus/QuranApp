package com.quranapp.android.utils.workers

import com.quranapp.android.api.models.mediaplayer.RecitationAudioKind
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.recitationDownloadChaptersProgress
import com.quranapp.android.generated.resources.textDownloading
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
import com.quranapp.android.utils.mediaplayer.RecitationAudioRepository
import com.quranapp.android.utils.mediaplayer.RecitationDownloadProgressBus
import com.quranapp.android.utils.mediaplayer.RecitationModelManager
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

class RecitationBulkDownloadWorker() : TaskWorker {

    override suspend fun doWork(context: TaskContext): TaskResult {
        val reciterId = context.params.get<String>(KEY_RECITER_ID)
        val kindName = context.params.get<String>(KEY_KIND)
        val kind = try {
            RecitationAudioKind.valueOf(kindName ?: "")
        } catch (_: Exception) {
            null
        }

        val urlTemplate = context.params.get<String>(KEY_URL_TEMPLATE)
        val displayTitle = context.params.get<String>(KEY_DISPLAY_TITLE).orEmpty().ifBlank { "Recitation" }

        if (reciterId == null || kind == null || urlTemplate == null) {
            return TaskResult.Failure()
        }

        val modelManager = RecitationModelManager

        val pendingChapters = buildList {
            for (chapterNo in QuranMeta.chapterRange) {
                val audioFile = modelManager.getRecitationAudioFile(reciterId, chapterNo)

                if (AppFiles.size(audioFile) > 0L) continue

                val url = RecitationAudioRepository.prepareAudioUrl(urlTemplate, chapterNo)
                    ?: continue

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
        context.setForeground(
            createEnqueueForegroundInfo(
                displayTitle,
                0,
                total.coerceAtLeast(1),
            ),
        )

        val completed = AtomicInteger(0)
        val limitedDispatcher = Dispatchers.IO.limitedParallelism(MAX_PARALLEL_DOWNLOADS)
        val notificationMutex = kotlinx.coroutines.sync.Mutex()
        var lastNotificationTime = 0L

        suspend fun updateForeground(done: Int) {
            notificationMutex.withLock {
                val now = System.currentTimeMillis()

                if (now - lastNotificationTime >= 2000L || done == total) {
                    lastNotificationTime = now
                    context.setForeground(createEnqueueForegroundInfo(displayTitle, done, total))
                    context.setProgress(TaskProgress.of(done, total.coerceAtLeast(1)))
                }
            }
        }

        try {
            withContext(limitedDispatcher) {
                pendingChapters.map { pending ->
                    async {
                        currentCoroutineContext().ensureActive()
                        if (context.isCancelled) return@async

                        val outputFile = pending.outputPath.toPath()

                        try {
                            RecitationDownloadProgressBus.set(
                                reciterId,
                                pending.chapterNo,
                                0L,
                                -1L,
                            )
                            RecitationAudioFileDownloader.downloadToFile(
                                pending.url,
                                outputFile,
                            ) { consumed, totalBytes ->
                                RecitationDownloadProgressBus.set(
                                    reciterId,
                                    pending.chapterNo,
                                    consumed,
                                    totalBytes,
                                )
                            }
                        } catch (e: Exception) {
                            AppFiles.delete(outputFile)
                            Logger.saveError(e, "RecitationBulkDownload: ${pending.chapterNo}")
                        } finally {
                            RecitationDownloadProgressBus.clear(reciterId, pending.chapterNo)
                        }

                        updateForeground(completed.incrementAndGet())
                    }
                }.awaitAll()
            }

            updateForeground(completed.get())

            return TaskResult.Success()
        } catch (e: Exception) {
            return TaskResult.Failure()
        }
    }

    private suspend fun createEnqueueForegroundInfo(
        displayTitle: String,
        completedCount: Int,
        chapterTotal: Int,
    ): ForegroundTaskInfo {
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
        const val TAG = "recitation_bulk_download"
        private const val BULK_TAG_PREFIX = "recitation_bulk_reciter:"

        private const val MAX_PARALLEL_DOWNLOADS = 6

        private const val KEY_RECITER_ID = "bulk_reciter_id"
        private const val KEY_KIND = "bulk_kind"
        private const val KEY_URL_TEMPLATE = "bulk_url_template"
        private const val KEY_DISPLAY_TITLE = "bulk_display_title"

        const val KEY_BULK_PROGRESS_CHAPTER_NO = "bulk_progress_chapter_no"
        const val KEY_BULK_PROGRESS_RECITER_ID = "bulk_progress_reciter_id"

        fun uniqueWorkName(reciterId: String, kind: RecitationAudioKind): String {
            return "recitation-bulk:$reciterId:${kind.name}"
        }

        fun reciterTag(reciterId: String, kind: RecitationAudioKind): String {
            return "$BULK_TAG_PREFIX${kind.name}:$reciterId"
        }

        fun parseBulkReciterTag(tag: String): Pair<RecitationAudioKind, String>? {
            if (!tag.startsWith(BULK_TAG_PREFIX)) return null

            val rest = tag.removePrefix(BULK_TAG_PREFIX)
            val idx = rest.indexOf(':')

            if (idx <= 0 || idx >= rest.length - 1) return null
            val kindName = rest.substring(0, idx)
            val reciterId = rest.substring(idx + 1)

            val kind = try {
                RecitationAudioKind.valueOf(kindName)
            } catch (_: Exception) {
                return null
            }

            return kind to reciterId
        }

        fun oneTimeRequest(
            reciterId: String,
            kind: RecitationAudioKind,
            urlTemplate: String,
            displayTitle: String,
        ): TaskRequest {
            val params = buildTaskParams {
                put(KEY_RECITER_ID, reciterId)
                put(KEY_KIND, kind.name)
                put(KEY_URL_TEMPLATE, urlTemplate)
                put(KEY_DISPLAY_TITLE, displayTitle)
            }

            return TaskRequest(
                workerClass = RecitationBulkDownloadWorker::class,
                params = params,
                constraints = TaskConstraints.NETWORK,
                tags = setOf(TAG, reciterTag(reciterId, kind)),
            )
        }
    }
}
