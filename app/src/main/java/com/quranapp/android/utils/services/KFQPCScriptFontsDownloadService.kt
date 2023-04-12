package com.quranapp.android.utils.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.quranapp.android.R
import com.quranapp.android.activities.readerSettings.ActivitySettings
import com.quranapp.android.api.RetrofitInstance
import com.quranapp.android.utils.reader.QuranScriptUtils
import com.quranapp.android.utils.reader.getQuranScriptName
import com.quranapp.android.utils.receivers.KFQPCScriptFontsDownloadReceiver
import com.quranapp.android.utils.univ.FileUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipFile

class KFQPCScriptFontsDownloadService : Service() {
    companion object {
        private const val DOWNLOAD_NOTIF_GROUP = "download_script_group"
        private const val DOWNLOAD_SCRIPT_NOTIFICATION_ID = 417
        const val ALL_PART_DOWNLOADS_FINISHED = -2

        // This is to prevent foreground and notification when binding to this service
        var STARTED_BY_USER = false
    }

    private val job = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + job)
    private val binder = LocalBinder()
    private val notifManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }
    var isDownloadRunning = false
    var currentScriptKey: String? = null

    private val notifFlag by lazy {
        var flag: Int = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flag = flag or PendingIntent.FLAG_IMMUTABLE
        }
        flag
    }

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
        job.cancel(CancellationException("Cancelled on destroy"))
        isDownloadRunning = false
        STARTED_BY_USER = false
        currentScriptKey = null
    }

    override fun onBind(intent: Intent): IBinder {
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

        coroutineScope.launch {
            flow {
                val fileUtils = FileUtils.newInstance(ctx)

                try {
                    val scriptFile = fileUtils.getScriptFile(scriptKey)

                    if (scriptFile.length() == 0L) {
                        if (!fileUtils.createFile(scriptFile)) {
                            emit(DownloadFlow.Failed(null))
                            return@flow
                        }

                        emit(DownloadFlow.Start(null))
                        emit(DownloadFlow.Progress(null, 0))

                        val scriptResBody = RetrofitInstance.github.getQuranScript(
                            "script_$scriptKey.json"
                        )
                        val byteStream = scriptResBody.byteStream()

                        val totalBytes = byteStream.available()
                        readStreams(
                            this@flow,
                            null,
                            byteStream,
                            scriptFile.outputStream(),
                            totalBytes
                        )

                        emit(DownloadFlow.Complete(null))
                    }
                } catch (e: Exception) {
                    emit(DownloadFlow.Failed(null))
                    e.printStackTrace()
                    return@flow
                }

                val fontsDir = fileUtils.getKFQPCScriptFontDir(scriptKey)

                for (partNo in  arrayOf(1, 2, 3)) {
                    try {
                        val partFilename = "$scriptKey-$partNo.zip"
                        val partFile = File.createTempFile("tmp", partFilename, filesDir)

                        emit(DownloadFlow.Start(partNo))
                        emit(DownloadFlow.Progress(partNo, 0))

                        val fontResBody = RetrofitInstance.github.getKFQPCFont(scriptKey, partFilename)
                        val byteStream = fontResBody.byteStream()

                        val totalBytes = byteStream.available()

                        readStreams(
                            this,
                            partNo,
                            byteStream,
                            partFile.outputStream(),
                            totalBytes
                        )

                        extractFonts(partFile, fontsDir)

                        emit(DownloadFlow.Complete(partNo))
                    } catch (e: Exception) {
                        e.printStackTrace()
                        emit(DownloadFlow.Failed(partNo))
                    }
                }

                emit(DownloadFlow.Complete(ALL_PART_DOWNLOADS_FINISHED))
            }.flowOn(Dispatchers.IO).catch {
                it.printStackTrace()
                sendBroadcast(
                    Intent(KFQPCScriptFontsDownloadReceiver.ACTION_DOWNLOAD_STATUS).apply {
                        putExtra(
                            KFQPCScriptFontsDownloadReceiver.KEY_DOWNLOAD_FLOW,
                            DownloadFlow.Failed(null)
                        )
                    }
                )
                finish()
            }.collect {
                sendBroadcast(
                    Intent(KFQPCScriptFontsDownloadReceiver.ACTION_DOWNLOAD_STATUS).apply {
                        putExtra(KFQPCScriptFontsDownloadReceiver.KEY_DOWNLOAD_FLOW, it)
                    }
                )

                if (it is DownloadFlow.Complete && it.partNo == ALL_PART_DOWNLOADS_FINISHED || it is DownloadFlow.Failed && it.partNo == null) {
                    finish()
                } else if (it is DownloadFlow.Progress) {
                    showProgressNotification(it.partNo, it.progress, scriptKey)
                }
            }
        }
    }

    private fun extractFonts(partFile: File, fontsDir: File) {
        if (!fontsDir.exists()) {
            fontsDir.mkdirs()
        }

        ZipFile(partFile).use {
            it.entries().asSequence().forEach { entry ->
                val entryFile = File(fontsDir, entry.name).apply {
                    parentFile?.mkdirs()
                }

                it.getInputStream(entry).use { input ->
                    entryFile.outputStream().use { output ->
                        input.copyTo(output)

                        output.flush()
                    }
                }
            }
        }
    }

    private suspend fun readStreams(
        flowCollector: FlowCollector<DownloadFlow>,
        partNo: Int?,
        byteStream: InputStream,
        outStream: OutputStream,
        totalBytes: Int
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

                    flowCollector.emit(
                        DownloadFlow.Progress(partNo, ((progressBytes * 100) / totalBytes).toInt())
                    )
                }
            }
        }
    }

    private fun startDownloadForeground() {
        val initialNotifBuilder = NotificationCompat
            .Builder(this, getString(R.string.strNotifChannelIdDownloads))
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

        startForeground(DOWNLOAD_SCRIPT_NOTIFICATION_ID, initialNotifBuilder.build())
    }

    private fun showProgressNotification(partNo: Int?, progress: Int, scriptKey: String) {
        val builder = NotificationCompat
            .Builder(this, getString(R.string.strNotifChannelIdDownloads))
            .setSmallIcon(R.drawable.dr_logo)
            .setContentTitle(
                if (partNo == null) {
                    getString(R.string.msgDownloadingScript)
                } else {
                    getString(R.string.msgDownloadingFonts)
                }
            )
            .setContentText(scriptKey.getQuranScriptName())
            .setSubText(
                if (partNo == null) {
                    null
                } else {
                    getString(R.string.msgFontsDonwloadProgressShort, partNo, 3)
                }
            )
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setShowWhen(false)
            .setContentIntent(notifActivityIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setGroup(DOWNLOAD_NOTIF_GROUP)

        notifManager.notify(DOWNLOAD_SCRIPT_NOTIFICATION_ID, builder.build())
    }

    fun finish() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    fun cancel() {
        job.cancel(CancellationException("Cancelled by user"))
        finish()
    }

    inner class LocalBinder : Binder() {
        val service get() = this@KFQPCScriptFontsDownloadService
    }
}

sealed class DownloadFlow : java.io.Serializable {
    data class Start(val partNo: Int?) : DownloadFlow()
    data class Progress(val partNo: Int?, val progress: Int) : DownloadFlow()
    data class Complete(val partNo: Int?) : DownloadFlow()
    data class Failed(val partNo: Int?) : DownloadFlow()
}
