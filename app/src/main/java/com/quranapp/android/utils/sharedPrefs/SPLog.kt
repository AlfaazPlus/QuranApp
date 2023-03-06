package com.quranapp.android.utils.sharedPrefs

import android.annotation.SuppressLint
import android.content.Context

object SPLog {
    private const val SP_CRASH_LOG = "sp_crash_log"
    private const val KEY_SP_LAST_CRASH_LOG = "key.last_crash_log"

    private fun sp(ctx: Context) = ctx.getSharedPreferences(SP_CRASH_LOG, Context.MODE_PRIVATE)

    @SuppressLint("ApplySharedPref")
    fun saveLastCrashLog(ctx: Context, stackTraceString: String) {
        sp(ctx).edit().apply {
            putString(KEY_SP_LAST_CRASH_LOG, stackTraceString)
            commit() // We want to save it immediately
        }
    }

    fun getLastCrashLog(ctx: Context): String? = sp(ctx).getString(KEY_SP_LAST_CRASH_LOG, null)

    fun removeLastCrashLog(ctx: Context) {
        sp(ctx).edit().apply {
            remove(KEY_SP_LAST_CRASH_LOG)
            apply()
        }
    }
}
