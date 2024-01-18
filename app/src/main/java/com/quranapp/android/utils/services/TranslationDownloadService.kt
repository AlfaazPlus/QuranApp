package com.quranapp.android.utils.services

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.quranapp.android.R
import com.quranapp.android.activities.readerSettings.ActivitySettings
import com.quranapp.android.api.RetrofitInstance
import com.quranapp.android.components.quran.subcomponents.QuranTranslBookInfo
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.app.AppActions
import com.quranapp.android.utils.app.NotificationUtils
import com.quranapp.android.utils.extensions.getContentLengthAndStream
import com.quranapp.android.utils.extensions.serializableExtra
import com.quranapp.android.utils.reader.factory.QuranTranslationFactory
import com.quranapp.android.utils.receivers.TranslDownloadReceiver
import com.quranapp.android.utils.sharedPrefs.SPAppActions.removeFromPendingAction
import com.quranapp.android.utils.sharedPrefs.SPReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlin.io.path.deleteIfExists
import kotlin.io.path.outputStream
import kotlin.io.path.readText

class TranslationDownloadService : Service() {
    companion object {
        private const val NOTIF_ID = 44

        // To prevent notification when using BIND_AUTO_CREATE
        private var STARTED_BY_USER = false
        fun startDownloadService(wrapper: ContextWrapper, bookInfo: QuranTranslBookInfo) {
            STARTED_BY_USER = true
            val service = Intent(wrapper, TranslationDownloadService::class.java)
            service.putExtra(TranslDownloadReceiver.KEY_TRANSL_BOOK_INFO, bookInfo)
            ContextCompat.startForegroundService(wrapper, service)
        }
    }

    private val binder = LocalBinder()
    private val currentDownloads = HashSet<String>()
    private val jobs = HashMap<String, Job>()

    override fun onCreate() {
        super.onCreate()
        if (STARTED_BY_USER && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ServiceCompat.startForeground(
                this,
                NOTIF_ID,
                NotificationUtils.createEmptyNotif(this, NotificationUtils.CHANNEL_ID_DOWNLOADS),
                FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        STARTED_BY_USER = false
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            val notification = NotificationUtils.createEmptyNotif(
                this,
                NotificationUtils.CHANNEL_ID_DOWNLOADS
            )
            ServiceCompat.startForeground(this, NOTIF_ID, notification, FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            finish()
            return START_NOT_STICKY
        }

        val bookInfo = intent.serializableExtra<QuranTranslBookInfo>(
            TranslDownloadReceiver.KEY_TRANSL_BOOK_INFO
        )
            ?: return START_NOT_STICKY

        currentDownloads.add(bookInfo.slug)
        jobs[bookInfo.slug] = Job()

        val notifBuilder = prepareNotification(bookInfo)
        val notifManager = NotificationManagerCompat.from(this)

        showNotification(bookInfo.slug.hashCode(), notifBuilder.build(), notifManager)
        startDownload(bookInfo, notifBuilder, notifManager)

        return START_NOT_STICKY
    }

    private fun showNotification(
        notifId: Int,
        notification: Notification,
        notifManager: NotificationManagerCompat
    ) {
        notifManager.cancel(NOTIF_ID)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
        ServiceCompat.startForeground(this, notifId, notification, FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    private fun startDownload(
        bookInfo: QuranTranslBookInfo,
        notifBuilder: NotificationCompat.Builder,
        notifManager: NotificationManagerCompat
    ) {
        CoroutineScope(Dispatchers.Main + jobs[bookInfo.slug]!!).launch {
            val notifId = bookInfo.slug.hashCode()
            val tmpFile = kotlin.io.path.createTempFile(
                prefix = bookInfo.slug,
                suffix = ".json"
            )

            flow {
                emit(TranslationDownloadFlow.Start)
                emit(TranslationDownloadFlow.Progress(0))


                val (totalBytes, byteStream) = RetrofitInstance.github.getTranslation(
                    bookInfo.downloadPath
                ).getContentLengthAndStream()


                byteStream.use { inS ->
                    tmpFile.outputStream().buffered().use { outS ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var progressBytes = 0L

                        while (true) {
                            val bytes = inS.read(buffer)

                            if (bytes <= 0) break

                            outS.write(buffer, 0, bytes)
                            progressBytes += bytes

                            emit(
                                TranslationDownloadFlow.Progress(
                                    if (totalBytes > 0) ((progressBytes * 100) / totalBytes).toInt() else 0
                                )
                            )
                        }

                        outS.flush()
                    }
                }

                val context = this@TranslationDownloadService

                QuranTranslationFactory(context).use {
                    it.dbHelper.storeTranslation(bookInfo, tmpFile.readText())
                }

                removeFromPendingAction(context, AppActions.APP_ACTION_TRANSL_UPDATE, bookInfo.slug)
                val savedTranslations = SPReader.getSavedTranslations(context)
                if (savedTranslations.remove(bookInfo.slug)) {
                    SPReader.setSavedTranslations(context, savedTranslations)
                }

                removeDownload(bookInfo.slug)
                notifManager.cancel(notifId)

                emit(TranslationDownloadFlow.Complete)
            }.flowOn(Dispatchers.IO).catch {
                it.printStackTrace()
                Log.saveError(it, "TranslationDownloadService")
                emit(TranslationDownloadFlow.Failed)
            }.collect {
                when (it) {
                    is TranslationDownloadFlow.Start -> {
                        notify(notifId, notifManager, notifBuilder.build())
                    }

                    is TranslationDownloadFlow.Progress -> {
                        notifBuilder.setProgress(100, it.progress, false)
                        notifBuilder.setSubText("${it.progress}%")
                        notify(notifId, notifManager, notifBuilder.build())
                    }

                    is TranslationDownloadFlow.Complete -> {
                        tmpFile.deleteIfExists()
                        sendStatusBroadcast(TranslDownloadReceiver.TRANSL_DOWNLOAD_STATUS_SUCCEED, bookInfo)
                        finish()
                    }

                    is TranslationDownloadFlow.Failed -> {
                        tmpFile.deleteIfExists()
                        sendStatusBroadcast(TranslDownloadReceiver.TRANSL_DOWNLOAD_STATUS_FAILED, bookInfo)
                        removeDownload(bookInfo.slug)
                        notifManager.cancel(notifId)
                        finish()
                    }
                }
            }
        }
    }

    private fun notify(
        notifId: Int,
        notifManager: NotificationManagerCompat,
        notification: Notification
    ) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        notifManager.notify(notifId, notification)
    }

    private fun sendStatusBroadcast(status: String, bookInfo: QuranTranslBookInfo) {
        sendBroadcast(
            Intent(TranslDownloadReceiver.ACTION_TRANSL_DOWNLOAD_STATUS).apply {
                putExtra(TranslDownloadReceiver.KEY_TRANSL_BOOK_INFO, bookInfo)
                putExtra(TranslDownloadReceiver.KEY_TRANSL_DOWNLOAD_STATUS, status)
            }
        )
    }

    private fun prepareNotification(bookInfo: QuranTranslBookInfo): NotificationCompat.Builder {
        val channelId = NotificationUtils.CHANNEL_ID_DOWNLOADS
        val builder = NotificationCompat.Builder(this, channelId).apply {
            setAutoCancel(false)
            setOngoing(true)
            setShowWhen(false)
            setSmallIcon(R.drawable.dr_logo)
            setContentTitle(getString(R.string.textDownloading))
            setContentText(bookInfo.bookName)
            setCategory(NotificationCompat.CATEGORY_PROGRESS)
            setProgress(0, 0, true)
        }

        var flag = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flag = flag or PendingIntent.FLAG_IMMUTABLE
        }

        val activityIntent = Intent(this, ActivitySettings::class.java).apply {
            putExtra(
                ActivitySettings.KEY_SETTINGS_DESTINATION,
                ActivitySettings.SETTINGS_TRANSLATION_DOWNLOAD
            )
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            bookInfo.slug.hashCode(),
            activityIntent,
            flag
        )
        builder.setContentIntent(pendingIntent)

        val cancelIntent = PendingIntent.getBroadcast(
            this@TranslationDownloadService,
            bookInfo.slug.hashCode(),
            Intent(TranslDownloadReceiver.ACTION_TRANSL_DOWNLOAD_STATUS).apply {
                putExtra(
                    TranslDownloadReceiver.KEY_TRANSL_DOWNLOAD_STATUS,
                    TranslDownloadReceiver.TRANSL_DOWNLOAD_STATUS_CANCELED
                )
                putExtra(TranslDownloadReceiver.KEY_TRANSL_BOOK_INFO, bookInfo)
            },
            flag
        )

        builder.addAction(
            R.drawable.dr_icon_close,
            getString(R.string.strLabelCancel),
            cancelIntent
        )

        return builder
    }

    fun isDownloading(slug: String): Boolean {
        return currentDownloads.contains(slug) && jobs[slug]?.isActive == true
    }

    fun isAnyDownloading(): Boolean {
        return currentDownloads.isNotEmpty() && jobs.values.any { it.isActive }
    }

    fun cancelDownload(slug: String) {
        currentDownloads.remove(slug)
        jobs[slug]?.cancel()
        checkIfFinished()
    }

    private fun removeDownload(slug: String?) {
        currentDownloads.remove(slug)
        checkIfFinished()
    }

    private fun checkIfFinished() {
        if (currentDownloads.size == 0) {
            sendBroadcast(Intent(TranslDownloadReceiver.ACTION_NO_MORE_DOWNLOADS))
            finish()
        }
    }

    private fun finish() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    inner class LocalBinder : Binder() {
        val service: TranslationDownloadService get() = this@TranslationDownloadService
    }
}

sealed class TranslationDownloadFlow : java.io.Serializable {
    object Start : TranslationDownloadFlow()
    data class Progress(val progress: Int) : TranslationDownloadFlow()
    object Complete : TranslationDownloadFlow()
    object Failed : TranslationDownloadFlow()
}
