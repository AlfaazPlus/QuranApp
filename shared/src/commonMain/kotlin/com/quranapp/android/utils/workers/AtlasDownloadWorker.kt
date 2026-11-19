package com.quranapp.android.utils.workers

import com.quranapp.android.api.downloadTo
import com.quranapp.android.api.fetchInventoryStreamingResponse
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.textDownloading
import com.quranapp.android.task.ForegroundTaskInfo
import com.quranapp.android.task.TaskContext
import com.quranapp.android.task.TaskProgress
import com.quranapp.android.task.TaskResult
import com.quranapp.android.task.TaskWorker
import com.quranapp.android.task.buildTaskParams
import com.quranapp.android.utils.app.AppFiles
import com.quranapp.android.utils.app.Logger
import com.quranapp.android.utils.reader.atlas.AtlasManager
import com.quranapp.android.utils.reader.toAtlasBundleDownloadKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.IOException
import org.jetbrains.compose.resources.getString

class AtlasDownloadWorker() : TaskWorker {

    override suspend fun doWork(context: TaskContext): TaskResult {
        val scriptKey = context.params.get<String>("scriptKey")
        val densityLevel = context.params.int("densityLevel", -1)

        if (scriptKey == null || densityLevel == -1) {
            return TaskResult.Failure()
        }

        context.setForeground(createForegroundInfo(scriptKey, 0))

        return try {
            downloadAndStore(context, scriptKey, densityLevel)
            TaskResult.Success()
        } catch (e: Exception) {
            val msg = e.message ?: if (e is IOException) {
                "Atlas download failed"
            } else {
                "Atlas import failed"
            }
            TaskResult.Failure(message = msg, output = buildTaskParams { put("error", msg) })
        }
    }

    private suspend fun downloadAndStore(
        context: TaskContext,
        scriptKey: String,
        densityLevel: Int,
    ) = withContext(Dispatchers.IO) {
        val tmpFile = AtlasManager.getTempDownloadFile(scriptKey)

        try {
            val response =
                fetchInventoryStreamingResponse("ghraw://AlfaazPlus/QuranAppInventory/master/atlas/${scriptKey.toAtlasBundleDownloadKey()}/${densityLevel}x.zip")

            response.downloadTo(
                tmpFile,
                onFailed = {
                    throw IllegalStateException("WBW download failed: HTTP ${it.code}")
                },
                onProgress = { downloaded, totalBytes ->
                    if (!context.isCancelled) {
                        val progress = if (totalBytes != null && totalBytes > 0L) {
                            ((downloaded * 100) / totalBytes).toInt()
                        } else {
                            null
                        }

                        context.setProgress(
                            TaskProgress(
                                progress ?: 0,
                                indeterminate = progress == null
                            )
                        )
                        context.setForeground(createForegroundInfo(scriptKey, progress))
                    }
                },
            )

            val db = DatabaseProvider.getExternalQuranDatabase()
            AtlasManager.importAtlasFromZip(tmpFile, scriptKey, db)
        } catch (e: Exception) {
            Logger.saveError(e, "AtlasDownloadWorker.downloadAndStore")
        } finally {
            AppFiles.delete(tmpFile)
        }
    }

    private suspend fun createForegroundInfo(
        scriptKey: String,
        progress: Int?,
    ): ForegroundTaskInfo {
        return ForegroundTaskInfo(
            notificationId = scriptKey.hashCode(),
            title = getString(Res.string.textDownloading),
            body = scriptKey,
            indeterminate = progress == null,
            progressCurrent = progress ?: 0,
        )
    }
}
