package com.quranapp.android.platform.notification

/**
 * Platform-agnostic interface for posting and cancelling notifications.
 */
interface NotificationManager {
    fun notify(id: Int, notification: AppNotification, tag: String? = null)
    fun cancel(id: Int, tag: String? = null)
    fun cancelAll()
    fun areNotificationsEnabled(): Boolean
}
