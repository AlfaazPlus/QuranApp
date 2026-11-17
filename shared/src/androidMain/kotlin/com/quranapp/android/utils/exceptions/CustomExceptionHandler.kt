package com.quranapp.android.utils.exceptions

import android.content.Context
import com.quranapp.android.utils.app.Logger
import com.quranapp.android.utils.app.NotificationUtils
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.exception.ExceptionUtils

class CustomExceptionHandler(
    private val ctx: Context
) : Thread.UncaughtExceptionHandler {
    private val defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, exc: Throwable) {
        Logger.saveCrash(exc)
        runBlocking {
            NotificationUtils.showCrashNotification(ctx, ExceptionUtils.getStackTrace(exc))
        }
        defaultExceptionHandler?.uncaughtException(thread, exc)
    }
}
