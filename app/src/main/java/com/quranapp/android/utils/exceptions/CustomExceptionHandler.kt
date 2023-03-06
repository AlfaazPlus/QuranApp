package com.quranapp.android.utils.exceptions

import android.content.Context
import com.quranapp.android.utils.sharedPrefs.SPLog
import org.apache.commons.lang3.exception.ExceptionUtils

class CustomExceptionHandler(
    private val ctx: Context,
) : Thread.UncaughtExceptionHandler {
    private val defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, exc: Throwable) {
        SPLog.saveLastCrashLog(ctx, ExceptionUtils.getStackTrace(exc))
        defaultExceptionHandler?.uncaughtException(thread, exc)
    }
}