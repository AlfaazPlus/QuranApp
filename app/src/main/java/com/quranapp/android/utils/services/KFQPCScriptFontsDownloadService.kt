package com.quranapp.android.utils.services


import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import com.quranapp.android.R
import com.quranapp.android.activities.readerSettings.ActivitySettings
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.app.NotificationUtils
import com.quranapp.android.utils.reader.QuranScriptUtils
import com.quranapp.android.utils.reader.getQuranScriptName
import com.quranapp.android.utils.receivers.KFQPCScriptFontsDownloadReceiver
import com.quranapp.android.utils.univ.FileUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.io.Serializable
import java.net.URL

class KFQPCScriptFontsDownloadService : LifecycleService() {
    companion object {
        private const val DOWNLOAD_NOTIF_GROUP = "download_script_group"
        private const val DOWNLOAD_SCRIPT_NOTIFICATION_ID = 417

        // This is to prevent foreground and notification when binding to this service
        var STARTED_BY_USER = false
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var downloadJob: Job? = null
    private val binder = LocalBinder()
    private val notifManager by lazy { getSystemService(NOTIFICATION_SERVICE) as NotificationManager }
    var isDownloadRunning = false
    var currentScriptKey: String? = null

    private val notifFlag by lazy {
        var flag: Int = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flag = flag or PendingIntent.FLAG_IMMUTABLE
        }
        flag
    }

    private val tmpFiles = ArrayList<File>()

    private val notifActivityIntent by lazy {
        PendingIntent.getActivity(
            this,
            0,
            Intent(this, ActivitySettings::class.java).putExtra(
                ActivitySettings.KEY_SETTINGS_DESTINATION,
                ActivitySettings.SETTINGS_SCRIPT
            ),
            notifFlag
        )
    }

    override fun onCreate() {
        super.onCreate()
        if (STARTED_BY_USER) {
            startDownloadForeground()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        downloadJob?.cancel(CancellationException("Cancelled on destroy"))
        tmpFiles.forEach { it.delete() }
        tmpFiles.clear()
        isDownloadRunning = false
        STARTED_BY_USER = false
        currentScriptKey = null
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)

        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (isDownloadRunning) return START_NOT_STICKY

        val script = intent?.getStringExtra(QuranScriptUtils.KEY_SCRIPT)
        currentScriptKey = script

        if (!script.isNullOrEmpty()) {
            startDownloadForeground()
            startDownload(script)
        }

        return START_NOT_STICKY
    }

    private fun startDownload(scriptKey: String) {
        val ctx = this
        isDownloadRunning = true

        downloadJob = scope.launch {
            try {
                val urlPath = if (scriptKey == QuranScriptUtils.SCRIPT_KFQPC_V1) "qpc_v1"
                else "qpc_v2"

                val fileName =
                    if (scriptKey == QuranScriptUtils.SCRIPT_KFQPC_V1) "qpc_v1_by_page.tar.bz2"
                    else "qpc_v2_by_page.tar.bz2"

                val url =
                    "https://github.com/dabatase/qpc_fonts/releases/download/$urlPath/$fileName"

                val tempFile = File.createTempFile("tmp", fileName, filesDir)
                tmpFiles.add(tempFile)

                sendStatusBroadcast(DownloadFlow.Start)

                val conn = URL(url).openConnection()
                conn.connect()

                val totalBytes = conn.contentLength.toLong()
                if (totalBytes <= 0) {
                    throw Exception("Failed to get content length for $url")
                }

                val byteStream = conn.getInputStream()
                val outStream = tempFile.outputStream()

                readStream(
                    scriptKey,
                    totalBytes,
                    byteStream,
                    outStream,
                )

                val fileUtils = FileUtils.newInstance(ctx)
                val fontsDir = fileUtils.getKFQPCScriptFontDir(scriptKey)

                extractFonts(scriptKey, tempFile, fontsDir)

                sendStatusBroadcast(DownloadFlow.Complete)
                finish()
            } catch (e: Throwable) {
                Log.saveError(e, "KFQPCScriptFontsDownloadService")
                e.printStackTrace()
                sendStatusBroadcast(DownloadFlow.Failed)
                finish()
            }
        }
    }

    private fun extractFonts(scriptKey: String, tempFile: File, fontsDir: File) {
        sendStatusBroadcast(DownloadFlow.Extracting)
        showProgressNotification(
            scriptKey,
            100,
            true
        )

        if (!fontsDir.exists()) {
            fontsDir.mkdirs()
        }

        val inputStream = tempFile.inputStream().buffered()
        val tarIn = TarArchiveInputStream(BZip2CompressorInputStream(inputStream))

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
        tmpFiles.remove(tempFile)
    }

    private suspend fun readStream(
        scriptKey: String,
        totalBytes: Long,
        byteStream: InputStream,
        outStream: OutputStream,
    ) {
        byteStream.use { inS ->
            outStream.use { outS ->
                val buffer = ByteArray(8192)
                var progressBytes = 0L

                while (true) {
                    val bytes = inS.read(buffer)

                    if (bytes <= 0) break

                    outS.write(buffer, 0, bytes)
                    progressBytes += bytes

                    val progress =
                        if (totalBytes > 0) ((progressBytes * 100) / totalBytes).toInt() else 0

                    sendStatusBroadcast(DownloadFlow.Progress(progress))
                    showProgressNotification(scriptKey, progress, false)
                }
            }
        }
    }

    private fun startDownloadForeground() {
        val initialNotifBuilder = NotificationCompat
            .Builder(this, NotificationUtils.CHANNEL_ID_DOWNLOADS)
            .setSmallIcon(R.drawable.dr_logo)
            .setSubText(getString(R.string.textDownloading))
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setGroup(DOWNLOAD_NOTIF_GROUP)
            .setOngoing(true)
            .setContentIntent(notifActivityIntent)
            .setProgress(0, 0, true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setGroupSummary(true)

        ServiceCompat.startForeground(
            this,
            DOWNLOAD_SCRIPT_NOTIFICATION_ID,
            initialNotifBuilder.build(),
            FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    private fun showProgressNotification(scriptKey: String, progress: Int, isExtracting: Boolean) {
        val builder = NotificationCompat
            .Builder(this, NotificationUtils.CHANNEL_ID_DOWNLOADS)
            .setSmallIcon(R.drawable.dr_logo)
            .setContentTitle(
                getString(R.string.msgDownloadingFonts)
            )
            .setContentText(scriptKey.getQuranScriptName())
            .setProgress(100, progress, isExtracting)
            .setSubText(
                if (isExtracting) getString(R.string.msgExtractingFonts)
                else "${progress}%"
            )
            .setOngoing(true)
            .setShowWhen(false)
            .setContentIntent(notifActivityIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setGroup(DOWNLOAD_NOTIF_GROUP)

        notifManager.notify(DOWNLOAD_SCRIPT_NOTIFICATION_ID, builder.build())
    }

    fun sendStatusBroadcast(downloadFlow: DownloadFlow) {
        sendBroadcast(
            Intent(KFQPCScriptFontsDownloadReceiver.ACTION_DOWNLOAD_STATUS).apply {
                putExtra(KFQPCScriptFontsDownloadReceiver.KEY_DOWNLOAD_FLOW, downloadFlow)
            }
        )
    }

    fun finish() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    fun cancel() {
        downloadJob?.cancel(CancellationException("Cancelled by user"))
        finish()
    }

    inner class LocalBinder : Binder() {
        val service get() = this@KFQPCScriptFontsDownloadService
    }
}

sealed class DownloadFlow : Serializable {
    object Start : DownloadFlow()
    data class Progress(val progress: Int) : DownloadFlow()
    object Extracting : DownloadFlow()
    object Complete : DownloadFlow()
    object Failed : DownloadFlow()
}
