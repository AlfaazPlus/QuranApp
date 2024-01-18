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
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.quranapp.android.R
import com.quranapp.android.activities.readerSettings.ActivitySettings
import com.quranapp.android.components.recitation.ManageAudioChapterModel
import com.quranapp.android.frags.settings.recitations.manage.FragSettingsManageAudioReciter
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.app.NotificationUtils
import com.quranapp.android.utils.extensions.serializableExtra
import com.quranapp.android.utils.reader.recitation.RecitationUtils
import com.quranapp.android.utils.receivers.RecitationChapterDownloadReceiver.Companion.ACTION_RECITATION_DOWNLOAD_STATUS
import com.quranapp.android.utils.receivers.RecitationChapterDownloadReceiver.Companion.KEY_RECITATION_CHAPTER_MODEL
import com.quranapp.android.utils.receivers.RecitationChapterDownloadReceiver.Companion.KEY_RECITATION_DOWNLOAD_PROGRESS
import com.quranapp.android.utils.receivers.RecitationChapterDownloadReceiver.Companion.KEY_RECITATION_DOWNLOAD_STATUS
import com.quranapp.android.utils.receivers.RecitationChapterDownloadReceiver.Companion.RECITATION_DOWNLOAD_STATUS_CANCELED
import com.quranapp.android.utils.receivers.RecitationChapterDownloadReceiver.Companion.RECITATION_DOWNLOAD_STATUS_FAILED
import com.quranapp.android.utils.receivers.RecitationChapterDownloadReceiver.Companion.RECITATION_DOWNLOAD_STATUS_PROGRESS
import com.quranapp.android.utils.receivers.RecitationChapterDownloadReceiver.Companion.RECITATION_DOWNLOAD_STATUS_SUCCEED
import com.quranapp.android.utils.univ.FileUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

class RecitationChapterDownloadService : Service() {
    companion object {
        private const val NOTIF_ID = 1

        // To prevent notification when using BIND_AUTO_CREATE
        private var STARTED_BY_USER = false

        fun startDownloadService(
            wrapper: ContextWrapper,
            chapterModel: ManageAudioChapterModel,
        ) {
            STARTED_BY_USER = true

            val service = Intent(wrapper, RecitationChapterDownloadService::class.java)
            service.putExtra(KEY_RECITATION_CHAPTER_MODEL, chapterModel)
            ContextCompat.startForegroundService(wrapper, service)
        }
    }

    private val binder = LocalBinder()
    private var fileUtils: FileUtils? = null
    private var notifManager: NotificationManagerCompat? = null
    private var jobs = hashMapOf<String, Job>()
    private var downloadProgress = hashMapOf<String, Int>()

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

        fileUtils = FileUtils.newInstance(this)
        notifManager = NotificationManagerCompat.from(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        STARTED_BY_USER = false
    }

    override fun onBind(intent: Intent) = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            val notification = NotificationUtils.createEmptyNotif(
                this,
                NotificationUtils.CHANNEL_ID_DOWNLOADS
            )
            ServiceCompat.startForeground(
                this,
                NOTIF_ID,
                notification,
                FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
            finish()
            return START_NOT_STICKY
        }

        val chapterModel: ManageAudioChapterModel = intent.serializableExtra(KEY_RECITATION_CHAPTER_MODEL)!!

        val key = makeKey(chapterModel.reciterModel.slug, chapterModel.chapterMeta.chapterNo)
        val notifBuilder = prepareNotification(key, chapterModel)
        showNotification(key.hashCode(), notifBuilder.build())
        startDownload(key, chapterModel, notifBuilder)

        return START_NOT_STICKY
    }

    private fun startDownload(
        key: String,
        chapterModel: ManageAudioChapterModel,
        notifBuilder: NotificationCompat.Builder
    ) {

        val job = Job()
        jobs[key] = job

        val notifId = key.hashCode()
        val verseCount = chapterModel.chapterMeta.verseCount

        CoroutineScope(Dispatchers.Main + job).launch {
            val filesInProgress = HashMap<String, File>()

            try {
                var downloadedCount = 0
                val subJobs = mutableListOf<Job>()

                for (verseNo in 1..verseCount) {
                    subJobs.add(launch {
                        try {
                            download(chapterModel, verseNo, filesInProgress)
                            downloadedCount++

                            val progress = downloadedCount * 100 / verseCount

                            downloadProgress[key] = progress

                            notifBuilder.setProgress(100, progress, false)
                            notifBuilder.setSubText("$progress%")

                            notify(notifId, notifBuilder.build())
                            sendStatusBroadcast(chapterModel, RECITATION_DOWNLOAD_STATUS_PROGRESS, progress)
                        } catch (e: Exception) {
                            job.cancel(CancellationException("failure", e))
                        }
                    })
                }

                subJobs.joinAll()
                sendStatusBroadcast(chapterModel, RECITATION_DOWNLOAD_STATUS_SUCCEED, -1)
            } catch (e: Exception) {
                filesInProgress.values.forEach { file -> file.delete() }

                if (e !is CancellationException || e.message == "failure") {
                    Log.saveError(e, "RecitationChapterDownload")
                    sendStatusBroadcast(chapterModel, RECITATION_DOWNLOAD_STATUS_FAILED, -1)
                }
            } finally {
                removeJob(key, notifId)
            }
        }

    }

    private suspend fun download(
        chapterModel: ManageAudioChapterModel,
        verseNo: Int,
        filesInProgress: HashMap<String, File>
    ) {
        val chapterNo = chapterModel.chapterMeta.chapterNo

        val verseFile = fileUtils?.getRecitationAudioFile(chapterModel.reciterModel.slug, chapterNo, verseNo) ?: return

        if (verseFile.length() > 0 || fileUtils?.createFile(verseFile) != true) return

        filesInProgress["$chapterNo:$verseNo"] = verseFile

        val audioUrl =
            RecitationUtils.prepareRecitationAudioUrl(chapterModel.reciterModel, chapterNo, verseNo) ?: return

        withContext(Dispatchers.IO) {
            val inputStream = URL(audioUrl).openStream()
            val buffer = ByteArray(1024)

            inputStream.use { input ->
                verseFile.outputStream().use { output ->
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                    }
                    output.flush()

                    filesInProgress.remove("$chapterNo:$verseNo")
                }
            }
        }
    }

    private fun makeKey(slug: String, chapterNo: Int): String {
        return "$slug:$chapterNo"
    }

    private fun showNotification(
        notifId: Int,
        notification: Notification,
    ) {
        notifManager?.cancel(NOTIF_ID)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
        ServiceCompat.startForeground(
            this,
            notifId,
            notification,
            FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    private fun notify(
        notifId: Int,
        notification: Notification
    ) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        notifManager?.notify(notifId, notification)
    }

    private fun sendStatusBroadcast(chapterModel: ManageAudioChapterModel, status: String, progress: Int) {
        sendBroadcast(
            Intent(ACTION_RECITATION_DOWNLOAD_STATUS).apply {
                putExtra(KEY_RECITATION_DOWNLOAD_STATUS, status)
                putExtra(KEY_RECITATION_CHAPTER_MODEL, chapterModel)
                putExtra(KEY_RECITATION_DOWNLOAD_PROGRESS, progress)
            }
        )
    }

    private fun prepareNotification(
        key: String,
        chapterModel: ManageAudioChapterModel
    ): NotificationCompat.Builder {
        val channelId = NotificationUtils.CHANNEL_ID_DOWNLOADS
        val builder = NotificationCompat.Builder(this, channelId).apply {
            setAutoCancel(false)
            setOngoing(true)
            setShowWhen(false)
            setSmallIcon(R.drawable.dr_logo)
            setContentTitle(chapterModel.reciterModel.getReciterName())
            setContentText(chapterModel.chapterMeta.name)
            setCategory(NotificationCompat.CATEGORY_PROGRESS)
            setProgress(0, 0, true)
        }

        var flag = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flag = flag or PendingIntent.FLAG_IMMUTABLE
        }

        val activityIntent = Intent(this, ActivitySettings::class.java).apply {
            putExtra(ActivitySettings.KEY_SETTINGS_DESTINATION, ActivitySettings.SETTINGS_MANAGE_AUDIO_RECITER)
            putExtra(FragSettingsManageAudioReciter.KEY_RECITATION_INFO_MODEL, chapterModel.reciterModel)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            key.hashCode(),
            activityIntent,
            flag
        )
        builder.setContentIntent(pendingIntent)

        val cancelIntent = PendingIntent.getBroadcast(
            this,
            key.hashCode(),
            Intent(ACTION_RECITATION_DOWNLOAD_STATUS).apply {
                putExtra(KEY_RECITATION_DOWNLOAD_STATUS, RECITATION_DOWNLOAD_STATUS_CANCELED)
                putExtra(KEY_RECITATION_CHAPTER_MODEL, chapterModel)
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

    fun isDownloading(slug: String, chapterNo: Int): Boolean {
        val job = jobs[makeKey(slug, chapterNo)]
        return job != null && job.isActive
    }

    fun getDownloadProgress(slug: String, chapterNo: Int): Int {
        return downloadProgress[makeKey(slug, chapterNo)] ?: 0
    }

    fun cancelDownload(slug: String, chapterNo: Int) {
        val key = makeKey(slug, chapterNo)
        jobs[key]?.cancel()
        removeJob(key, key.hashCode())
    }

    private fun removeJob(key: String, notifId: Int) {
        jobs.remove(key)
        notifManager?.cancel(notifId)
        downloadProgress.remove(key)
        checkIfFinished()
    }

    private fun checkIfFinished() {
        if (jobs.isEmpty()) {
//            sendBroadcast(Intent(RecitationChapterDownloadReceiver.ACTION_NO_MORE_DOWNLOADS))
            finish()
        }
    }

    private fun finish() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    inner class LocalBinder : Binder() {
        val service: RecitationChapterDownloadService get() = this@RecitationChapterDownloadService
    }
}