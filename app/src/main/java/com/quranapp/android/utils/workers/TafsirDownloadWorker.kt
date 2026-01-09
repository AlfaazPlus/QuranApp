import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.quranapp.android.R
import com.quranapp.android.activities.readerSettings.ActivitySettings
import com.quranapp.android.api.RetrofitInstance
import com.quranapp.android.api.models.tafsir.TafsirInfoModel
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.db.tafsir.QuranTafsirDBHelper
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.Logger
import com.quranapp.android.utils.app.NotificationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class TafsirDownloadWorker(
    val ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val bookInfoJson = inputData.getString("bookInfo") ?: return Result.failure()
        val tafsirInfo = Json.decodeFromString<TafsirInfoModel>(bookInfoJson)


        setForeground(createForegroundInfo(tafsirInfo, 0))

        return try {
            download(tafsirInfo)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.saveError(e, "TafsirDownloadWorker")
            Result.failure()
        }
    }


    private suspend fun mockDownload(
        bookInfo: TafsirInfoModel
    ) {
        for (progress in 0..100 step 5) {
            if (isStopped) break

            Logger.d("Mock downloading ${bookInfo.key}: $progress%")

            setProgressAsync(workDataOf("progress" to progress))
            setForeground(createForegroundInfo(bookInfo, progress))

            delay(2000)
        }
    }

    private suspend fun download(
        bookInfo: TafsirInfoModel
    ) = withContext(Dispatchers.IO) {
        // max=10 is supported by the server
        val batchSize = 10
        val totalChapters = QuranMeta.totalChapters()
        var downloadedChapters = setOf<Int>()

        QuranTafsirDBHelper(ctx).use {
            for (start in 1..totalChapters step batchSize) {
                ensureActive()
                if (isStopped) break

                val end = (start + batchSize - 1).coerceAtMost(totalChapters)

                if (downloadedChapters.containsAll((start..end).toList())) {
                    Logger.d("Tafsir ${bookInfo.key} chapters $start-$end already downloaded, skipping")
                    continue
                }

                val range = "$start-$end"
                Logger.d("Downloading tafsir ${bookInfo.key} chapters $range")

                val response = RetrofitInstance.alfaazplus.getTafsirsByChapter(bookInfo.key, range)

                it.storeTafsirs(response.tafsirs, response.version, response.timestamp1)

                // newly downloaded chapter range
                val newChapters = response.surahs.sorted()
                val newFrom = newChapters.firstOrNull() ?: -1
                val newTo = newChapters.lastOrNull() ?: -1

                if (newFrom != -1 && newTo != -1) {
                    downloadedChapters = downloadedChapters + (newFrom..newTo).toList()
                }

                val progress = ((end * 100) / totalChapters)
                setProgressAsync(workDataOf("progress" to progress))
                setForeground(createForegroundInfo(bookInfo, progress))
            }

            it.storeTafsirInfo(bookInfo.apply {
                isDownloaded = true
            })
        }
    }

    private fun createForegroundInfo(
        bookInfo: TafsirInfoModel,
        progress: Int?
    ): ForegroundInfo {
        val channelId = NotificationUtils.CHANNEL_ID_DOWNLOADS
        val builder = NotificationCompat.Builder(ctx, channelId).apply {
            setAutoCancel(false)
            setOngoing(true)
            setShowWhen(false)
            setSmallIcon(R.drawable.dr_logo)
            setContentTitle(ctx.getString(R.string.textDownloading))
            setContentText(bookInfo.author)
            setCategory(NotificationCompat.CATEGORY_PROGRESS)
            setProgress(100, progress ?: 0, progress == null)
        }

        var flag = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flag = flag or PendingIntent.FLAG_IMMUTABLE
        }

        val activityIntent = Intent(ctx, ActivitySettings::class.java).apply {
            putExtra(
                ActivitySettings.KEY_SETTINGS_DESTINATION,
                ActivitySettings.SETTINGS_TAFSIR
            )
        }
        val pendingIntent = PendingIntent.getActivity(
            ctx,
            bookInfo.key.hashCode(),
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

        val notificationId = bookInfo.key.hashCode()
        val notification = builder.build()

        return ForegroundInfo(
            notificationId,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

}
