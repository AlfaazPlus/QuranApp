package com.quranapp.android.utils.exceptions

import android.content.Context
import com.quranapp.android.utils.app.NotificationUtils
import com.quranapp.android.utils.sharedPrefs.SPLog
import org.apache.commons.lang3.exception.ExceptionUtils

class CustomExceptionHandler(
    private val ctx: Context
) : Thread.UncaughtExceptionHandler {
    private val defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, exc: Throwable) {
        val stackTraceString = ExceptionUtils.getStackTrace(exc);
        SPLog.saveLastCrashLog(ctx, stackTraceString)
        NotificationUtils.showCrashNotification(ctx, stackTraceString)
        defaultExceptionHandler?.uncaughtException(thread, exc)
    }
}
