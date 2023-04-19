package com.quranapp.android.utils

import android.content.Context
import com.quranapp.android.BuildConfig
import com.quranapp.android.utils.app.AppUtils
import com.quranapp.android.utils.sharedPrefs.SPLog
import com.quranapp.android.utils.univ.DateUtils
import com.quranapp.android.utils.univ.FileUtils
import java.io.File

object Log {
    private const val TAG = "QuranAppLogs"
    const val FILE_NAME_DATE_FORMAT = "yyyyMMddHHmmssSSS"
    val CRASH_ERROR_DIR: File = FileUtils.makeAndGetAppResourceDir(
        FileUtils.createPath(AppUtils.BASE_APP_LOG_DATA_DIR, "crashes")
    )
    val SUPPRESSED_ERROR_DIR: File = FileUtils.makeAndGetAppResourceDir(
        FileUtils.createPath(AppUtils.BASE_APP_LOG_DATA_DIR, "suppressed_errors")
    )

    fun getLastCrashLog(ctx: Context): String? {
        val filename = SPLog.getLastCrashLogFilename(ctx) ?: return null
        val file = File(CRASH_ERROR_DIR, filename)
        if (file.length() == 0L) return null

        return file.readText()
    }

    @JvmStatic
    fun saveCrash(ctx: Context, e: Throwable?) {
        if (e == null) return

        e.printStackTrace()

        try {
            val trc = e.stackTraceToString()
            val filename = DateUtils.getDateTimeNow(FILE_NAME_DATE_FORMAT)
            val logFile = File(CRASH_ERROR_DIR, "$filename.txt")

            logFile.createNewFile()
            logFile.writeText(trc)

            // keep last 30 files
            keepLastNFiles(CRASH_ERROR_DIR, 30)

            SPLog.saveLastCrashLogFileName(ctx, logFile.name)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    @JvmStatic
    fun saveError(e: Throwable?, place: String) {
        if (e == null) return

        e.printStackTrace()

        try {
            val trc = e.stackTraceToString()
            val filename = DateUtils.getDateTimeNow(FILE_NAME_DATE_FORMAT)
            val logFile = File(SUPPRESSED_ERROR_DIR, "$filename@${place}.txt")

            logFile.createNewFile()
            logFile.writeText(trc)

            // keep last 30 files
            keepLastNFiles(SUPPRESSED_ERROR_DIR, 30)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun keepLastNFiles(dir: File, n: Int) {
        val files = dir.listFiles()
        if (files != null && files.size > n) {
            val sortedFiles = files.sortedByDescending { it.lastModified() }
            val len = sortedFiles.size - n
            for (i in 0 until len) {
                sortedFiles[i].delete()
            }
        }
    }

    @JvmStatic
    fun d(vararg messages: Any?) {
        if (!BuildConfig.DEBUG) return

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
