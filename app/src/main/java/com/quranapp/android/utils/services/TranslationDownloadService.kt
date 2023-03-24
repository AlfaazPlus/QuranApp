package com.quranapp.android.utils.services

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
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
import com.quranapp.android.utils.app.AppActions
import com.quranapp.android.utils.app.NotificationUtils
import com.quranapp.android.utils.extensions.serializableExtra
import com.quranapp.android.utils.reader.factory.QuranTranslationFactory
import com.quranapp.android.utils.receivers.TranslDownloadReceiver
import com.quranapp.android.utils.sharedPrefs.SPAppActions.removeFromPendingAction
import com.quranapp.android.utils.sharedPrefs.SPReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TranslationDownloadService : Service() {
    companion object {
        private var STARTED_BY_USER = false

        fun startDownloadService(wrapper: ContextWrapper, bookInfo: QuranTranslBookInfo) {
            STARTED_BY_USER = true
            val service = Intent(wrapper, TranslationDownloadService::class.java)
            service.putExtra(TranslDownloadReceiver.KEY_TRANSL_BOOK_INFO, bookInfo)
            ContextCompat.startForegroundService(wrapper, service)
        }
    }

    private val mBinder = TranslationDownloadServiceBinder()
    private val mCurrentDownloads = HashSet<String>()

    override fun onCreate() {
        super.onCreate()
        if (STARTED_BY_USER && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(
                1,
                NotificationUtils.createEmptyNotif(
                    this,
                    getString(R.string.strNotifChannelIdDownloads)
                )
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        STARTED_BY_USER = false
    }

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            val notification = NotificationUtils.createEmptyNotif(
                this,
                getString(R.string.strNotifChannelIdDownloads)
            )
            startForeground(1, notification)
            finish()
            return START_NOT_STICKY
        }

        val bookInfo = intent.serializableExtra<QuranTranslBookInfo>(
            TranslDownloadReceiver.KEY_TRANSL_BOOK_INFO
        )
            ?: return START_NOT_STICKY

        mCurrentDownloads.add(bookInfo.slug)

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
        notifManager.cancel(1)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
        startForeground(notifId, notification)
    }

    private fun startDownload(
        bookInfo: QuranTranslBookInfo,
        notifBuilder: NotificationCompat.Builder,
        notifManager: NotificationManagerCompat
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val notifId = bookInfo.slug.hashCode()

            try {
                notify(notifId, notifManager, notifBuilder.build())

                val responseBody = RetrofitInstance.github.getTranslation(bookInfo.downloadPath)

                val data = responseBody.string()

                if (data.isEmpty()) {
                    sendStatusBroadcast(
                        TranslDownloadReceiver.TRANSL_DOWNLOAD_STATUS_FAILED,
                        bookInfo
                    )
                    return@launch
                }

                val context = this@TranslationDownloadService

                val factory = QuranTranslationFactory(context)
                factory.dbHelper.storeTranslation(bookInfo, data)
                factory.close()

                sendStatusBroadcast(TranslDownloadReceiver.TRANSL_DOWNLOAD_STATUS_SUCCEED, bookInfo)

                val slug = bookInfo.slug

                removeFromPendingAction(context, AppActions.APP_ACTION_TRANSL_UPDATE, slug)
                val savedTranslations = SPReader.getSavedTranslations(context)
                if (savedTranslations.remove(slug)) {
                    SPReader.setSavedTranslations(context, savedTranslations)
                }

                removeDownload(bookInfo.slug)
                notifManager.cancel(notifId)
            } catch (e: Exception) {
                e.printStackTrace()
                sendStatusBroadcast(TranslDownloadReceiver.TRANSL_DOWNLOAD_STATUS_FAILED, bookInfo)
                removeDownload(bookInfo.slug)
                notifManager.cancel(notifId)
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
        val channelId = getString(R.string.strNotifChannelIdDownloads)
        val builder = NotificationCompat.Builder(this, channelId).apply {
            setAutoCancel(false)
            setOngoing(true)
            setShowWhen(false)
            setSmallIcon(R.drawable.dr_logo)
            setContentTitle(bookInfo.bookName)
            setSubText(getString(R.string.textDownloading))
            setCategory(NotificationCompat.CATEGORY_PROGRESS)
            setProgress(0, 0, true)
        }

        var flag = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flag = flag or PendingIntent.FLAG_IMMUTABLE
        }

        val activityIntent = Intent(this, ActivitySettings::class.java)
        activityIntent.putExtra(
            ActivitySettings.KEY_SETTINGS_DESTINATION,
            ActivitySettings.SETTINGS_TRANSL_DOWNLOAD
        )
        val pendingIntent = PendingIntent.getActivity(
            this,
            bookInfo.slug.hashCode(),
            activityIntent,
            flag
        )
        builder.setContentIntent(pendingIntent)
        return builder
    }

    fun isDownloading(slug: String): Boolean {
        return mCurrentDownloads.contains(slug)
    }

    fun isAnyDownloading(): Boolean {
        return mCurrentDownloads.isNotEmpty()
    }

    private fun removeDownload(slug: String?) {
        mCurrentDownloads.remove(slug)
        if (mCurrentDownloads.size == 0) {
            sendBroadcast(Intent(TranslDownloadReceiver.ACTION_NO_MORE_DOWNLOADS))
            finish()
        }
    }

    private fun finish() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    inner class TranslationDownloadServiceBinder : Binder() {
        val service: TranslationDownloadService get() = this@TranslationDownloadService
    }
}
