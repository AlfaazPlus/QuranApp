package com.quranapp.android.utils.workers

import com.quranapp.android.api.JsonHelper
import com.quranapp.android.api.downloadTo
import com.quranapp.android.api.fetchInventoryStreamingResponse
import com.quranapp.android.api.models.wbw.WbwLanguageInfo
import com.quranapp.android.api.models.wbw.WbwPayloadModel
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.db.entities.wbw.WbwWordEntity
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.textDownloading
import com.quranapp.android.preferences.ReaderPreferences
import com.quranapp.android.task.ForegroundTaskInfo
import com.quranapp.android.task.TaskContext
import com.quranapp.android.task.TaskProgress
import com.quranapp.android.task.TaskResult
import com.quranapp.android.task.TaskWorker
import com.quranapp.android.task.buildTaskParams
import com.quranapp.android.utils.app.AppFiles
import com.quranapp.android.utils.miscellenous.isGzip
import com.quranapp.android.utils.wbw.WbwManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okio.Path
import org.jetbrains.compose.resources.getString

class WbwDownloadWorker() : TaskWorker {

    override suspend fun doWork(context: TaskContext): TaskResult {
        val wbwInfoJson = context.params.get<String>("wbwInfo") ?: return TaskResult.Failure()
        val info = Json.decodeFromString<WbwLanguageInfo>(wbwInfoJson)

        context.setForeground(createForegroundInfo(info, 0))

        return try {
            downloadAndStore(context, info)
            TaskResult.Success()
        } catch (e: Exception) {
            val msg = e.message ?: "WBW download failed"
            TaskResult.Failure(message = msg, output = buildTaskParams { put("error", msg) })
        }
    }

    private suspend fun downloadAndStore(
        context: TaskContext,
        info: WbwLanguageInfo
    ) = withContext(Dispatchers.IO) {
        val tmpFile = WbwManager.tempDownloadFile(info.id)

        try {
            val response = fetchInventoryStreamingResponse(info.url)

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

                        context.setForeground(createForegroundInfo(info, progress))
                    }
                },
            )

            val payload = decodePayload(tmpFile)
            val entities = toEntities(payload, info.id)

            val db = DatabaseProvider.getExternalQuranDatabase()
            db.wbwDao().replaceByWbwId(info.id, entities)
            ReaderPreferences.bumpWbwContentEpoch()

            WbwManager.markResourceVersion(
                id = info.id,
                version = payload.version,
            )
        } finally {
            AppFiles.delete(tmpFile)
        }
    }

    private suspend fun decodePayload(source: Path): WbwPayloadModel = withContext(Dispatchers.IO) {
        if (AppFiles.size(source) <= 0L) {
            throw IllegalStateException("Source file does not exist")
        }

        val content = if (source.isGzip()) {
            AppFiles.readTextGzipped(source)
        } else {
            AppFiles.readText(source)
        }

        JsonHelper.json.decodeFromString(content)
    }

    private suspend fun createForegroundInfo(
        info: WbwLanguageInfo,
        progress: Int?
    ): ForegroundTaskInfo {
        return ForegroundTaskInfo(
            notificationId = info.id.hashCode(),
            title = getString(Res.string.textDownloading),
            body = info.langName,
            indeterminate = progress == null,
            progressCurrent = progress ?: 0
        )
    }

    private fun toEntities(payload: WbwPayloadModel, wbwId: String): List<WbwWordEntity> {
        if (payload.verses.isEmpty()) return emptyList()

        val out = ArrayList<WbwWordEntity>()
        for ((ayahId, words) in payload.verses) {
            if (ayahId <= 0) continue

            for ((wordIndex, pair) in words) {
                val translation = pair.getOrNull(0)?.takeIf { it.isNotBlank() }
                val transliteration = pair.getOrNull(1)?.takeIf { it.isNotBlank() }

                out.add(
                    WbwWordEntity(
                        ayahId = ayahId,
                        wordIndex = wordIndex,
                        wbwId = wbwId,
                        translation = translation,
                        transliteration = transliteration,
                    )
                )
            }
        }

        return out
    }
}
