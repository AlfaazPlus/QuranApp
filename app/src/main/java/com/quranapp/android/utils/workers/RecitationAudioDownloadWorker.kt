package com.quranapp.android.utils.workers

import android.app.PendingIntent
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.quranapp.android.R
import com.quranapp.android.api.RetrofitInstance
import com.quranapp.android.api.models.mediaplayer.RecitationAudioKind
import com.quranapp.android.utils.app.NotificationUtils
import com.quranapp.android.utils.mediaplayer.RecitationBulkDownloadNotificationHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class RecitationAudioDownloadWorker(
    private val ctx: Context,
    params: WorkerParameters,
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val downloadUrl = inputData.getString(KEY_URL)
        val outputPath = inputData.getString(KEY_OUTPUT_PATH)
        val title = inputData.getString(KEY_TITLE).orEmpty().ifBlank { "Recitation download" }
        val subtitle = inputData.getString(KEY_SUBTITLE)
        val isBulkChild = inputData.getBoolean(KEY_BULK_CHILD, false)
        val bulkReciterId = inputData.getString(KEY_BULK_RECITER_ID)
        val bulkKindName = inputData.getString(KEY_BULK_AUDIO_KIND)
        val bulkKind = bulkKindName?.let {
            try {
                RecitationAudioKind.valueOf(it)
            } catch (_: Exception) {
                null
            }
        }

        if (downloadUrl == null || outputPath == null) {
            return Result.failure(workDataOf(KEY_ERROR to "Invalid input"))
        }

        val outputFile = File(outputPath)
        val parent = outputFile.parentFile
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            return Result.failure(workDataOf(KEY_ERROR to "Failed to create destination directory"))
        }

        if (outputFile.exists() && outputFile.length() > 0L) {
            if (isBulkChild && bulkReciterId != null && bulkKind != null) {
                RecitationBulkDownloadNotificationHelper.updateOverall(
                    applicationContext,
                    bulkReciterId,
                    bulkKind,
                    title,
                )
            }

            return Result.success()
        }

        val cancelPendingIntent =
            WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)

        // Player / standalone: show per-chapter progress in the notification.
        // Bulk queue: one indeterminate foreground; overall progress is shown by [RecitationBulkDownloadNotificationHelper].
        setForeground(
            createForegroundInfo(
                title = title,
                subtitle = subtitle,
                progress = null,
                indeterminateProgress = isBulkChild,
                cancelPendingIntent = cancelPendingIntent,
            ),
        )

        return try {
            downloadToFile(downloadUrl, outputFile) { progress ->
                setProgress(workDataOf(KEY_PROGRESS to progress))

                if (!isBulkChild) {
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
            }

            if (isBulkChild && bulkReciterId != null && bulkKind != null) {
                RecitationBulkDownloadNotificationHelper.updateOverall(
                    applicationContext,
                    bulkReciterId,
                    bulkKind,
                    title,
                )
            }
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            outputFile.delete()
            if (isBulkChild && bulkReciterId != null && bulkKind != null) {
                RecitationBulkDownloadNotificationHelper.updateOverall(
                    applicationContext,
                    bulkReciterId,
                    bulkKind,
                    title,
                )
            }
            Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Download failed")))
        }
    }

    private suspend fun downloadToFile(
        urlStr: String,
        finalFile: File,
        onProgress: suspend (Int) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val parent = finalFile.parentFile
            ?: throw IllegalStateException("Output path has no parent directory")

        val tempFile = File(parent, "${finalFile.name}.part")

        if (tempFile.exists()) {
            tempFile.delete()
        }

        try {
            val response = RetrofitInstance.any.downloadStreaming(urlStr)

            if (response.code() == 404) {
                throw IllegalStateException("Audio file not found")
            }

            if (!response.isSuccessful) {
                throw IllegalStateException("Download failed: HTTP ${response.code()}")
            }

            val body = response.body()
                ?: throw IllegalStateException("Download response body is null")

            val totalLength = body.contentLength()

            body.use { responseBody ->
                responseBody.byteStream().buffered(DOWNLOAD_BUFFER_SIZE).use { input ->
                    FileOutputStream(tempFile).buffered(DOWNLOAD_BUFFER_SIZE).use { output ->
                        val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
                        var totalConsumed = 0L
                        while (true) {
                            ensureActive()
                            val bytes = input.read(buffer)
                            if (bytes <= 0) break
                            output.write(buffer, 0, bytes)
                            totalConsumed += bytes
                            if (totalLength > 0L) {
                                onProgress((totalConsumed * 100 / totalLength).toInt())
                            }
                        }
                        output.flush()
                    }
                }
            }

            commitTempFile(tempFile, finalFile)
        } catch (e: Exception) {
            tempFile.delete()
            throw e
        }
    }

    /**
     * Replace [finalFile] with fully downloaded [tempFile].
     */
    private fun commitTempFile(tempFile: File, finalFile: File) {
        if (tempFile.renameTo(finalFile)) return

        try {
            tempFile.copyTo(finalFile, overwrite = true)
        } catch (e: Exception) {
            finalFile.delete()
            throw e
        }

        if (!tempFile.delete()) {
            finalFile.delete()
            throw IllegalStateException("Could not finalize download file")
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
        private const val DOWNLOAD_BUFFER_SIZE = 4096
        const val TAG = "recitation_audio_download"
        private const val KEY_URL = "url"
        private const val KEY_OUTPUT_PATH = "output_path"
        private const val KEY_TITLE = "title"
        private const val KEY_SUBTITLE = "subtitle"
        const val KEY_PROGRESS = "progress"
        const val KEY_ERROR = "error"
        private const val KEY_BULK_CHILD = "bulk_child"
        private const val KEY_BULK_RECITER_ID = "bulk_reciter_id"
        private const val KEY_BULK_AUDIO_KIND = "bulk_audio_kind"

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
            isBulkChild: Boolean
        ): OneTimeWorkRequest {
            val builder = androidx.work.Data.Builder()
                .putString(KEY_URL, url)
                .putString(KEY_OUTPUT_PATH, outputPath)
                .putString(KEY_TITLE, title)
                .putString(KEY_SUBTITLE, subtitle)
                .putBoolean(KEY_BULK_CHILD, isBulkChild)
                .putString(KEY_BULK_RECITER_ID, reciterId)

            if (audioKind != null) builder.putString(KEY_BULK_AUDIO_KIND, audioKind.name)

            val inputData = builder.build()

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
