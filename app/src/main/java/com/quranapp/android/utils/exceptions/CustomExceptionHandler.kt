package com.quranapp.android.utils.exceptions

import android.content.Context
import com.quranapp.android.utils.sharedPrefs.SPLog

class CustomExceptionHandler(
    private val ctx: Context,
) : Thread.UncaughtExceptionHandler {
    private val defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, exc: Throwable) {
        SPLog.saveLastCrashLog(ctx, exc.stackTraceToString())
        defaultExceptionHandler?.uncaughtException(thread, exc)
    }
}