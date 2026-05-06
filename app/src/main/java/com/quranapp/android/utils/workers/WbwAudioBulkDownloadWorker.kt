package com.quranapp.android.utils.workers

import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.quranapp.android.R
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.app.NotificationUtils
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
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class WbwAudioBulkDownloadWorker(
    private val ctx: Context,
    params: WorkerParameters,
) : CoroutineWorker(ctx, params) {
    private val cancelPendingIntent =
        WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)

    override suspend fun getForegroundInfo(): ForegroundInfo =
        createEnqueueForegroundInfo(0, 1)

    override suspend fun doWork(): Result {
        val audioId = inputData.getString(KEY_AUDIO_ID) ?: return Result.failure()

        val app = applicationContext
        WbwAudioRepository.ensureTimingsAvailable(app)

        val pendingChapters = buildList {
            for (chapterNo in QuranMeta.chapterRange) {
                val audioFile = WbwAudioRepository.getChapterAudioFile(app, chapterNo)
                if (audioFile.exists() && audioFile.length() > 0L) continue

                val url = WbwAudioRepository.prepareChapterAudioUrl(chapterNo) ?: continue

                add(
                    PendingChapter(
                        chapterNo = chapterNo,
                        url = url,
                        outputPath = audioFile.absolutePath,
                    ),
                )
            }
        }

        val total = pendingChapters.size
        setForeground(createEnqueueForegroundInfo(0, total.coerceAtLeast(1)))

        val completed = AtomicInteger(0)
        val limitedDispatcher = Dispatchers.IO.limitedParallelism(MAX_PARALLEL_DOWNLOADS)
        val notificationMutex = kotlinx.coroutines.sync.Mutex()
        var lastNotificationTime = 0L

        suspend fun updateForeground(done: Int) {
            notificationMutex.withLock {
                val now = System.currentTimeMillis()
                if (now - lastNotificationTime >= 2000L || done == total) {
                    lastNotificationTime = now
                    setForeground(createEnqueueForegroundInfo(done, total))
                }
            }
        }

        return try {
            withContext(limitedDispatcher) {
                pendingChapters.map { pending ->
                    async {
                        currentCoroutineContext().ensureActive()

                        val outputFile = File(pending.outputPath)
                        val parent = outputFile.parentFile

                        if (parent != null && !parent.exists() && !parent.mkdirs()) {
                            updateForeground(completed.incrementAndGet())
                            return@async
                        }

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
                            outputFile.delete()
                            Log.saveError(e, "WbwAudioBulkDownload: ${pending.chapterNo}")
                        } finally {
                            WbwAudioDownloadProgressBus.clear(audioId, pending.chapterNo)
                        }

                        updateForeground(completed.incrementAndGet())
                    }
                }.awaitAll()
            }

            updateForeground(completed.get())
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private fun createEnqueueForegroundInfo(
        completedCount: Int,
        chapterTotal: Int,
    ): ForegroundInfo {
        val displayTitle = ctx.getString(R.string.wbwAudio)
        val builder = NotificationCompat.Builder(ctx, NotificationUtils.CHANNEL_ID_DOWNLOADS)
            .apply {
                setAutoCancel(false)
                setOngoing(true)
                setShowWhen(false)
                setSmallIcon(R.drawable.dr_logo)
                setSubText(ctx.getString(R.string.textDownloading))
                setContentTitle(displayTitle)
                setContentText(
                    ctx.getString(
                        R.string.recitationDownloadChaptersProgress,
                        completedCount,
                        chapterTotal,
                    ),
                )
                setCategory(NotificationCompat.CATEGORY_PROGRESS)
                val max = chapterTotal.coerceAtLeast(1)
                setProgress(max, completedCount.coerceAtMost(max), false)
            }

        builder.addAction(
            R.drawable.dr_icon_close,
            ctx.getString(R.string.strLabelCancel),
            cancelPendingIntent,
        )

        return ForegroundInfo(
            id.hashCode(),
            builder.build(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
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

        fun oneTimeRequest(
            audioId: String,
        ) = OneTimeWorkRequestBuilder<WbwAudioBulkDownloadWorker>()
            .setInputData(
                workDataOf(
                    KEY_AUDIO_ID to audioId,
                )
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .addTag(TAG)
            .addTag(bulkTag(audioId))
            .build()
    }
}
