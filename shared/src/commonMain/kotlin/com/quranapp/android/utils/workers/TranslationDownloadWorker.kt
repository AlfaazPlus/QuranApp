package com.quranapp.android.utils.workers

import com.quranapp.android.api.ApiClient
import com.quranapp.android.api.downloadTo
import com.quranapp.android.api.models.translation.TranslationBookInfoModel
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.textDownloading
import com.quranapp.android.platform.Platform
import com.quranapp.android.search.SearchIndexScheduler
import com.quranapp.android.task.ForegroundTaskInfo
import com.quranapp.android.task.TaskContext
import com.quranapp.android.task.TaskProgress
import com.quranapp.android.task.TaskResult
import com.quranapp.android.task.TaskWorker
import com.quranapp.android.utils.app.AppFiles
import com.quranapp.android.utils.app.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.getString
import kotlin.time.Duration.Companion.milliseconds

class TranslationDownloadWorker() : TaskWorker {
    override suspend fun doWork(context: TaskContext): TaskResult {
        val bookInfoJson = context.params.get<String>("bookInfo") ?: return TaskResult.Failure()
        val bookInfo = Json.decodeFromString<TranslationBookInfoModel>(bookInfoJson)

        context.setForeground(
            createForegroundInfo(bookInfo, 0)
        )

        return try {
            downloadFile(context, bookInfo)
            TaskResult.Success()
        } catch (e: Exception) {
            e.printStackTrace()
            TaskResult.Failure()
        }
    }


    private suspend fun mockDownloadFile(
        context: TaskContext,
        bookInfo: TranslationBookInfoModel
    ) {
        for (progress in 0..100 step 10) {
            if (context.isCancelled) break

            Logger.d("Mock downloading ${bookInfo.slug}: $progress%")

            context.setProgress(TaskProgress.percent(progress))
            context.setForeground(createForegroundInfo(bookInfo, progress))

            delay(1000.milliseconds)
        }
    }

    private suspend fun downloadFile(
        context: TaskContext,
        bookInfo: TranslationBookInfoModel
    ) = withContext(Dispatchers.IO) {
        val tmpFile =
            Platform.files.cacheDirectory / "${bookInfo.slug}.${System.currentTimeMillis()}.json"

        try {
            val response = ApiClient.getTranslation(bookInfo.downloadPath)

            response.downloadTo(
                tmpFile,
                onFailed = {
                    throw IllegalStateException("HTTP ${it.code}")
                },
                onProgress = { downloaded, totalBytes ->
                    if (context.isCancelled) return@downloadTo

                    val progress =
                        if (totalBytes != null && totalBytes > 0L) {
                            ((downloaded * 100) / totalBytes).toInt()
                        } else {
                            null
                        }

                    context.setProgress(
                        TaskProgress.of(
                            current = downloaded,
                            total = totalBytes ?: 0L,
                            indeterminate = progress == null,
                        )
                    )
                    context.setForeground(createForegroundInfo(bookInfo, progress))
                },
            )

            DatabaseProvider.getTranslationRepository()
                .store(bookInfo, AppFiles.readText(tmpFile))

            SearchIndexScheduler.enqueueSlug(bookInfo.slug)
        } finally {
            AppFiles.delete(tmpFile)
        }
    }

    private suspend fun createForegroundInfo(
        bookInfo: TranslationBookInfoModel,
        progress: Int?
    ): ForegroundTaskInfo {
        return ForegroundTaskInfo(
            notificationId = bookInfo.slug.hashCode(),
            title = getString(Res.string.textDownloading),
            body = bookInfo.bookName,
            indeterminate = progress == null,
            progressCurrent = progress ?: 0
        )
    }
}
