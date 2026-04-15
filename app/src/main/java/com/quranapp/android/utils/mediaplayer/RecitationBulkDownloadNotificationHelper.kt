package com.quranapp.android.utils.mediaplayer

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.WorkManager
import com.quranapp.android.R
import com.quranapp.android.api.models.mediaplayer.RecitationAudioKind
import com.quranapp.android.utils.app.NotificationUtils
import com.quranapp.android.utils.quran.QuranMeta
import com.quranapp.android.utils.workers.RecitationAudioDownloadWorker
import com.quranapp.android.utils.workers.RecitationBulkDownloadWorker

/**
 * Shows a single ongoing notification with overall chapter progress (from files on disk) during bulk
 * downloads, and exposes one cancel action that stops the bulk coordinator and all chapter workers
 * for that reciter.
 */
object RecitationBulkDownloadNotificationHelper {
    private fun notificationId(reciterId: String, kind: RecitationAudioKind): Int {
        return ("bulk_rec:$reciterId:${kind.name}").hashCode()
    }

    fun cancelNotification(context: Context, reciterId: String, kind: RecitationAudioKind) {
        NotificationManagerCompat.from(context).cancel(notificationId(reciterId, kind))
    }

    /**
     * Stops the bulk download coordinator and every chapter download for [reciterId] / [kind],
     * and dismisses the summary notification.
     */
    fun cancelAllBulkWork(context: Context, reciterId: String, kind: RecitationAudioKind) {
        val app = context.applicationContext
        val wm = WorkManager.getInstance(app)

        wm.cancelUniqueWork(RecitationBulkDownloadWorker.uniqueWorkName(reciterId, kind))
        wm.cancelAllWorkByTag(RecitationAudioDownloadWorker.reciterTag(reciterId, kind))
        wm.cancelAllWorkByTag(RecitationBulkDownloadWorker.reciterTag(reciterId, kind))

        cancelNotification(app, reciterId, kind)
    }

    fun createCancelAllBulkPendingIntent(
        context: Context,
        reciterId: String,
        kind: RecitationAudioKind,
    ): PendingIntent {
        val cancelIntent = Intent(context, CancelReceiver::class.java).apply {
            action = CancelReceiver.ACTION_CANCEL_BULK
            putExtra(CancelReceiver.EXTRA_RECITER_ID, reciterId)
            putExtra(CancelReceiver.EXTRA_KIND, kind.name)
        }

        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }

        return PendingIntent.getBroadcast(
            context.applicationContext,
            notificationId(reciterId, kind),
            cancelIntent,
            flags,
        )
    }

    /**
     * Refresh overall progress from completed files on disk (and dismiss when done).
     */
    fun updateOverall(
        context: Context,
        reciterId: String,
        kind: RecitationAudioKind,
        displayName: String,
    ) {
        val total = QuranMeta.chapterRange.last
        val modelManager = RecitationModelManager.get(context)
        var downloaded = 0

        for (chapterNo in QuranMeta.chapterRange) {
            val f = modelManager.getRecitationAudioFile(reciterId, chapterNo)
            if (f.exists() && f.length() > 0L) downloaded++
        }

        val nm = NotificationManagerCompat.from(context)
        val id = notificationId(reciterId, kind)

        if (downloaded >= total) {
            nm.cancel(id)
            return
        }

        val cancelPi = createCancelAllBulkPendingIntent(context, reciterId, kind)

        val builder =
            NotificationCompat.Builder(context, NotificationUtils.CHANNEL_ID_DOWNLOADS).apply {
                setSmallIcon(R.drawable.dr_logo)
                setContentTitle(displayName)
                setContentText(
                    context.getString(
                        R.string.recitationBulkDownloadNotifProgress,
                        downloaded,
                        total,
                    ),
                )
                setProgress(total, downloaded, false)
                setOngoing(true)
                setOnlyAlertOnce(true)
                setShowWhen(false)
                setCategory(NotificationCompat.CATEGORY_PROGRESS)
                addAction(
                    R.drawable.dr_icon_close,
                    context.getString(R.string.strLabelCancel),
                    cancelPi,
                )
            }

        nm.notify(id, builder.build())
    }

    class CancelReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent?.action != ACTION_CANCEL_BULK) return

            val reciterId = intent.getStringExtra(EXTRA_RECITER_ID) ?: return
            val kindName = intent.getStringExtra(EXTRA_KIND) ?: return
            val kind = RecitationAudioKind.valueOf(kindName)

            cancelAllBulkWork(context, reciterId, kind)
        }

        companion object {
            const val ACTION_CANCEL_BULK = "com.quranapp.android.action.CANCEL_BULK_RECITATION"
            const val EXTRA_RECITER_ID = "extra.reciter_id"
            const val EXTRA_KIND = "extra.kind"
        }
    }
}

