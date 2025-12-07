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
import com.quranapp.android.api.models.translation.TranslationBookInfoModel
import com.quranapp.android.utils.Logger
import com.quranapp.android.utils.app.AppActions
import com.quranapp.android.utils.app.NotificationUtils
import com.quranapp.android.utils.reader.factory.QuranTranslationFactory
import com.quranapp.android.utils.sharedPrefs.SPAppActions.removeFromPendingAction
import com.quranapp.android.utils.sharedPrefs.SPReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

class TranslationDownloadWorker(
    val ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val bookInfoJson = inputData.getString("bookInfo") ?: return Result.failure()
        val bookInfo = Json.decodeFromString<TranslationBookInfoModel>(bookInfoJson)


        setForeground(createForegroundInfo(bookInfo, 0))

        return try {
            downloadFile(bookInfo)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private suspend fun mockDownloadFile(
        bookInfo: TranslationBookInfoModel
    ) {
        for (progress in 0..100 step 5) {
            if (isStopped) break

            Logger.d("Mock downloading ${bookInfo.slug}: $progress%")

            setProgressAsync(workDataOf("progress" to progress))
            setForeground(createForegroundInfo(bookInfo, progress))

            kotlinx.coroutines.delay(2000)
        }
    }

    private suspend fun downloadFile(
        bookInfo: TranslationBookInfoModel
    ) = withContext(Dispatchers.IO) {
        val tmpFile = File.createTempFile(
            bookInfo.slug,
            ".json",
            ctx.cacheDir
        )

        val response = RetrofitInstance.github.getTranslation(bookInfo.downloadPath)

        if (!response.isSuccessful) throw Exception("HTTP ${response.code()}")
        val body = response.body() ?: throw Exception("Empty body")
        val totalBytes = body.contentLength()
        val byteStream = body.byteStream()

        byteStream.use { inS ->
            tmpFile.outputStream().buffered().use { outS ->


                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var downloaded = 0L

                while (true) {
                    ensureActive()

                    if (isStopped) break

                    val bytes = inS.read(buffer)

                    if (bytes <= 0) break

                    outS.write(buffer, 0, bytes)
                    downloaded += bytes

                    val progress =
                        if (totalBytes > 0) ((downloaded * 100) / totalBytes).toInt() else null
                    setProgressAsync(workDataOf("progress" to progress))
                    setForeground(createForegroundInfo(bookInfo, progress))
                }

                outS.flush()
            }
        }

        QuranTranslationFactory(ctx).use {
            it.dbHelper.storeTranslation(bookInfo, tmpFile.readText())
        }

        removeFromPendingAction(ctx, AppActions.APP_ACTION_TRANSL_UPDATE, bookInfo.slug)
        val savedTranslations = SPReader.getSavedTranslations(ctx)
        if (savedTranslations.remove(bookInfo.slug)) {
            SPReader.setSavedTranslations(ctx, savedTranslations)
        }
    }

    private fun createForegroundInfo(
        bookInfo: TranslationBookInfoModel,
        progress: Int?
    ): ForegroundInfo {
        val channelId = NotificationUtils.CHANNEL_ID_DOWNLOADS
        val builder = NotificationCompat.Builder(ctx, channelId).apply {
            setAutoCancel(false)
            setOngoing(true)
            setShowWhen(false)
            setSmallIcon(R.drawable.dr_logo)
            setContentTitle(ctx.getString(R.string.textDownloading))
            setContentText(bookInfo.bookName)
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
                ActivitySettings.SETTINGS_TRANSLATION_DOWNLOAD
            )
        }
        val pendingIntent = PendingIntent.getActivity(
            ctx,
            bookInfo.slug.hashCode(),
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

        val notificationId = bookInfo.slug.hashCode()
        val notification = builder.build()

        return ForegroundInfo(
            notificationId,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

}
