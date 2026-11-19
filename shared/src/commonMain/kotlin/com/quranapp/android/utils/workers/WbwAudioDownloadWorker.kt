package com.quranapp.android.utils.workers

import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.strTitleChapInfoChapterNo
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
import okio.Path.Companion.toPath
import org.jetbrains.compose.resources.getString

class WbwAudioDownloadWorker() : TaskWorker {

    override suspend fun doWork(context: TaskContext): TaskResult {
        val downloadUrl = context.params.get<String>(KEY_URL)
        val outputPath = context.params.get<String>(KEY_OUTPUT_PATH)
        val chapterNo = context.params.int(KEY_CHAPTER_NO, -1)
        val audioId = context.params.get<String>(KEY_AUDIO_ID)

        if (downloadUrl == null || outputPath == null || audioId == null) {
            return TaskResult.Failure()
        }

        WbwAudioRepository.ensureTimingsAvailable()

        val outputFile = outputPath.toPath()

        if (AppFiles.size(outputFile) > 0L) {
            return TaskResult.Success()
        }

        WbwAudioDownloadProgressBus.set(audioId, chapterNo, 0L, -1L)

        context.setForeground(
            createForegroundInfo(
                chapterNo = chapterNo,
                progressText = null,
                progress = null,
                indeterminateProgress = false,
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

                    context.setProgress(
                        TaskProgress(
                            current = pct,
                            total = 100,
                            indeterminate = false,
                            extras = buildTaskParams {
                                put(KEY_PROGRESS, pct)
                                put(KEY_PROGRESS_BYTES, consumed)
                                put(KEY_PROGRESS_TOTAL, total)
                            },
                        ),
                    )

                    val byteLine = when {
                        total > 0L -> "${formatFileSize(consumed)} / ${formatFileSize(total)}"
                        consumed > 0L -> formatFileSize(consumed)
                        else -> null
                    }

                    context.setForeground(
                        createForegroundInfo(
                            chapterNo = chapterNo,
                            progressText = byteLine,
                            progress = pct,
                            indeterminateProgress = false,
                        ),
                    )
                }
            }

            TaskResult.Success()
        } catch (e: Exception) {
            AppFiles.delete(outputFile)
            Logger.saveError(e, "WbwAudioSingleDownload: $chapterNo")
            TaskResult.Failure()
        } finally {
            WbwAudioDownloadProgressBus.clear(audioId, chapterNo)
        }
    }

    private suspend fun createForegroundInfo(
        chapterNo: Int,
        progressText: String?,
        progress: Int?,
        indeterminateProgress: Boolean = false,
    ): ForegroundTaskInfo {
        return ForegroundTaskInfo(
            notificationId = chapterNo,
            title = getString(Res.string.wbwAudio),
            subText = getString(Res.string.strTitleChapInfoChapterNo) + " $chapterNo",
            body = progressText,
            indeterminate = indeterminateProgress || progress == null,
            progressCurrent = progress ?: 0,
        )
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        var size = bytes.toDouble()
        var unitIdx = 0
        while (size >= 1024 && unitIdx < units.lastIndex) {
            size /= 1024
            unitIdx++
        }
        return "${((size * 10).toLong() / 10.0)} ${units[unitIdx]}"
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
        ): TaskRequest {
            val params = buildTaskParams {
                put(KEY_URL, url)
                put(KEY_OUTPUT_PATH, outputPath)
                put(KEY_CHAPTER_NO, chapterNo)
                put(KEY_AUDIO_ID, audioId)
            }

            return TaskRequest(
                workerClass = WbwAudioDownloadWorker::class,
                params = params,
                constraints = TaskConstraints.NETWORK,
                tags = setOf(TAG, chapterWorkTag(audioId, chapterNo)),
            )
        }
    }
}
