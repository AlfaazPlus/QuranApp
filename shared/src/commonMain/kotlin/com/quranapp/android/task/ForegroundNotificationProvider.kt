package com.quranapp.android.task

/**
 * Platform hook for customizing native foreground-service notifications.
 *
 * If not set, [AndroidTaskManager] uses a built-in default notification with the app's logo,
 * download channel, and a cancel action button.
 */
interface ForegroundNotificationProvider {

    /**
     * Build the platform foreground descriptor from [info].
     *
     * @return Platform-specific foreground info (e.g. [androidx.work.ForegroundInfo] on Android),
     *         or null to fall back to default platform notification.
     */
    fun buildForegroundInfo(info: ForegroundTaskInfo): Any?

    companion object {
        @Volatile
        private var _instance: ForegroundNotificationProvider? = null

        val instance: ForegroundNotificationProvider?
            get() = _instance

        fun install(provider: ForegroundNotificationProvider) {
            _instance = provider
        }
    }
}

/**
 * Minimal foreground-service descriptor passed to [TaskContext.setForeground].
 *
 * On Android, this is mapped to a system notification and [androidx.work.ForegroundInfo].
 * On platforms without persistent background services, this is a safe no-op.
 */
data class ForegroundTaskInfo(
    /**
     * Stable ID used as the Android notification ID.
     * Must be unique across concurrent foreground workers.
     */
    val notificationId: Int,

    /** Primary notification title. */
    val title: String,

    /** Optional notification body / content text. */
    val body: String? = null,

    /** Optional sub-text shown below the title. */
    val subText: String? = null,

    /** Current progress units. Ignored when [indeterminate] is true. */
    val progressCurrent: Int = 0,

    /** Max progress units. Ignored when [indeterminate] is true. */
    val progressTotal: Int = 100,

    /** Show an indeterminate progress indicator instead of a determinate bar. */
    val indeterminate: Boolean = false,

    /** Whether to show a progress bar at all. */
    val showProgress: Boolean = true,
)
