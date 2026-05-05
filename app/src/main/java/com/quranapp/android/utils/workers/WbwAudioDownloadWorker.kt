package com.quranapp.android.utils.workers

import android.app.PendingIntent
import android.content.Context
import android.content.pm.ServiceInfo
import android.text.format.Formatter
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.quranapp.android.R
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.app.NotificationUtils
import com.quranapp.android.utils.mediaplayer.RecitationAudioFileDownloader
import com.quranapp.android.utils.mediaplayer.WbwAudioDownloadProgressBus
import java.io.File

class WbwAudioDownloadWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    private val cancelPendingIntent =
        WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return createForegroundInfo(
            chapterNo = inputData.getInt(KEY_CHAPTER_NO, -1),
            progressText = null,
            progress = null,
            indeterminateProgress = false,
            cancelPendingIntent = cancelPendingIntent,
        )
    }

    override suspend fun doWork(): Result {
        val downloadUrl = inputData.getString(KEY_URL)
        val outputPath = inputData.getString(KEY_OUTPUT_PATH)
        val chapterNo = inputData.getInt(KEY_CHAPTER_NO, -1)
        val audioId = inputData.getString(KEY_AUDIO_ID)

        if (downloadUrl == null || outputPath == null || audioId == null) {
            return Result.failure()
        }

        val outputFile = File(outputPath)
        val parent = outputFile.parentFile

        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            return Result.failure()
        }

        if (outputFile.exists() && outputFile.length() > 0L) {
            return Result.success()
        }


        WbwAudioDownloadProgressBus.set(audioId, chapterNo, 0L, -1L)

        setForeground(
            createForegroundInfo(
                chapterNo = chapterNo,
                progressText = null,
                progress = null,
                indeterminateProgress = false,
                cancelPendingIntent = cancelPendingIntent,
            ),
        )

        var lastWorkManagerUpdateTime = 0L

        return try {
            RecitationAudioFileDownloader.downloadToFile(
                downloadUrl,
                outputFile,
            ) { consumed, total ->
                WbwAudioDownloadProgressBus.set(audioId, chapterNo, consumed, total)

                val now = System.currentTimeMillis()
                val isFinished = total > 0L && consumed == total

                if (now - lastWorkManagerUpdateTime >= 2000L || isFinished) {
                    lastWorkManagerUpdateTime = now

                    val pct = when {
                        total > 0L -> ((consumed * 100L) / total).toInt().coerceIn(0, 100)
                        else -> 0
                    }

                    setProgress(
                        workDataOf(
                            KEY_PROGRESS to pct,
                            KEY_PROGRESS_BYTES to consumed,
                            KEY_PROGRESS_TOTAL to total,
                        ),
                    )

                    val byteLine = when {
                        total > 0L ->
                            "${Formatter.formatFileSize(context, consumed)} / ${
                                Formatter.formatFileSize(context, total)
                            }"

                        consumed > 0L ->
                            Formatter.formatFileSize(context, consumed)

                        else -> null
                    }

                    setForeground(
                        createForegroundInfo(
                            chapterNo = chapterNo,
                            progressText = byteLine,
                            progress = pct,
                            indeterminateProgress = false,
                            cancelPendingIntent = cancelPendingIntent,
                        ),
                    )
                }
            }

            Result.success()
        } catch (e: Exception) {
            outputFile.delete()
            Log.saveError(e, "WbwAudioSingleDownload: $chapterNo")
            Result.failure()
        } finally {
            WbwAudioDownloadProgressBus.clear(audioId, chapterNo)
        }
    }

    private fun createForegroundInfo(
        chapterNo: Int,
        progressText: String?,
        progress: Int?,
        indeterminateProgress: Boolean = false,
        cancelPendingIntent: PendingIntent,
    ): ForegroundInfo {
        val builder = NotificationCompat.Builder(
            context,
            NotificationUtils.CHANNEL_ID_DOWNLOADS,
        ).apply {
            setAutoCancel(false)
            setOngoing(true)
            setShowWhen(false)
            setSmallIcon(R.drawable.dr_logo)
            setContentTitle(context.getString(R.string.wbwAudio))
            setSubText(context.getString(R.string.strTitleChapInfoChapterNo) + " $chapterNo")

            if (!progressText.isNullOrBlank()) {
                setContentText(progressText)
            }

            setCategory(NotificationCompat.CATEGORY_PROGRESS)

            when {
                indeterminateProgress -> setProgress(0, 0, true)
                progress == null -> setProgress(0, 0, true)
                else -> setProgress(100, progress, false)
            }
        }

        builder.addAction(
            R.drawable.dr_icon_close,
            context.getString(R.string.strLabelCancel),
            cancelPendingIntent,
        )

        return ForegroundInfo(
            id.hashCode(),
            builder.build(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    companion object {
        const val TAG = "wbw_audio_download"

        private const val CHAPTER_TAG_PREFIX = "wbw_audio_chapter|"

        private const val KEY_URL = "url"
        private const val KEY_OUTPUT_PATH = "output_path"
        private const val KEY_CHAPTER_NO = "chapter_no"
        private const val KEY_AUDIO_ID = "audio_id"

        const val KEY_PROGRESS = "progress"
        const val KEY_PROGRESS_BYTES = "progress_bytes"
        const val KEY_PROGRESS_TOTAL = "progress_total"

        fun uniqueWorkName(chapterNo: Int): String = "wbw-audio-chapter:$chapterNo"

        fun chapterWorkTag(audioId: String, chapterNo: Int): String =
            "$CHAPTER_TAG_PREFIX$audioId|$chapterNo"

        fun parseChapterWorkTag(tag: String): Pair<String, Int>? {
            if (!tag.startsWith(CHAPTER_TAG_PREFIX)) return null

            val rest = tag.removePrefix(CHAPTER_TAG_PREFIX)
            val idx = rest.lastIndexOf('|')

            if (idx <= 0 || idx >= rest.length - 1) return null

            val audioId = rest.substring(0, idx)
            val chapterNo = rest.substring(idx + 1).toIntOrNull() ?: return null

            return audioId to chapterNo
        }

        fun oneTimeRequest(
            url: String,
            outputPath: String,
            chapterNo: Int,
            audioId: String,
        ): OneTimeWorkRequest {
            val inputData = Data.Builder()
                .putString(KEY_URL, url)
                .putString(KEY_OUTPUT_PATH, outputPath)
                .putInt(KEY_CHAPTER_NO, chapterNo)
                .putString(KEY_AUDIO_ID, audioId)
                .build()

            return OneTimeWorkRequestBuilder<WbwAudioDownloadWorker>()
                .setInputData(inputData)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .addTag(TAG)
                .addTag(chapterWorkTag(audioId, chapterNo))
                .build()
        }
    }
}
