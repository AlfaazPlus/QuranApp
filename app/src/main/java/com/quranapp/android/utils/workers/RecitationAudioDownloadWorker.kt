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
import com.quranapp.android.api.models.mediaplayer.RecitationAudioKind
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.app.NotificationUtils
import com.quranapp.android.utils.mediaplayer.RecitationAudioFileDownloader
import com.quranapp.android.utils.mediaplayer.RecitationDownloadProgressBus
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
            progressText = null,
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
        val reciterId = inputData.getString(KEY_RECITER_ID)
        val chapterNo = inputData.getInt(KEY_CHAPTER_NO, -1)
        val kind = inputData.getString(KEY_KIND)?.let {
            try {
                RecitationAudioKind.valueOf(it)
            } catch (_: Exception) {
                null
            }
        }

        if (downloadUrl == null || outputPath == null) {
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

        val trackProgress =
            reciterId != null && chapterNo > 0 && kind != null

        if (trackProgress) {
            RecitationDownloadProgressBus.set(reciterId, chapterNo, 0L, -1L)
        }

        setForeground(
            createForegroundInfo(
                title = title,
                subtitle = subtitle,
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
                outputFile
            ) { consumed, total ->
                if (trackProgress) {
                    RecitationDownloadProgressBus.set(reciterId, chapterNo, consumed, total)
                }

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
                            "${
                                Formatter.formatFileSize(
                                    ctx,
                                    consumed
                                )
                            } / ${Formatter.formatFileSize(ctx, total)}"

                        consumed > 0L ->
                            Formatter.formatFileSize(ctx, consumed)

                        else -> null
                    }

                    setForeground(
                        createForegroundInfo(
                            title = title,
                            subtitle = subtitle,
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
            Log.saveError(e, "RecitationSingleDownload: ${chapterNo}")
            Result.failure()
        } finally {
            if (trackProgress) {
                RecitationDownloadProgressBus.clear(reciterId, chapterNo)
            }
        }
    }

    private fun createForegroundInfo(
        title: String,
        subtitle: String?,
        progressText: String?,
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
                setSubText(subtitle)
            }

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

        private const val CHAPTER_TAG_PREFIX = "recitation_audio_chapter|"

        private const val KEY_URL = "url"
        private const val KEY_OUTPUT_PATH = "output_path"
        private const val KEY_TITLE = "title"
        private const val KEY_SUBTITLE = "subtitle"
        private const val KEY_RECITER_ID = "reciter_id"
        private const val KEY_CHAPTER_NO = "chapter_no"
        private const val KEY_KIND = "audio_kind"
        const val KEY_PROGRESS = "progress"
        const val KEY_PROGRESS_BYTES = "progress_bytes"
        const val KEY_PROGRESS_TOTAL = "progress_total"

        fun uniqueWorkName(reciterId: String, chapterNo: Int): String {
            return "recitation-audio:$reciterId:$chapterNo"
        }

        /** Tag so batch UI and player share the same WorkManager work for a reciter + kind. */
        fun reciterTag(reciterId: String, kind: RecitationAudioKind): String {
            return "recitation_reciter:${kind.name}:$reciterId"
        }

        fun chapterWorkTag(reciterId: String, chapterNo: Int): String {
            return "$CHAPTER_TAG_PREFIX$reciterId|$chapterNo"
        }

        fun parseChapterWorkTag(tag: String): Pair<String, Int>? {
            if (!tag.startsWith(CHAPTER_TAG_PREFIX)) return null

            val rest = tag.removePrefix(CHAPTER_TAG_PREFIX)
            val idx = rest.lastIndexOf('|')

            if (idx <= 0 || idx >= rest.length - 1) return null

            val reciterId = rest.substring(0, idx)
            val chapterNo = rest.substring(idx + 1).toIntOrNull() ?: return null

            return reciterId to chapterNo
        }

        fun oneTimeRequest(
            url: String,
            outputPath: String,
            title: String,
            subtitle: String?,
            reciterId: String,
            audioKind: RecitationAudioKind,
            chapterNo: Int,
        ): OneTimeWorkRequest {
            val inputData = Data.Builder()
                .putString(KEY_URL, url)
                .putString(KEY_OUTPUT_PATH, outputPath)
                .putString(KEY_TITLE, title)
                .putString(KEY_SUBTITLE, subtitle)
                .putString(KEY_RECITER_ID, reciterId)
                .putInt(KEY_CHAPTER_NO, chapterNo)
                .putString(KEY_KIND, audioKind.name)
                .build()

            return OneTimeWorkRequestBuilder<RecitationAudioDownloadWorker>()
                .setInputData(inputData)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .addTag(TAG)
                .addTag(reciterTag(reciterId, audioKind))
                .addTag(chapterWorkTag(reciterId, chapterNo))
                .build()
        }
    }
}
