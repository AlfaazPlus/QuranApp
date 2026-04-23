package com.quranapp.android.utils.workers

import android.app.PendingIntent
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.quranapp.android.R
import com.quranapp.android.api.models.mediaplayer.RecitationAudioKind
import com.quranapp.android.utils.app.NotificationUtils
import com.quranapp.android.utils.mediaplayer.RecitationAudioFileDownloader
import kotlinx.coroutines.CancellationException
import java.io.File

class RecitationAudioDownloadWorker(
    private val ctx: Context,
    params: WorkerParameters,
) : CoroutineWorker(ctx, params) {
    private val cancelPendingIntent =
        WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val title = inputData.getString(KEY_TITLE).orEmpty().ifBlank { "Recitation download" }
        val subtitle = inputData.getString(KEY_SUBTITLE)

        return createForegroundInfo(
            title = title,
            subtitle = subtitle,
            progress = null,
            indeterminateProgress = false,
            cancelPendingIntent = cancelPendingIntent,
        )
    }

    override suspend fun doWork(): Result {
        val downloadUrl = inputData.getString(KEY_URL)
        val outputPath = inputData.getString(KEY_OUTPUT_PATH)
        val title = inputData.getString(KEY_TITLE).orEmpty().ifBlank { "Recitation download" }
        val subtitle = inputData.getString(KEY_SUBTITLE)

        if (downloadUrl == null || outputPath == null) {
            return Result.failure(workDataOf(KEY_ERROR to "Invalid input"))
        }

        val outputFile = File(outputPath)
        val parent = outputFile.parentFile
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            return Result.failure(workDataOf(KEY_ERROR to "Failed to create destination directory"))
        }

        if (outputFile.exists() && outputFile.length() > 0L) {
            return Result.success()
        }

        setForeground(
            createForegroundInfo(
                title = title,
                subtitle = subtitle,
                progress = null,
                indeterminateProgress = false,
                cancelPendingIntent = cancelPendingIntent,
            ),
        )

        return try {
            RecitationAudioFileDownloader.downloadToFile(downloadUrl, outputFile) { progress ->
                setProgress(workDataOf(KEY_PROGRESS to progress))

                setForeground(
                    createForegroundInfo(
                        title = title,
                        subtitle = subtitle,
                        progress = progress,
                        indeterminateProgress = false,
                        cancelPendingIntent = cancelPendingIntent,
                    ),
                )
            }
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            outputFile.delete()
            Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Download failed")))
        }
    }

    private fun createForegroundInfo(
        title: String,
        subtitle: String?,
        progress: Int?,
        indeterminateProgress: Boolean = false,
        cancelPendingIntent: PendingIntent,
    ): ForegroundInfo {
        val builder = NotificationCompat.Builder(
            ctx, NotificationUtils.CHANNEL_ID_DOWNLOADS
        ).apply {
            setAutoCancel(false)
            setOngoing(true)
            setShowWhen(false)
            setSmallIcon(R.drawable.dr_logo)
            setContentTitle(title)

            if (!subtitle.isNullOrBlank()) {
                setContentText(subtitle)
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
            ctx.getString(R.string.strLabelCancel),
            cancelPendingIntent,
        )

        return ForegroundInfo(
            id.hashCode(),
            builder.build(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    companion object {
        const val TAG = "recitation_audio_download"
        private const val KEY_URL = "url"
        private const val KEY_OUTPUT_PATH = "output_path"
        private const val KEY_TITLE = "title"
        private const val KEY_SUBTITLE = "subtitle"
        const val KEY_PROGRESS = "progress"
        const val KEY_ERROR = "error"

        fun uniqueWorkName(reciterId: String, chapterNo: Int): String {
            return "recitation-audio:$reciterId:$chapterNo"
        }

        /** Tag so batch UI and player share the same WorkManager work for a reciter + kind. */
        fun reciterTag(reciterId: String, kind: RecitationAudioKind): String {
            return "recitation_reciter:${kind.name}:$reciterId"
        }

        fun oneTimeRequest(
            url: String,
            outputPath: String,
            title: String,
            subtitle: String?,
            reciterId: String,
            audioKind: RecitationAudioKind?,
        ): OneTimeWorkRequest {
            val inputData = Data.Builder()
                .putString(KEY_URL, url)
                .putString(KEY_OUTPUT_PATH, outputPath)
                .putString(KEY_TITLE, title)
                .putString(KEY_SUBTITLE, subtitle)
                .build()

            val requestBuilder = OneTimeWorkRequestBuilder<RecitationAudioDownloadWorker>()
                .setInputData(inputData)
                .addTag(TAG)

            if (audioKind != null) {
                requestBuilder.addTag(reciterTag(reciterId, audioKind))
            }

            return requestBuilder.build()
        }
    }
}
