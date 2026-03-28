package com.quranapp.android.utils.workers

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.quranapp.android.R
import com.quranapp.android.activities.readerSettings.ActivitySettings
import com.quranapp.android.utils.app.NotificationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class RecitationAudioDownloadWorker(
    private val ctx: Context,
    params: WorkerParameters,
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val downloadUrl = inputData.getString(KEY_URL)
            ?: return Result.failure(workDataOf(KEY_ERROR to "Missing download URL"))
        val outputPath = inputData.getString(KEY_OUTPUT_PATH)
            ?: return Result.failure(workDataOf(KEY_ERROR to "Missing output path"))
        val title = inputData.getString(KEY_TITLE).orEmpty().ifBlank { "Recitation download" }
        val subtitle = inputData.getString(KEY_SUBTITLE)

        val outputFile = File(outputPath)
        val parent = outputFile.parentFile
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            return Result.failure(workDataOf(KEY_ERROR to "Failed to create destination directory"))
        }

        if (outputFile.exists() && outputFile.length() > 0L) {
            return Result.success()
        }

        setForeground(createForegroundInfo(title, subtitle, progress = null))

        return try {
            downloadToFile(downloadUrl, outputFile) { progress ->
                setProgress(workDataOf(KEY_PROGRESS to progress))
                setForeground(createForegroundInfo(title, subtitle, progress))
            }
            Result.success()
        } catch (e: Exception) {
            if (outputFile.exists() && outputFile.length() == 0L) {
                outputFile.delete()
            }
            Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Download failed")))
        }
    }

    private suspend fun downloadToFile(
        urlStr: String,
        targetFile: File,
        onProgress: suspend (Int) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.setRequestProperty("Connection", "close")
        conn.connectTimeout = 180000
        conn.readTimeout = 180000
        conn.allowUserInteraction = false
        conn.connect()

        if (conn.responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
            throw IllegalStateException("Audio file not found")
        }

        val totalLength = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            conn.contentLengthLong
        } else {
            conn.contentLength.toLong()
        }

        conn.inputStream.buffered(DOWNLOAD_BUFFER_SIZE).use { input ->
            FileOutputStream(targetFile).buffered(DOWNLOAD_BUFFER_SIZE).use { output ->
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

    private fun createForegroundInfo(
        title: String,
        subtitle: String?,
        progress: Int?,
    ): ForegroundInfo {
        val builder = NotificationCompat.Builder(ctx, NotificationUtils.CHANNEL_ID_DOWNLOADS).apply {
            setAutoCancel(false)
            setOngoing(true)
            setShowWhen(false)
            setSmallIcon(R.drawable.dr_logo)
            setContentTitle(title)
            if (!subtitle.isNullOrBlank()) {
                setContentText(subtitle)
            }
            setCategory(NotificationCompat.CATEGORY_PROGRESS)
            if (progress == null) {
                setProgress(0, 0, true)
            } else {
                setProgress(100, progress, false)
            }
        }

        var flag = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flag = flag or PendingIntent.FLAG_IMMUTABLE
        }

        val activityIntent = Intent(ctx, ActivitySettings::class.java).apply {
            putExtra(
                ActivitySettings.KEY_SETTINGS_DESTINATION,
                ActivitySettings.SETTINGS_MANAGE_AUDIO_RECITER
            )
        }
        val pendingIntent = PendingIntent.getActivity(
            ctx,
            id.hashCode(),
            activityIntent,
            flag
        )
        builder.setContentIntent(pendingIntent)

        val cancelIntent = WorkManager.getInstance(applicationContext)
            .createCancelPendingIntent(id)
        builder.addAction(
            R.drawable.dr_icon_close,
            ctx.getString(R.string.strLabelCancel),
            cancelIntent
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
        const val KEY_URL = "url"
        const val KEY_OUTPUT_PATH = "output_path"
        const val KEY_TITLE = "title"
        const val KEY_SUBTITLE = "subtitle"
        const val KEY_PROGRESS = "progress"
        const val KEY_ERROR = "error"

        fun uniqueWorkName(reciterId: String, chapterNo: Int): String {
            return "recitation-audio:$reciterId:$chapterNo"
        }

        fun inputData(
            url: String,
            outputPath: String,
            title: String,
            subtitle: String?,
        ): Data {
            return workDataOf(
                KEY_URL to url,
                KEY_OUTPUT_PATH to outputPath,
                KEY_TITLE to title,
                KEY_SUBTITLE to subtitle,
            )
        }
    }
}
