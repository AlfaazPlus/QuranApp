package com.quranapp.android.utils.exceptions

import android.content.Context
import com.quranapp.android.utils.Log

class CustomExceptionHandler(
    private val ctx: Context
) : Thread.UncaughtExceptionHandler {
    private val defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, exc: Throwable) {
        Log.saveCrash(ctx, exc)
        defaultExceptionHandler?.uncaughtException(thread, exc)
    }
}
