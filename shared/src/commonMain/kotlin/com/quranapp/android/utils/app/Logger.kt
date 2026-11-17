package com.quranapp.android.utils.app

import okio.Path

expect object Logger {
    val FILE_NAME_DATE_FORMAT: String
    val CRASH_LOGS_DIR: Path
    val SUPPRESSED_LOGS_DIR: Path

    fun getLastCrashLog(): String?
    fun saveCrash(error: Throwable?)
    fun saveError(error: Throwable?, place: String)
    fun d(vararg messages: Any?)
    fun print(vararg messages: Any?)
}
