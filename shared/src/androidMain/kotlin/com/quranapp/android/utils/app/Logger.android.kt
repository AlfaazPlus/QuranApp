package com.quranapp.android.utils.app

import com.quranapp.android.preferences.AppPreferences
import com.quranapp.android.shared.BuildConfig
import com.quranapp.android.utils.files.AppFiles
import com.quranapp.android.utils.miscellenous.DateUtils
import kotlinx.coroutines.runBlocking
import okio.Path
import java.io.File

actual object Logger {
    private const val TAG = "QuranAppLogs"

    actual val FILE_NAME_DATE_FORMAT: String = "yyyyMMddHHmmssSSS"
    actual val CRASH_LOGS_DIR: Path = AppFiles.logDir("crashes")
    actual val SUPPRESSED_LOGS_DIR: Path = AppFiles.logDir("suppressed_errors")

    actual fun getLastCrashLog(): String? {
        val filename = AppPreferences.getLastCrashLogFilename() ?: return null
        val file = (CRASH_LOGS_DIR / filename).toFile()
        if (file.length() == 0L) return null
        return file.readText()
    }

    actual fun saveCrash(error: Throwable?) {
        if (error == null) return

        error.printStackTrace()

        try {
            val filename = DateUtils.getDateTimeNow(FILE_NAME_DATE_FORMAT)
            val logFile = File(CRASH_LOGS_DIR.toFile(), "$filename.txt")
            logFile.createNewFile()
            logFile.writeText(error.stackTraceToString())
            keepLastNFiles(CRASH_LOGS_DIR.toFile(), 30)

            runBlocking {
                AppPreferences.setLastCrashLogFileName(logFile.name)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual fun saveError(error: Throwable?, place: String) {
        if (error == null) return

        error.printStackTrace()

        try {
            val filename = DateUtils.getDateTimeNow(FILE_NAME_DATE_FORMAT)
            val logFile = File(SUPPRESSED_LOGS_DIR.toFile(), "$filename@$place.txt")
            logFile.createNewFile()
            logFile.writeText(error.stackTraceToString())
            keepLastNFiles(SUPPRESSED_LOGS_DIR.toFile(), 30)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual fun d(vararg messages: Any?) {
        if (!BuildConfig.DEBUG) return
        android.util.Log.d(TAG, prepareLogMsg(*messages))
    }

    actual fun print(vararg messages: Any?) {
        if (BuildConfig.DEBUG) {
            println("TRACKING: " + prepareLogMsg(*messages))
        }
    }

    private fun keepLastNFiles(dir: File, n: Int) {
        val files = dir.listFiles() ?: return
        if (files.size <= n) return

        val sortedFiles = files.sortedByDescending { it.lastModified() }
        val extra = sortedFiles.size - n
        for (index in 0 until extra) {
            sortedFiles[index].delete()
        }
    }

    private fun prepareLogMsg(vararg msgs: Any?): String {
        val trace = Thread.currentThread().stackTrace[4]
        val className = trace.className.substringAfterLast('.')
        val header = "($className=>${trace.methodName}:${trace.lineNumber}): "
        return header + msgs.joinToString(", ") { it?.toString() ?: "null" }
    }
}
