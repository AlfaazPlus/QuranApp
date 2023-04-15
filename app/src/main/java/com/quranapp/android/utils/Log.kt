package com.quranapp.android.utils

import com.quranapp.android.BuildConfig
import com.quranapp.android.utils.app.AppUtils
import com.quranapp.android.utils.univ.DateUtils
import com.quranapp.android.utils.univ.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object Log {
    private const val TAG = "QuranAppLogs"
    const val FILE_NAME_DATE_FORMAT = "yyyyMMddhhmmssSSS"
    val SUPPRESSED_ERROR_DIR: File = FileUtils.makeAndGetAppResourceDir(
        FileUtils.createPath(AppUtils.BASE_APP_LOG_DATA_DIR, "suppressed_errors")
    )

    fun saveError(e: Throwable?, place: String) {
        if (e == null) return
        
        e.printStackTrace()

        try {
            suspend {
                withContext(Dispatchers.IO) {
                    val trc = e.stackTraceToString()
                    val filename = DateUtils.getDateTimeNow(FILE_NAME_DATE_FORMAT)
                    val logFile = File(SUPPRESSED_ERROR_DIR, "$filename@${place}.txt")

                    logFile.createNewFile()
                    logFile.writeText(trc)

                    // keep last 30 files
                    val files = SUPPRESSED_ERROR_DIR.listFiles()
                    if (files != null && files.size > 30) {
                        val sortedFiles = files.sortedBy { it.lastModified() }
                        val len = sortedFiles.size - 30
                        for (i in 0 until len) {
                            sortedFiles[i].delete()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
