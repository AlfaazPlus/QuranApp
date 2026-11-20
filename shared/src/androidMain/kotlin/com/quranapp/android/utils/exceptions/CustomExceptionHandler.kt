package com.quranapp.android.utils.exceptions

import android.content.Context
import android.content.Intent
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.lastCrashLog
import com.quranapp.android.generated.resources.strLabelCopy
import com.quranapp.android.platform.Platform
import com.quranapp.android.platform.notification.AppNotification
import com.quranapp.android.platform.notification.NotificationAction
import com.quranapp.android.platform.notification.NotificationChannels
import com.quranapp.android.platform.notification.NotificationIntent
import com.quranapp.android.shared.R
import com.quranapp.android.utils.app.Logger
import com.quranapp.android.utils.receivers.CrashReceiver
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.exception.ExceptionUtils
import org.jetbrains.compose.resources.getString

class CustomExceptionHandler(
    private val ctx: Context
) : Thread.UncaughtExceptionHandler {
    private val defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, exc: Throwable) {
        Logger.saveCrash(exc)

        runBlocking {
            showCrashNotification(ctx, ExceptionUtils.getStackTrace(exc))
        }

        defaultExceptionHandler?.uncaughtException(thread, exc)
    }

    private suspend fun showCrashNotification(ctx: Context, stackTraceString: String) {
        val copyAction = NotificationAction(
            title = getString(Res.string.strLabelCopy),
            intent = NotificationIntent.Broadcast(
                action = CrashReceiver.CRASH_ACTION_COPY_LOG,
                extras = mapOf(Intent.EXTRA_TEXT to stackTraceString),
            ),
            iconRes = R.drawable.icon_copy,
        )

        val notification = AppNotification(
            id = CrashReceiver.NOTIFICATION_ID_CRASH,
            title = getString(Res.string.lastCrashLog),
            content = stackTraceString,
            bigText = stackTraceString,
            channelId = NotificationChannels.CHANNEL_ID_DEFAULT,
            actions = listOf(copyAction),
        )

        Platform.system.notificationManager.notify(
            CrashReceiver.NOTIFICATION_ID_CRASH,
            notification,
        )
    }
}
