package com.quranapp.android.utils.app

import com.quranapp.android.preferences.AppPreferences
import com.quranapp.android.utils.miscellenous.DateUtils
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path

object Logger {
    private const val TAG = "QuranAppLogs"

    private val fileSystem = FileSystem.SYSTEM

    val FILE_NAME_DATE_FORMAT: String = "yyyyMMddHHmmssSSS"
    val CRASH_LOGS_DIR: Path = AppFiles.logDir("crashes")
    val SUPPRESSED_LOGS_DIR: Path = AppFiles.logDir("suppressed_errors")

    fun getLastCrashLog(): String? {
        val filename = AppPreferences.getLastCrashLogFilename() ?: return null
        val path = CRASH_LOGS_DIR / filename

        if (!fileSystem.exists(path)) return null
        if (fileSystem.metadata(path).size == 0L) return null

        return fileSystem.read(path) {
            readUtf8()
        }
    }

    fun saveCrash(error: Throwable?) {
        if (error == null) return

        error.printStackTrace()

        try {
            val filename = DateUtils.format(DateUtils.now(), DateUtils.FILENAME)
            val logFile = CRASH_LOGS_DIR / "$filename.txt"

            fileSystem.createDirectories(CRASH_LOGS_DIR)
            fileSystem.write(logFile) {
                writeUtf8(error.stackTraceToString())
            }

            keepLastNFiles(CRASH_LOGS_DIR, 30)

            runBlocking {
                AppPreferences.setLastCrashLogFileName(filename + ".txt")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveError(error: Throwable?, place: String) {
        if (error == null) return

        error.printStackTrace()

        try {
            val filename = DateUtils.format(DateUtils.now(), DateUtils.FILENAME)
            val logFile = SUPPRESSED_LOGS_DIR / "$filename@$place.txt"

            fileSystem.createDirectories(SUPPRESSED_LOGS_DIR)
            fileSystem.write(logFile) {
                writeUtf8(error.stackTraceToString())
            }

            keepLastNFiles(SUPPRESSED_LOGS_DIR, 30)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun d(vararg messages: Any?) {
        if (!IS_DEBUG) return
        print("$TAG: " + prepareLogMsg(*messages))
    }

    fun print(vararg messages: Any?) {
        if (IS_DEBUG) {
            println("TRACKING: " + prepareLogMsg(*messages))
        }
    }

    private fun keepLastNFiles(dir: Path, n: Int) {
        val files = fileSystem.list(dir)
        if (files.size <= n) return

        val sortedFiles = files.sortedByDescending {
            fileSystem.metadata(it).lastModifiedAtMillis ?: 0L
        }

        sortedFiles.drop(n).forEach {
            fileSystem.delete(it)
        }
    }

    private fun prepareLogMsg(vararg msgs: Any?): String {
        val trace = Thread.currentThread().stackTrace[4]
        val className = trace.className.substringAfterLast('.')
        val header = "($className=>${trace.methodName}:${trace.lineNumber}): "
        return header + msgs.joinToString(", ") { it?.toString() ?: "null" }
    }
}
