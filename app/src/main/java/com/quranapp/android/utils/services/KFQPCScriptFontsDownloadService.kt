package com.quranapp.android.utils.services

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.peacedesign.android.utils.Log
import com.quranapp.android.R
import com.quranapp.android.api.RetrofitInstance
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.utils.reader.QuranScriptUtils
import com.quranapp.android.utils.reader.getQuranScriptName
import com.quranapp.android.utils.reader.toKFQPCFontFilename
import com.quranapp.android.utils.receivers.KFQPCScriptFontsDownloadReceiver
import com.quranapp.android.utils.univ.FileUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlin.coroutines.CoroutineContext

class KFQPCScriptFontsDownloadService : Service() {
    companion object {
        private const val DOWNLOAD_NOTIF_GROUP = "download_script_group"
        private const val DOWNLOAD_SCRIPT_NOTIFICATION_ID = 417
        const val PAGE_NO_ALL_DOWNLOADS_FINISHED = -2
    }

    private var mCoroutineContext: CoroutineContext? = null
    private val binder = LocalBinder()

    private val notifManager by lazy { getSystemService(NOTIFICATION_SERVICE) as NotificationManager }

    override fun onCreate() {
        super.onCreate()
        startDownloadForeground()
    }

    override fun onDestroy() {
        super.onDestroy()
        mCoroutineContext = null
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        val script = intent?.getStringExtra(QuranScriptUtils.KEY_SCRIPT)

        Log.d(script)

        if (!script.isNullOrEmpty()) {
            startDownload(script)
        }

        return START_NOT_STICKY
    }

    private fun startDownload(scriptKey: String) {
        val ctx = this

        CoroutineScope(Dispatchers.IO).launch {
            mCoroutineContext = currentCoroutineContext()

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

                        val scriptResBody = RetrofitInstance.github.getQuranScript("script_$scriptKey.json")
                        readStreams(
                            this,
                            null,
                            scriptResBody.byteStream(),
                            scriptFile.outputStream(),
                            scriptResBody.contentLength()
                        )

                        emit(DownloadFlow.Complete(null))
                    }

                } catch (e: Exception) {
                    emit(DownloadFlow.Failed(null))
                    e.printStackTrace()
                    return@flow
                }


                val fontsDir = fileUtils.getKFQPCScriptFontDir(scriptKey)
                for (pageNo in 1..QuranMeta.totalPages()) {
                    try {
                        val fontName = pageNo.toKFQPCFontFilename()
                        val fontResFile = File(fontsDir, fontName)

                        if (fontResFile.length() > 0) continue

                        if (!fileUtils.createFile(fontResFile)) {
                            emit(DownloadFlow.Failed(pageNo))
                            continue
                        }

                        emit(DownloadFlow.Start(pageNo))
                        emit(DownloadFlow.Progress(pageNo, 0))

                        val fontResBody = RetrofitInstance.github.getKFQPCFont(fontName)

                        readStreams(
                            this,
                            pageNo,
                            fontResBody.byteStream(),
                            fontResFile.outputStream(),
                            fontResBody.contentLength()
                        )

                        emit(DownloadFlow.Complete(pageNo))
                    } catch (e: Exception) {
                        e.printStackTrace()
                        emit(DownloadFlow.Failed(pageNo))
                    }
                }

                emit(DownloadFlow.Complete(PAGE_NO_ALL_DOWNLOADS_FINISHED))
            }.collect {
                sendBroadcast(Intent(KFQPCScriptFontsDownloadReceiver.ACTION_DOWNLOAD_STATUS).apply {
                    putExtra(KFQPCScriptFontsDownloadReceiver.KEY_DOWNLOAD_FLOW, it)
                })

                if (it is DownloadFlow.Complete && it.pageNo == PAGE_NO_ALL_DOWNLOADS_FINISHED || it is DownloadFlow.Failed && it.pageNo == null) {
                    finish()
                } else if (it is DownloadFlow.Progress) {
                    showProgressNotification(it.pageNo, it.progress, scriptKey)
                }
            }
        }

    }

    private suspend fun readStreams(
        flowCollector: FlowCollector<DownloadFlow>,
        pageNo: Int?,
        inStream: InputStream,
        outStream: OutputStream,
        totalBytes: Long
    ) {
        inStream.use { inS ->
            outStream.use { outS ->
                val data = ByteArray(8192)
                var progressBytes = 0L

                while (true) {
                    val bytes = inS.read(data)

                    if (bytes == -1) {
                        break
                    }

                    outS.write(data, 0, bytes)
                    progressBytes += bytes

                    flowCollector.emit(DownloadFlow.Progress(pageNo, ((progressBytes * 100) / totalBytes).toInt()))
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
            .setProgress(0, 0, true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setGroupSummary(true)

        startForeground(DOWNLOAD_SCRIPT_NOTIFICATION_ID, initialNotifBuilder.build())
    }

    private fun showProgressNotification(pageNo: Int?, progress: Int, scriptKey: String) {
        /*var flag = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flag = flag or PendingIntent.FLAG_IMMUTABLE
        }

        val activityIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, ActivitySettings::class.java).putExtra(
                ActivitySettings.KEY_SETTINGS_DESTINATION,
                ActivitySettings.SETTINGS_SCRIPT
            ),
            flag
        )*/

        val builder = NotificationCompat
            .Builder(this, getString(R.string.strNotifChannelIdDownloads))
            .setSmallIcon(R.drawable.dr_logo)
            .setContentTitle(getString(R.string.msgDownloadingResource, scriptKey.getQuranScriptName()))
            .setContentText(
                if (pageNo == null) getString(
                    R.string.msgDownloadingScript,
                    scriptKey.getQuranScriptName()
                ) else getString(R.string.msgDownloadingFonts, scriptKey.getQuranScriptName())
            )
            .setSubText(
                if (pageNo == null) ""
                else getString(R.string.msgFontsDonwloadProgressShort, pageNo - 1, QuranMeta.totalPages())
            )
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setContentIntent(null)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .setGroup(DOWNLOAD_NOTIF_GROUP)

        notifManager.notify(DOWNLOAD_SCRIPT_NOTIFICATION_ID, builder.build())
    }

    fun finish() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    fun cancel() {
        mCoroutineContext?.cancel(CancellationException("Cancelled by user"))
        finish()
    }

    inner class LocalBinder : Binder() {
        val service get() = this@KFQPCScriptFontsDownloadService
    }

}

sealed class DownloadFlow : java.io.Serializable {
    data class Start(val pageNo: Int?) : DownloadFlow()
    data class Progress(val pageNo: Int?, val progress: Int) : DownloadFlow()
    data class Complete(val pageNo: Int?) : DownloadFlow()
    data class Failed(val pageNo: Int?) : DownloadFlow()
}
