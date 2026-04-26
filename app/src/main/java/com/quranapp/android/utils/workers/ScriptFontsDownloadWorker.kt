package com.quranapp.android.utils.workers

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
import com.quranapp.android.activities.ActivitySettings
import com.quranapp.android.api.RetrofitInstance
import com.quranapp.android.compose.navigation.SettingRoutes
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.Logger
import com.quranapp.android.utils.app.NotificationUtils
import com.quranapp.android.utils.app.NotificationUtils.createForegroundInfoFallback
import com.quranapp.android.utils.reader.QuranScriptUtils
import com.quranapp.android.utils.reader.getQuranScriptName
import com.quranapp.android.utils.univ.FileUtils
import com.quranapp.android.utils.univ.Keys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.File
import java.util.zip.GZIPInputStream

class ScriptFontsDownloadWorker(
    private val ctx: Context, params: WorkerParameters
) : CoroutineWorker(ctx, params) {
    companion object {
        const val KEY_SCRIPT = "script_key"
        const val KEY_PROGRESS = "progress"
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val scriptKey = inputData.getString(KEY_SCRIPT)
            ?: return createForegroundInfoFallback(ctx)

        return createForegroundInfo(scriptKey, 0, false)
    }

    override suspend fun doWork(): Result {
        val scriptKey = inputData.getString(KEY_SCRIPT) ?: return Result.failure()

        updateProgres(scriptKey, 0)

        return try {
            downloadAndExtract(scriptKey)
            // mockDownload(scriptKey)
            Result.success()
        } catch (e: Exception) {
            Log.saveError(e, "KFQPCScriptFontsDownloadWorker")
            Result.failure()
        }
    }

    private suspend fun mockDownload(
        scriptKey: String
    ) {
        for (progress in 0..160 step 10) {
            if (isStopped) break

            Logger.d("Mock downloading ${scriptKey}: $progress%")

            updateProgres(scriptKey, progress)

            kotlinx.coroutines.delay(1000)
        }
    }

    private suspend fun downloadAndExtract(scriptKey: String) = withContext(Dispatchers.IO) {
        val fileName = when (scriptKey) {
            QuranScriptUtils.SCRIPT_KFQPC_V1 -> "qpc_v1_by_page.tar.gz"
            QuranScriptUtils.SCRIPT_KFQPC_V2 -> "qpc_v2_by_page.tar.gz"
            QuranScriptUtils.SCRIPT_KFQPC_V4 -> "qpc_v4_tajweed_by_page.tar.gz"
            else -> throw IllegalArgumentException("Unknown script key: $scriptKey")
        }

        val url = "https://github.com/AlfaazPlus/QuranAppInventory/releases/download/qpc/$fileName"
        val tempFile = File.createTempFile("tmp", fileName, ctx.filesDir)


        try {
            val response = RetrofitInstance.any.downloadStreaming(url)

            if (!response.isSuccessful || response.body() == null) {
                throw Error("Failed")
            }

            val body = response.body()!!
            val totalBytes = body.contentLength()

            val input = body.byteStream()
            val output = tempFile.outputStream()

            val buffer = ByteArray(8 * 1024)
            var bytesCopied = 0L
            var bytes = input.read(buffer)

            while (bytes >= 0) {
                output.write(buffer, 0, bytes)
                bytesCopied += bytes

                val progress = if (totalBytes > 0) {
                    ((bytesCopied * 100) / totalBytes).toInt()
                } else -1

                updateProgres(scriptKey, progress)

                bytes = input.read(buffer)
            }

            output.flush()
            output.close()
            input.close()


            val fileUtils = FileUtils.newInstance(ctx)
            val fontsDir = fileUtils.getKFQPCScriptFontDir(scriptKey)

            extractFonts(scriptKey, tempFile, fontsDir)
        } finally {
            tempFile.delete()
        }
    }

    private suspend fun extractFonts(scriptKey: String, tempFile: File, fontsDir: File) {
        updateProgres(scriptKey, 101)

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

    suspend fun updateProgres(
        script: String,
        progress: Int,
    ) {
        setForeground(
            createForegroundInfo(
                script,
                if (progress <= 100) null else progress,
                progress > 100
            )
        )

        setProgress(workDataOf(KEY_PROGRESS to progress))
    }

    private fun createForegroundInfo(
        scriptKey: String, progress: Int?, isExtracting: Boolean
    ): ForegroundInfo {
        var flag = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flag = flag or PendingIntent.FLAG_IMMUTABLE
        }

        val pendingIntent = PendingIntent.getActivity(
            ctx, scriptKey.hashCode(), Intent(ctx, ActivitySettings::class.java).putExtra(
                Keys.NAV_DESTINATION, SettingRoutes.SCRIPT
            ), flag
        )

        val cancelIntent = WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)
        val builder = NotificationCompat.Builder(ctx, NotificationUtils.CHANNEL_ID_DOWNLOADS)
            .setSmallIcon(R.drawable.dr_logo)
            .setContentTitle(ctx.getString(R.string.msgDownloadingFonts))
            .setContentText(scriptKey.getQuranScriptName())
            .setProgress(100, progress ?: 0, progress == null || isExtracting).setSubText(
                if (isExtracting) ctx.getString(R.string.msgExtractingFonts)
                else if (progress != null) "$progress%" else ctx.getString(R.string.textDownloading)
            ).setOngoing(true).setShowWhen(false).setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS).addAction(
                R.drawable.dr_icon_close, ctx.getString(R.string.strLabelCancel), cancelIntent
            )

        return ForegroundInfo(
            scriptKey.hashCode(), builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }
}
