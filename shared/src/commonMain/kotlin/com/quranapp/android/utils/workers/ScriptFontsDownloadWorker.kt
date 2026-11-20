package com.quranapp.android.utils.workers

import com.quranapp.android.api.ApiClient
import com.quranapp.android.api.downloadTo
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.msgDownloadingFonts
import com.quranapp.android.generated.resources.msgExtractingFonts
import com.quranapp.android.generated.resources.textDownloading
import com.quranapp.android.platform.Platform
import com.quranapp.android.task.ForegroundTaskInfo
import com.quranapp.android.task.TaskContext
import com.quranapp.android.task.TaskProgress
import com.quranapp.android.task.TaskResult
import com.quranapp.android.task.TaskWorker
import com.quranapp.android.utils.app.AppFiles
import com.quranapp.android.utils.app.Logger
import com.quranapp.android.utils.reader.QuranScriptUtils
import com.quranapp.android.utils.reader.getQuranScriptName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.jetbrains.compose.resources.getString
import java.io.File
import java.util.zip.GZIPInputStream
import kotlin.time.Duration.Companion.milliseconds

class ScriptFontsDownloadWorker() : TaskWorker {
    companion object {
        const val KEY_SCRIPT = "script_key"
    }

    override suspend fun doWork(context: TaskContext): TaskResult {
        val scriptKey = context.params.get<String>(KEY_SCRIPT) ?: return TaskResult.Failure()

        updateProgress(context, scriptKey, 0)

        return try {
            downloadAndExtract(context, scriptKey)
            TaskResult.Success()
        } catch (e: Exception) {
            Logger.saveError(e, "KFQPCScriptFontsDownloadWorker")
            TaskResult.Failure()
        }
    }

    private suspend fun mockDownload(
        context: TaskContext,
        scriptKey: String
    ) {
        for (progress in 0..160 step 10) {
            if (context.isCancelled) break

            Logger.d("Mock downloading ${scriptKey}: $progress%")

            updateProgress(context, scriptKey, progress)

            kotlinx.coroutines.delay(1000.milliseconds)
        }
    }

    private suspend fun downloadAndExtract(
        context: TaskContext,
        scriptKey: String
    ) = withContext(Dispatchers.IO) {
        val fileName = when (scriptKey) {
            QuranScriptUtils.SCRIPT_KFQPC_V1 -> "qpc_v1_by_page.tar.gz"
            QuranScriptUtils.SCRIPT_KFQPC_V2 -> "qpc_v2_by_page.tar.gz"
            QuranScriptUtils.SCRIPT_KFQPC_V4 -> "qpc_v4_tajweed_by_page.tar.gz"
            else -> throw IllegalArgumentException("Unknown script key: $scriptKey")
        }

        val url = "https://github.com/AlfaazPlus/QuranAppInventory/releases/download/qpc/$fileName"
        val tempFile =
            Platform.files.filesDirectory / "tmp_${System.currentTimeMillis()}_$fileName"

        try {
            val response = ApiClient.downloadStreaming(url)

            response.downloadTo(
                tempFile,
                onFailed = {
                    throw IllegalStateException("Font download failed: HTTP ${it.code}")
                },
                onProgress = { bytesCopied, totalBytes ->
                    if (context.isCancelled) return@downloadTo

                    val progress = if (totalBytes != null && totalBytes > 0L) {
                        ((bytesCopied * 100) / totalBytes).toInt()
                    } else {
                        -1
                    }

                    updateProgress(context, scriptKey, progress)
                },
            )

            val fontsDir = QuranScriptUtils.kfqpcFontDir(scriptKey).toFile()

            extractFonts(context, scriptKey, tempFile.toFile(), fontsDir)
        } finally {
            AppFiles.delete(tempFile)
        }
    }

    private suspend fun extractFonts(
        context: TaskContext,
        scriptKey: String,
        tempFile: File,
        fontsDir: File
    ) {
        updateProgress(context, scriptKey, 101)

        if (!fontsDir.exists()) {
            fontsDir.mkdirs()
        }

        val inputStream = tempFile.inputStream().buffered()
        val tarIn = TarArchiveInputStream(GZIPInputStream(inputStream))

        var entry = tarIn.nextEntry
        while (entry != null) {
            if (!tarIn.canReadEntryData(entry)) {
                entry = tarIn.nextEntry
                continue
            }

            val outFile = File(fontsDir, entry.name).apply {
                parentFile?.mkdirs()
            }

            outFile.parentFile?.mkdirs()
            outFile.outputStream().buffered().use { out ->
                tarIn.copyTo(out)
            }

            entry = tarIn.nextEntry
        }

        tarIn.close()

        tempFile.delete()
    }

    suspend fun updateProgress(
        context: TaskContext,
        script: String,
        progress: Int,
    ) {
        context.setForeground(
            createForegroundInfo(
                script,
                if (progress <= 100) null else progress,
                progress > 100
            )
        )

        val extracting = progress > 100 || progress < 0
        context.setProgress(
            if (extracting) TaskProgress.INDETERMINATE else TaskProgress.percent(progress)
        )
    }

    private suspend fun createForegroundInfo(
        scriptKey: String,
        progress: Int?,
        isExtracting: Boolean
    ): ForegroundTaskInfo {
        val subText = if (isExtracting) getString(Res.string.msgExtractingFonts)
        else if (progress != null) "$progress%" else getString(Res.string.textDownloading)

        return ForegroundTaskInfo(
            notificationId = scriptKey.hashCode(),
            title = getString(Res.string.msgDownloadingFonts),
            body = scriptKey.getQuranScriptName(),
            subText = subText,
            indeterminate = progress == null || isExtracting,
            progressCurrent = progress ?: 0,
        )
    }
}
