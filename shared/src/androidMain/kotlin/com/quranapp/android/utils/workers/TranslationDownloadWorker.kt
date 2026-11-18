package com.quranapp.android.utils.workers

import android.app.PendingIntent
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.quranapp.android.api.ApiClient
import com.quranapp.android.api.downloadTo
import com.quranapp.android.api.models.translation.TranslationBookInfoModel
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.strLabelCancel
import com.quranapp.android.generated.resources.textDownloading
import com.quranapp.android.navigation.SettingRoutes
import com.quranapp.android.platform.PlatformServices
import com.quranapp.android.search.SearchIndexScheduler
import com.quranapp.android.shared.R
import com.quranapp.android.utils.app.Logger
import com.quranapp.android.utils.app.AppActions
import com.quranapp.android.utils.app.AppDestinations
import com.quranapp.android.utils.app.NotificationUtils
import com.quranapp.android.utils.app.NotificationUtils.createForegroundInfoFallback
import com.quranapp.android.utils.app.AppFiles
import com.quranapp.android.utils.miscellenous.Keys
import com.quranapp.android.utils.sharedPrefs.SPAppActions.removeFromPendingAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.getString

class TranslationDownloadWorker(
    val ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {
    override suspend fun getForegroundInfo(): ForegroundInfo {
        val bookInfoJson = inputData.getString("bookInfo")
            ?: return createForegroundInfoFallback(ctx)
        val bookInfo = Json.decodeFromString<TranslationBookInfoModel>(bookInfoJson)

        return createForegroundInfo(bookInfo, 0)
    }

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
        for (progress in 0..100 step 10) {
            if (isStopped) break

            Logger.d("Mock downloading ${bookInfo.slug}: $progress%")

            setProgressAsync(workDataOf("progress" to progress))
            setForeground(createForegroundInfo(bookInfo, progress))

            kotlinx.coroutines.delay(1000)
        }
    }

    private suspend fun downloadFile(
        bookInfo: TranslationBookInfoModel
    ) = withContext(Dispatchers.IO) {
        val tmpFile =
            PlatformServices.files.cacheDirectory / "${bookInfo.slug}.${System.currentTimeMillis()}.json"

        try {
            val response = ApiClient.getTranslation(bookInfo.downloadPath)

            response.downloadTo(
                tmpFile,
                onFailed = {
                    throw IllegalStateException("HTTP ${it.code}")
                },
                onProgress = { downloaded, totalBytes ->
                    if (isStopped) return@downloadTo

                    val progress =
                        if (totalBytes != null && totalBytes > 0L) {
                            ((downloaded * 100) / totalBytes).toInt()
                        } else {
                            null
                        }

                    setProgressAsync(workDataOf("progress" to progress))
                    setForeground(createForegroundInfo(bookInfo, progress))
                },
            )

            DatabaseProvider.getTranslationRepository()
                .store(bookInfo, AppFiles.readText(tmpFile))

            SearchIndexScheduler.enqueueSlug(ctx.applicationContext, bookInfo.slug)

            removeFromPendingAction(ctx, AppActions.APP_ACTION_TRANSL_UPDATE, bookInfo.slug)
        } finally {
            AppFiles.delete(tmpFile)
        }
    }

    private suspend fun createForegroundInfo(
        bookInfo: TranslationBookInfoModel,
        progress: Int?
    ): ForegroundInfo {
        val channelId = NotificationUtils.CHANNEL_ID_DOWNLOADS
        val builder = NotificationCompat.Builder(ctx, channelId).apply {
            setAutoCancel(false)
            setOngoing(true)
            setShowWhen(false)
            setSmallIcon(R.drawable.dr_logo)
            setContentTitle(getString(Res.string.textDownloading))
            setContentText(bookInfo.bookName)
            setCategory(NotificationCompat.CATEGORY_PROGRESS)
            setProgress(100, progress ?: 0, progress == null)
        }

        var flag = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flag = flag or PendingIntent.FLAG_IMMUTABLE
        }

        val activityIntent = AppDestinations.intent(ctx, AppDestinations.SETTINGS).apply {
            putExtra(
                Keys.NAV_DESTINATION,
                SettingRoutes.TRANSLATIONS_DOWNLOAD
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
            getString(Res.string.strLabelCancel),
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
