package com.quranapp.android.utils.sharedPrefs

import android.content.Context
import androidx.core.content.edit

object SPLastRead {
    private const val SP_NAME = "sp_last_read"
    private const val SP_TIME_NAME = "sp_last_read_time"

    @JvmStatic
    fun setLastRead(context: Context, chapterNo: Int, verseNo: Int) {
        val sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
        val spTime = context.getSharedPreferences(SP_TIME_NAME, Context.MODE_PRIVATE)
        sp.edit {
            putInt(chapterNo.toString(), verseNo)
        }
        spTime.edit {
            putLong(chapterNo.toString(), System.currentTimeMillis())
        }
    }

    @JvmStatic
    fun getLastRead(context: Context, chapterNo: Int): Int {
        val sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
        return sp.getInt(chapterNo.toString(), -1)
    }

    @JvmStatic
    fun removeLastRead(context: Context, chapterNo: Int) {
        val sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
        val spTime = context.getSharedPreferences(SP_TIME_NAME, Context.MODE_PRIVATE)
        sp.edit {
            remove(chapterNo.toString())
        }
        spTime.edit {
            remove(chapterNo.toString())
        }
    }

    @JvmStatic
    fun getAllLastRead(context: Context): List<LastReadEntry> {
        val sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
        val spTime = context.getSharedPreferences(SP_TIME_NAME, Context.MODE_PRIVATE)
        
        val allVerses = sp.all
        val result = mutableListOf<LastReadEntry>()
        
        for ((key, value) in allVerses) {
            val chapterNo = key.toIntOrNull() ?: continue
            val verseNo = value as? Int ?: continue
            val timestamp = spTime.getLong(key, 0L)
            result.add(LastReadEntry(chapterNo, verseNo, timestamp))
        }
        return result
    }

    data class LastReadEntry(val chapterNo: Int, val verseNo: Int, val timestamp: Long)
}
