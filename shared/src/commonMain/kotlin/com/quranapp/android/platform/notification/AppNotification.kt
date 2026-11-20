package com.quranapp.android.platform.notification

import com.quranapp.android.navigation.AppRoute

data class AppNotification(
    val id: Int,
    val title: String,
    val content: String? = null,
    val subText: String? = null,
    val bigText: String? = null,
    val channelId: String? = null,
    val tag: String? = null,
    val progress: NotificationProgress? = null,
    val ongoing: Boolean = false,
    val autoCancel: Boolean = true,
    val onlyAlertOnce: Boolean = false,
    val category: NotificationCategory? = null,
    val priority: NotificationPriority = NotificationPriority.DEFAULT,
    val contentIntent: NotificationIntent? = null,
    val deleteIntent: NotificationIntent? = null,
    val actions: List<NotificationAction> = emptyList(),
)

data class NotificationProgress(
    val current: Int,
    val max: Int = 100,
    val indeterminate: Boolean = false,
)

data class NotificationAction(
    val title: String,
    val intent: NotificationIntent,
    val iconRes: Int? = null,
)

enum class NotificationPriority {
    LOW,
    DEFAULT,
    HIGH;
}

enum class NotificationCategory {
    DEFAULT,
    REMINDER,
    RECOMMENDATION,
    PROGRESS,
    SERVICE,
    EVENT,
    ERROR;
}

sealed interface NotificationIntent {
    /** Navigate to a typed app route when the notification is tapped. */
    data class Route(val route: AppRoute) : NotificationIntent

    data class Activity(
        val destination: String? = null,
        val action: String? = null,
        val dataUri: String? = null,
        val extras: Map<String, Any?> = emptyMap(),
        val flags: Int = 0,
    ) : NotificationIntent

    data class Broadcast(
        val action: String,
        val extras: Map<String, Any?> = emptyMap(),
    ) : NotificationIntent

    data class DeepLink(
        val uri: String,
    ) : NotificationIntent

    data class Platform(
        val nativePayload: Any,
    ) : NotificationIntent
}
