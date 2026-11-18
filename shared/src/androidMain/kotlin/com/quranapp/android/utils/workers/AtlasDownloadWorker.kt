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
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.strLabelCancel
import com.quranapp.android.generated.resources.textDownloading
import com.quranapp.android.navigation.SettingRoutes
import com.quranapp.android.shared.R
import com.quranapp.android.utils.app.AppDestinations
import com.quranapp.android.utils.app.Logger
import com.quranapp.android.utils.app.NotificationUtils
import com.quranapp.android.utils.app.NotificationUtils.createForegroundInfoFallback
import com.quranapp.android.utils.app.AppFiles
import com.quranapp.android.utils.miscellenous.Keys
import com.quranapp.android.utils.reader.atlas.AtlasManager
import com.quranapp.android.utils.reader.toAtlasBundleDownloadKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import java.io.IOException

class AtlasDownloadWorker(
    private val ctx: Context,
    params: WorkerParameters,
) : CoroutineWorker(ctx, params) {
    override suspend fun getForegroundInfo(): ForegroundInfo {
        val scriptKey = inputData.getString("scriptKey") ?: return createForegroundInfoFallback(ctx)

        return createForegroundInfo(scriptKey, 0)
    }

    override suspend fun doWork(): Result {
        val scriptKey = inputData.getString("scriptKey")
        val densityLevel = inputData.getInt("densityLevel", -1)

        if (scriptKey == null || densityLevel == -1) {
            return Result.failure()
        }

        setForeground(createForegroundInfo(scriptKey, 0))

        return try {
            downloadAndStore(scriptKey, densityLevel)
            Result.success()
        } catch (e: Exception) {
            val msg = e.message ?: if (e is IOException) {
                "Atlas download failed"
            } else {
                "Atlas import failed"
            }
            Result.failure(workDataOf("error" to msg))
        }
    }

    private suspend fun downloadAndStore(
        scriptKey: String,
        densityLevel: Int,
    ) = withContext(Dispatchers.IO) {
        val tmpFile = AtlasManager.getTempDownloadFile(scriptKey)

        try {
            downloadGithubRawContentToFile(
                url = "ghraw://AlfaazPlus/QuranAppInventory/master/atlas/${scriptKey.toAtlasBundleDownloadKey()}/${densityLevel}x.zip",
                dest = tmpFile,
            ) { progress ->
                if (!isStopped) {
                    setProgressAsync(workDataOf("progress" to (progress ?: 0)))
                    setForeground(createForegroundInfo(scriptKey, progress))
                }
            }

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
    ): ForegroundInfo {
        val channelId = NotificationUtils.CHANNEL_ID_DOWNLOADS
        val builder = NotificationCompat.Builder(ctx, channelId).apply {
            setAutoCancel(false)
            setOngoing(true)
            setShowWhen(false)
            setSmallIcon(R.drawable.dr_logo)
            setContentTitle(getString(Res.string.textDownloading))
            setContentText(scriptKey)
            setCategory(NotificationCompat.CATEGORY_PROGRESS)
            setProgress(100, progress ?: 0, progress == null)
        }

        var flag = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flag = flag or PendingIntent.FLAG_IMMUTABLE
        }

        val activityIntent = AppDestinations.intent(ctx, AppDestinations.SETTINGS).apply {
            putExtra(Keys.NAV_DESTINATION, SettingRoutes.SCRIPT)
        }

        builder.setContentIntent(
            PendingIntent.getActivity(
                ctx,
                scriptKey.hashCode(),
                activityIntent,
                flag,
            ),
        )

        builder.addAction(
            R.drawable.dr_icon_close,
            getString(Res.string.strLabelCancel),
            WorkManager.getInstance(applicationContext).createCancelPendingIntent(id),
        )

        return ForegroundInfo(
            scriptKey.hashCode(),
            builder.build(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }
}
