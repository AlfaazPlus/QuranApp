package com.quranapp.android.utils.workers

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
import com.quranapp.android.utils.mediaplayer.RecitationAudioKind
import com.quranapp.android.utils.mediaplayer.RecitationDownloadProgressBus
import okio.Path.Companion.toPath

class RecitationAudioDownloadWorker() : TaskWorker {

    override suspend fun doWork(context: TaskContext): TaskResult {
        val downloadUrl = context.params.get<String>(KEY_URL)
        val outputPath = context.params.get<String>(KEY_OUTPUT_PATH)
        val title =
            context.params.get<String>(KEY_TITLE).orEmpty().ifBlank { "Recitation download" }
        val subtitle = context.params.get<String>(KEY_SUBTITLE)
        val reciterId = context.params.get<String>(KEY_RECITER_ID)
        val chapterNo = context.params.int(KEY_CHAPTER_NO, -1)
        val kind = context.params.get<String>(KEY_KIND)?.let {
            try {
                RecitationAudioKind.valueOf(it)
            } catch (_: Exception) {
                null
            }
        }

        if (downloadUrl == null || outputPath == null) {
            return TaskResult.Failure()
        }

        val outputFile = outputPath.toPath()

        if (AppFiles.size(outputFile) > 0L) {
            return TaskResult.Success()
        }

        val trackProgress = reciterId != null && chapterNo > 0 && kind != null

        if (trackProgress) {
            RecitationDownloadProgressBus.set(reciterId, chapterNo, 0L, -1L)
        }

        context.setForeground(
            createForegroundInfo(
                title = title,
                subtitle = subtitle,
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

                    context.setProgress(
                        TaskProgress.of(
                            current = consumed,
                            total = total,
                        ),
                    )

                    val byteLine = when {
                        total > 0L -> "${formatFileSize(consumed)} / ${formatFileSize(total)}"
                        consumed > 0L -> formatFileSize(consumed)
                        else -> null
                    }

                    context.setForeground(
                        createForegroundInfo(
                            title = title,
                            subtitle = subtitle,
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
            Logger.saveError(e, "RecitationSingleDownload: $chapterNo")
            TaskResult.Failure()
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
    ): ForegroundTaskInfo {
        return ForegroundTaskInfo(
            notificationId = title.hashCode(),
            title = title,
            body = progressText,
            subText = subtitle,
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
        const val TAG = "recitation_audio_download"

        private const val CHAPTER_TAG_PREFIX = "recitation_audio_chapter|"

        private const val KEY_URL = "url"
        private const val KEY_OUTPUT_PATH = "output_path"
        private const val KEY_TITLE = "title"
        private const val KEY_SUBTITLE = "subtitle"
        private const val KEY_RECITER_ID = "reciter_id"
        private const val KEY_CHAPTER_NO = "chapter_no"
        private const val KEY_KIND = "audio_kind"

        fun uniqueWorkName(reciterId: String, chapterNo: Int): String {
            return "recitation-audio:$reciterId:$chapterNo"
        }

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
        ): TaskRequest {
            val params = buildTaskParams {
                put(KEY_URL, url)
                put(KEY_OUTPUT_PATH, outputPath)
                put(KEY_TITLE, title)
                subtitle?.let { put(KEY_SUBTITLE, it) }
                put(KEY_RECITER_ID, reciterId)
                put(KEY_CHAPTER_NO, chapterNo)
                put(KEY_KIND, audioKind.name)
            }

            return TaskRequest(
                workerClass = RecitationAudioDownloadWorker::class,
                params = params,
                constraints = TaskConstraints.NETWORK,
                tags = setOf(
                    TAG,
                    reciterTag(reciterId, audioKind),
                    chapterWorkTag(reciterId, chapterNo),
                ),
            )
        }
    }
}
