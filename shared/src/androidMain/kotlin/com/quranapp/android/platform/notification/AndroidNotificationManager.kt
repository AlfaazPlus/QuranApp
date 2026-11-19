package com.quranapp.android.platform.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.quranapp.android.shared.R

class AndroidNotificationManager(private val appContext: Context) : NotificationManager {
    private val notificationManagerCompat: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(appContext)
    }

    private val nativeNotificationManager: android.app.NotificationManager? by lazy {
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
    }

    init {
        createDefaultChannels()
    }

    override fun notify(id: Int, notification: AppNotification, tag: String?) {
        try {
            val nativeNotif = buildNativeNotification(notification)
            notificationManagerCompat.notify(tag, id, nativeNotif)
        } catch (_: SecurityException) {
            // Android 13+ POST_NOTIFICATIONS permission may be revoked at runtime
        }
    }

    override fun cancel(id: Int, tag: String?) {
        notificationManagerCompat.cancel(tag, id)
    }

    override fun cancelAll() {
        notificationManagerCompat.cancelAll()
    }

    override fun areNotificationsEnabled(): Boolean {
        return notificationManagerCompat.areNotificationsEnabled()
    }

    fun isChannelEnabled(channelId: String): Boolean {
        if (!areNotificationsEnabled()) return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = nativeNotificationManager?.getNotificationChannel(channelId)
                ?: return true
            return channel.importance != android.app.NotificationManager.IMPORTANCE_NONE
        }

        return true
    }

    fun createDefaultChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nativeNotificationManager?.createNotificationChannels(allDefaultChannels())
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun allDefaultChannels(): List<NotificationChannel> {
        val soundUri = Settings.System.DEFAULT_NOTIFICATION_URI
        val audioAttr = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        val vibration = longArrayOf(500, 500)

        fun channel(
            id: String,
            name: String,
            desc: String,
            importance: Int = android.app.NotificationManager.IMPORTANCE_DEFAULT,
            alert: Boolean = false,
        ): NotificationChannel = NotificationChannel(id, name, importance).apply {
            description = desc
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(true)
            enableLights(alert)
            enableVibration(alert)

            if (alert) {
                lightColor = Color.GREEN
                vibrationPattern = vibration
                setSound(soundUri, audioAttr)
            } else {
                setSound(null, null)
            }
        }

        return listOf(
            channel(
                NotificationChannels.CHANNEL_ID_DEFAULT,
                "Default Channel",
                "Miscellaneous notifications",
                alert = true
            ),
            channel(
                NotificationChannels.CHANNEL_ID_VOTD,
                "Verse of The Day",
                "Daily verse reminder notifications",
                android.app.NotificationManager.IMPORTANCE_HIGH,
                alert = true
            ),
            channel(
                NotificationChannels.CHANNEL_ID_RECOMMENDED_REMINDER,
                "Recommended reading",
                "Curated reading suggestions based on time and day",
                android.app.NotificationManager.IMPORTANCE_HIGH,
                alert = true
            ),
            channel(
                NotificationChannels.CHANNEL_ID_DOWNLOADS,
                "Downloads",
                "Notifications for downloads"
            ),
            channel(
                NotificationChannels.CHANNEL_ID_RECITATION_PLAYER,
                "Recitation Player",
                "Recitation Player notifications"
            ),
        )
    }

    fun buildNativeNotification(notification: AppNotification): Notification {
        val channelId = notification.channelId ?: NotificationChannels.CHANNEL_ID_DEFAULT
        val builder = NotificationCompat.Builder(appContext, channelId).apply {
            setContentTitle(notification.title)
            notification.content?.let { setContentText(it) }
            notification.subText?.let { setSubText(it) }

            setSmallIcon(R.drawable.dr_logo)
            setOngoing(notification.ongoing)
            setAutoCancel(notification.autoCancel)
            setOnlyAlertOnce(notification.onlyAlertOnce)

            setPriority(notification.priority.toNativePriority())

            notification.category?.let { cat ->
                val nativeCategory = cat.toNativeCategory()
                if (nativeCategory != null) {
                    setCategory(nativeCategory)
                }
            }

            notification.progress?.let { p ->
                setProgress(p.max, p.current, p.indeterminate)
            }

            notification.bigText?.let {
                setStyle(NotificationCompat.BigTextStyle().bigText(it))
            }

            notification.contentIntent?.let {
                resolvePendingIntent(it, notification.id)?.let { pi ->
                    setContentIntent(pi)
                }
            }

            notification.deleteIntent?.let {
                resolvePendingIntent(it, notification.id xor 0x5A5A)?.let { pi ->
                    setDeleteIntent(pi)
                }
            }

            notification.actions.forEachIndexed { index, action ->
                val actionRequestCode = notification.id * 31 + index
                val actionPendingIntent = resolvePendingIntent(action.intent, actionRequestCode)
                val actionIcon = action.iconRes ?: 0
                addAction(
                    NotificationCompat.Action.Builder(
                        actionIcon,
                        action.title,
                        actionPendingIntent,
                    ).build(),
                )
            }
        }

        return builder.build()
    }

    private fun resolvePendingIntent(intent: NotificationIntent, requestCode: Int): PendingIntent? {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        return when (intent) {
            is NotificationIntent.Platform -> intent.nativePayload as? PendingIntent

            is NotificationIntent.Activity -> {
                val androidIntent = Intent().apply {
                    if (!intent.destination.isNullOrEmpty()) {
                        if (intent.destination.contains('.')) {
                            setClassName(appContext, intent.destination)
                        } else {
                            setClassName(appContext.packageName, intent.destination)
                        }
                    }
                    intent.action?.let { action = it }
                    intent.dataUri?.let { data = Uri.parse(it) }
                    if (intent.flags != 0) {
                        this.flags = intent.flags
                    }
                    intent.extras.forEach { (key, value) ->
                        putIntentExtra(this, key, value)
                    }
                }
                PendingIntent.getActivity(appContext, requestCode, androidIntent, flags)
            }

            is NotificationIntent.Broadcast -> {
                val androidIntent = Intent(intent.action).apply {
                    intent.extras.forEach { (key, value) ->
                        putIntentExtra(this, key, value)
                    }
                }
                PendingIntent.getBroadcast(appContext, requestCode, androidIntent, flags)
            }

            is NotificationIntent.DeepLink -> {
                val androidIntent = Intent(Intent.ACTION_VIEW, Uri.parse(intent.uri))
                PendingIntent.getActivity(appContext, requestCode, androidIntent, flags)
            }
        }
    }

    private fun putIntentExtra(intent: Intent, key: String, value: Any?) {
        when (value) {
            null -> intent.putExtra(key, null as String?)
            is String -> intent.putExtra(key, value)
            is Int -> intent.putExtra(key, value)
            is Long -> intent.putExtra(key, value)
            is Boolean -> intent.putExtra(key, value)
            is Float -> intent.putExtra(key, value)
            is Double -> intent.putExtra(key, value)
            is ByteArray -> intent.putExtra(key, value)
            is IntArray -> intent.putExtra(key, value)
            is LongArray -> intent.putExtra(key, value)
            is Array<*> -> {
                if (value.isArrayOf<String>()) {
                    @Suppress("UNCHECKED_CAST")
                    intent.putExtra(key, value as Array<String>)
                }
            }
        }
    }

    private fun NotificationPriority.toNativePriority(): Int {
        return when (this) {
            NotificationPriority.LOW -> NotificationCompat.PRIORITY_LOW
            NotificationPriority.DEFAULT -> NotificationCompat.PRIORITY_DEFAULT
            NotificationPriority.HIGH -> NotificationCompat.PRIORITY_HIGH
        }
    }

    private fun NotificationCategory.toNativeCategory(): String? {
        return when (this) {
            NotificationCategory.REMINDER -> NotificationCompat.CATEGORY_REMINDER
            NotificationCategory.RECOMMENDATION -> NotificationCompat.CATEGORY_RECOMMENDATION
            NotificationCategory.PROGRESS -> NotificationCompat.CATEGORY_PROGRESS
            NotificationCategory.SERVICE -> NotificationCompat.CATEGORY_SERVICE
            NotificationCategory.EVENT -> NotificationCompat.CATEGORY_EVENT
            NotificationCategory.ERROR -> NotificationCompat.CATEGORY_ERROR
            NotificationCategory.DEFAULT -> null
        }
    }
}
