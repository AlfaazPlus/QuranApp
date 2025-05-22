package com.quranapp.android.utils.sharedPrefs

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.content.edit

object SPLog {
    private const val SP_CRASH_LOG = "sp_crash_log"
    private const val KEY_SP_LAST_CRASH_LOG = "key.last_crash_log"

    private fun sp(ctx: Context) = ctx.getSharedPreferences(SP_CRASH_LOG, Context.MODE_PRIVATE)

    fun saveLastCrashLogFileName(ctx: Context, filename: String) {
        sp(ctx).edit(commit = true) {
            putString(KEY_SP_LAST_CRASH_LOG, filename)
        }
    }

    fun getLastCrashLogFilename(ctx: Context): String? = sp(ctx).getString(KEY_SP_LAST_CRASH_LOG, null)

    fun removeLastCrashLogFilename(ctx: Context) {
        sp(ctx).edit {
            remove(KEY_SP_LAST_CRASH_LOG)
        }
    }
}
