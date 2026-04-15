@file:OptIn(ExperimentalCoroutinesApi::class)

package com.quranapp.android.compose.utils.preferences

import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.alfaazplus.sunnah.ui.utils.shared_preference.DataStoreManager
import com.alfaazplus.sunnah.ui.utils.shared_preference.PrefKey
import com.quranapp.android.components.reader.ChapterVersePair
import kotlinx.coroutines.ExperimentalCoroutinesApi

object VersePreferences {
    val KEY_VOTD_DATE = PrefKey(longPreferencesKey("votd_timestamp"), -1)
    val KEY_VOTD_CHAPTER_NO = PrefKey(intPreferencesKey("votd_chapter_no"), -1)
    val KEY_VOTD_VERSE_NO = PrefKey(intPreferencesKey("votd_verse_no"), -1)
    val KEY_VOTD_REMINDER_ENABLED = PrefKey(booleanPreferencesKey("votd_reminder_enabled"), false)


    fun getVotd(): ChapterVersePair? {
        val chapterNo = DataStoreManager.read(KEY_VOTD_CHAPTER_NO)
        val verseNo = DataStoreManager.read(KEY_VOTD_VERSE_NO)

        return if (chapterNo != -1 && verseNo != -1) ChapterVersePair(chapterNo, verseNo) else null
    }

    fun getVotdTimestamp(): Long {
        return DataStoreManager.read(KEY_VOTD_DATE)
    }

    suspend fun saveVotd(
        chapterNo: Int,
        verseNo: Int,
        timestamp: Long
    ) {
        DataStoreManager.write(KEY_VOTD_CHAPTER_NO, chapterNo)
        DataStoreManager.write(KEY_VOTD_VERSE_NO, verseNo)
        DataStoreManager.write(KEY_VOTD_DATE, timestamp)
    }

    suspend fun removeVotd() {
        DataStoreManager.removeAll(
            KEY_VOTD_CHAPTER_NO.key,
            KEY_VOTD_VERSE_NO.key,
            KEY_VOTD_DATE.key,
        )
    }

    suspend fun setVOTDReminderEnabled(enabled: Boolean) {
        DataStoreManager.write(KEY_VOTD_REMINDER_ENABLED, enabled)
    }

    fun getVOTDReminderEnabled(): Boolean {
        return DataStoreManager.read(KEY_VOTD_REMINDER_ENABLED)
    }

    @Composable
    fun observeVOTDReminderEnabled(): Boolean {
        return DataStoreManager.observe(KEY_VOTD_REMINDER_ENABLED)
    }
}
