package com.quranapp.android.utils

import com.quranapp.android.BuildConfig

object Log {
    const val TAG = "QuranAppLogs"

    @JvmStatic
    fun d(vararg messages: Any?) {
        if(!BuildConfig.DEBUG) return

        val sb = StringBuilder()

        val trc = Thread.currentThread().stackTrace[3]
        var className = trc.className
        className = className.substring(className.lastIndexOf(".") + 1)
        sb.append("(")
            .append(className)
            .append("=>")
            .append(trc.methodName)
            .append(":")
            .append(trc.lineNumber)
            .append("): ")

        val len = messages.size
        for (i in messages.indices) {
            val msg = messages[i]
            if (msg != null) sb.append(msg.toString()) else sb.append("null")
            if (i < len - 1) sb.append(", ")
        }

        android.util.Log.d(TAG, sb.toString())
    }
}
