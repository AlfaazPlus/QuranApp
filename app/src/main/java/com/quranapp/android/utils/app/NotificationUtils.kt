/*
 * (c) Faisal Khan. Created on 18/2/2022.
 */
package com.quranapp.android.utils.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.quranapp.android.R
import com.quranapp.android.utils.receivers.CrashReceiver

object NotificationUtils {
    const val CHANNEL_ID_DEFAULT = "default"
    const val CHANNEL_ID_VOTD = "votd"
    const val CHANNEL_ID_RECITATION_PLAYER = "recitation_player"
    const val CHANNEL_ID_DOWNLOADS = "downloads"

    fun createNotificationChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.getSystemService(NotificationManager::class.java).apply {
                createNotificationChannel(createDefaultChannel(ctx))
                createNotificationChannel(createVOTDChannel(ctx))
                createNotificationChannel(createDownloadsChannel(ctx))
                createNotificationChannel(createRecitationChannel(ctx))
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun createDefaultChannel(ctx: Context): NotificationChannel {
        return NotificationChannel(
            ctx.getString(R.string.strNotifChannelIdDefault),
            ctx.getString(R.string.strNotifChannelDefault),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = null
            lightColor = Color.GREEN
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            vibrationPattern = longArrayOf(500, 500)

            enableLights(true)
            setShowBadge(true)
            enableVibration(true)

            setSound(
                Settings.System.DEFAULT_NOTIFICATION_URI,
                AudioAttributes.Builder().apply {
                    setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                }.build()
            )
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun createVOTDChannel(ctx: Context): NotificationChannel {
        return NotificationChannel(
            ctx.getString(R.string.strNotifChannelIdVOTD),
            ctx.getString(R.string.strNotifChannelVOTD),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = null
            lightColor = Color.GREEN
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            vibrationPattern = longArrayOf(500, 500)

            enableLights(true)
            setShowBadge(true)
            enableVibration(true)

            setSound(
                Settings.System.DEFAULT_NOTIFICATION_URI,
                AudioAttributes.Builder().apply {
                    setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                }.build()
            )
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun createDownloadsChannel(ctx: Context): NotificationChannel {
        return createChannel(
            ctx.getString(R.string.strNotifChannelIdDownloads),
            ctx.getString(R.string.strNotifChannelDownloads)
        )
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun createRecitationChannel(ctx: Context): NotificationChannel {
        return createChannel(
            ctx.getString(R.string.strNotifChannelIdRecitation),
            "Recitation Player"
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel(channelId: String, channelName: String): NotificationChannel {
        return NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = null
            vibrationPattern = null
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC

            enableLights(false)
            setSound(null, null)
            enableVibration(false)
        }
    }

    @JvmStatic
    fun createEmptyNotif(ctx: Context, channelId: String): Notification {
        return NotificationCompat.Builder(ctx, channelId).apply {
            setContentTitle("")
            setSmallIcon(R.drawable.dr_logo)
            setContentText("")
        }.build()
    }

    fun showCrashNotification(ctx: Context, stackTraceString: String) {
        var flag = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flag = flag or PendingIntent.FLAG_IMMUTABLE
        }

        val copyIntent = Intent(ctx, CrashReceiver::class.java).apply {
            action = CrashReceiver.CRASH_ACTION_COPY_LOG
            putExtra(Intent.EXTRA_TEXT, stackTraceString)
        }

        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID_DEFAULT).apply {
            setContentTitle(ctx.getString(R.string.lastCrashLog))
            setSmallIcon(R.drawable.dr_logo)
            setContentText(stackTraceString)
            setStyle(NotificationCompat.BigTextStyle().bigText(stackTraceString))
            addAction(
                NotificationCompat.Action.Builder(
                    R.drawable.icon_copy,
                    ctx.getString(R.string.strLabelCopy),
                    PendingIntent.getBroadcast(ctx, 0, copyIntent, flag)
                ).build()
            )
        }.build()

        ContextCompat.getSystemService(ctx, NotificationManager::class.java)?.notify(CrashReceiver.NOTIFICATION_ID_CRASH, notification)
    }
}
