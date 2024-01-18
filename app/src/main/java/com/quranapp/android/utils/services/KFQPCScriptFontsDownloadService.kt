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
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.quranapp.android.R
import com.quranapp.android.activities.readerSettings.ActivitySettings
import com.quranapp.android.api.RetrofitInstance
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.app.NotificationUtils
import com.quranapp.android.utils.extensions.getContentLengthAndStream
import com.quranapp.android.utils.reader.QuranScriptUtils
import com.quranapp.android.utils.reader.getQuranScriptName
import com.quranapp.android.utils.reader.toKFQPCFontFilename
import com.quranapp.android.utils.receivers.KFQPCScriptFontsDownloadReceiver
import com.quranapp.android.utils.univ.FileUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipFile

class KFQPCScriptFontsDownloadService : LifecycleService() {
    companion object {
        private const val DOWNLOAD_NOTIF_GROUP = "download_script_group"
        private const val DOWNLOAD_SCRIPT_NOTIFICATION_ID = 417
        const val ALL_PART_DOWNLOADS_FINISHED = -2

        // This is to prevent foreground and notification when binding to this service
        var STARTED_BY_USER = false
    }

    private var job = Job()
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
        job.cancel(CancellationException("Cancelled on destroy"))
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

        lifecycleScope.launch(Dispatchers.Main + job) {
            flow {
                val fileUtils = FileUtils.newInstance(ctx)
                val scriptFile = fileUtils.getScriptFile(scriptKey)

                try {
                    downloadScript(this@flow, fileUtils, scriptFile, scriptKey)
                } catch (e: Exception) {
                    emit(DownloadFlow.Failed(null))
                    e.printStackTrace()
                    return@flow
                }

                val fontsDir = fileUtils.getKFQPCScriptFontDir(scriptKey)
                val skipToPart = getSkipPartNumber(fontsDir)

                for (partNo in 1..QuranScriptUtils.TOTAL_DOWNLOAD_PARTS) {
                    if (partNo < skipToPart) continue
                    downloadFontsPart(this, scriptKey, partNo, fontsDir)
                }

                emit(DownloadFlow.Complete(ALL_PART_DOWNLOADS_FINISHED))
            }.flowOn(Dispatchers.IO)
                .flowWithLifecycle(lifecycle)
                .catch {
                    Log.saveError(it, "KFQPCScriptFontsDownloadService")
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

    private suspend fun downloadScript(
        flow: FlowCollector<DownloadFlow>,
        fileUtils: FileUtils,
        scriptFile: File,
        scriptKey: String
    ) {

        if (scriptFile.length() > 0) return

        if (!fileUtils.createFile(scriptFile)) {
            flow.emit(DownloadFlow.Failed(null))
            return
        }

        flow.emit(DownloadFlow.Start(null))
        flow.emit(DownloadFlow.Progress(null, 0))

        val (totalBytes, byteStream) = RetrofitInstance.github.getQuranScript(
            "script_$scriptKey.json"
        ).getContentLengthAndStream()

        readStreams(
            flow,
            null,
            byteStream,
            scriptFile.outputStream(),
            totalBytes
        )

        flow.emit(DownloadFlow.Complete(null))
    }

    private fun getSkipPartNumber(fontsDir: File): Int {
        val totalPages = QuranMeta.totalPages()
        var totalDownloaded = 0
        for (pageNo in 1..totalPages) {
            if (File(fontsDir, pageNo.toKFQPCFontFilename()).length() == 0L) {
                break
            }

            totalDownloaded++
        }

        val fontsInSingleZip = totalPages / QuranScriptUtils.TOTAL_DOWNLOAD_PARTS
        return (totalDownloaded / fontsInSingleZip) + 1
    }

    private suspend fun downloadFontsPart(
        flow: FlowCollector<DownloadFlow>,
        scriptKey: String,
        partNo: Int,
        fontsDir: File
    ) {
        val partFilename = "$scriptKey-$partNo.zip"
        val partFile = File.createTempFile("tmp", partFilename, filesDir)
        tmpFiles.add(partFile)

        flow.emit(DownloadFlow.Start(partNo))
        flow.emit(DownloadFlow.Progress(partNo, 0))

        val (totalBytes, byteStream) = RetrofitInstance.github.getKFQPCFont(
            scriptKey,
            partFilename
        ).getContentLengthAndStream()

        readStreams(
            flow,
            partNo,
            byteStream,
            partFile.outputStream(),
            totalBytes
        )

        extractFonts(partFile, fontsDir)
        flow.emit(DownloadFlow.Complete(partNo))
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
                        partFile.delete()

                        tmpFiles.remove(partFile)
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
        totalBytes: Long
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

                    val progress = if (totalBytes > 0) ((progressBytes * 100) / totalBytes).toInt() else 0
                    flowCollector.emit(
                        DownloadFlow.Progress(partNo, progress)
                    )
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

    private fun showProgressNotification(partNo: Int?, progress: Int, scriptKey: String) {
        val builder = NotificationCompat
            .Builder(this, NotificationUtils.CHANNEL_ID_DOWNLOADS)
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
                    getString(R.string.msgFontsDonwloadProgressShort, partNo, QuranScriptUtils.TOTAL_DOWNLOAD_PARTS)
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
