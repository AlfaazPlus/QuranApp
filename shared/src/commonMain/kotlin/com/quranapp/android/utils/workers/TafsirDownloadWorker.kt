package com.quranapp.android.utils.workers

import com.quranapp.android.api.ApiClient
import com.quranapp.android.api.models.tafsir.TafsirInfoModel
import com.quranapp.android.db.tafsir.QuranTafsirDBHelper
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.textDownloading
import com.quranapp.android.task.ForegroundTaskInfo
import com.quranapp.android.task.TaskContext
import com.quranapp.android.task.TaskProgress
import com.quranapp.android.task.TaskResult
import com.quranapp.android.task.TaskWorker
import com.quranapp.android.utils.app.Logger
import com.quranapp.android.utils.quran.QuranMeta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.getString
import kotlin.time.Duration.Companion.milliseconds

class TafsirDownloadWorker() : TaskWorker {

    override suspend fun doWork(context: TaskContext): TaskResult {
        val bookInfoJson = context.params.get<String>("bookInfo") ?: return TaskResult.Failure()
        val tafsirInfo = Json.decodeFromString<TafsirInfoModel>(bookInfoJson)

        context.setForeground(createForegroundInfo(tafsirInfo, 0))

        return try {
            download(context, tafsirInfo)
            TaskResult.Success()
        } catch (e: Exception) {
            e.printStackTrace()
            Logger.saveError(e, "TafsirDownloadWorker")
            TaskResult.Failure()
        }
    }

    private suspend fun mockDownload(
        context: TaskContext,
        bookInfo: TafsirInfoModel
    ) {
        for (progress in 0..100 step 5) {
            if (context.isCancelled) break

            Logger.d("Mock downloading ${bookInfo.key}: $progress%")

            context.setProgress(TaskProgress.percent(progress))
            context.setForeground(createForegroundInfo(bookInfo, progress))

            delay(2000.milliseconds)
        }
    }

    private suspend fun download(
        context: TaskContext,
        bookInfo: TafsirInfoModel
    ) = withContext(Dispatchers.IO) {
        // max=10 is supported by the server
        val batchSize = 10
        val chapterRange = QuranMeta.chapterRange
        var downloadedChapters = setOf<Int>()

        val dbHelper = QuranTafsirDBHelper()

        try {
            for (start in chapterRange step batchSize) {
                ensureActive()

                if (context.isCancelled) break

                val end = (start + batchSize - 1).coerceAtMost(chapterRange.last)

                if (downloadedChapters.containsAll((start..end).toList())) {
                    Logger.d("Tafsir ${bookInfo.key} chapters $start-$end already downloaded, skipping")
                    continue
                }

                val range = "$start-$end"
                Logger.d("Downloading tafsir ${bookInfo.key} chapters $range")

                val response = ApiClient.getTafsirsByChapter(bookInfo.key, range)

                dbHelper.storeTafsirs(response.tafsirs, response.version, response.timestamp1)

                // newly downloaded chapter range
                val newChapters = response.surahs.sorted()
                val newFrom = newChapters.firstOrNull() ?: -1
                val newTo = newChapters.lastOrNull() ?: -1

                if (newFrom != -1 && newTo != -1) {
                    downloadedChapters = downloadedChapters + (newFrom..newTo).toList()
                }

                val progress = ((end * 100) / chapterRange.last)
                context.setProgress(TaskProgress.percent(progress))
                context.setForeground(createForegroundInfo(bookInfo, progress))
            }

            dbHelper.storeTafsirInfo(bookInfo.apply {
                isDownloaded = true
            })
        } finally {
            dbHelper.close()
        }
    }

    private suspend fun createForegroundInfo(
        bookInfo: TafsirInfoModel,
        progress: Int?
    ): ForegroundTaskInfo {
        return ForegroundTaskInfo(
            notificationId = bookInfo.key.hashCode(),
            title = getString(Res.string.textDownloading),
            body = bookInfo.author,
            indeterminate = progress == null,
            progressCurrent = progress ?: 0
        )
    }
}
