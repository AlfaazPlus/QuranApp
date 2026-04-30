@file:OptIn(ExperimentalCoroutinesApi::class)

package com.quranapp.android.compose.utils.preferences

import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.alfaazplus.sunnah.ui.utils.shared_preference.DataStoreManager
import com.alfaazplus.sunnah.ui.utils.shared_preference.PrefKey
import com.quranapp.android.components.reader.ChapterVersePair
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

object VersePreferences {
    val KEY_VOTD_DATE = PrefKey(longPreferencesKey("votd_timestamp"), -1)
    val KEY_VOTD_CHAPTER_NO = PrefKey(intPreferencesKey("votd_chapter_no"), -1)
    val KEY_VOTD_VERSE_NO = PrefKey(intPreferencesKey("votd_verse_no"), -1)
    val KEY_VOTD_REMINDER_ENABLED = PrefKey(booleanPreferencesKey("votd_reminder_enabled"), false)

    private val KEY_RECOMMENDED_NOTIF_EPOCH_DAY =
        PrefKey(longPreferencesKey("recommended_notif_epoch_day"), -1L)
    private val KEY_RECOMMENDED_NOTIF_SIGNATURE =
        PrefKey(stringPreferencesKey("recommended_notif_signature"), "")

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
        DataStoreManager.edit {
            this[KEY_VOTD_CHAPTER_NO.key] = chapterNo
            this[KEY_VOTD_VERSE_NO.key] = verseNo
            this[KEY_VOTD_DATE.key] = timestamp
        }
    }

    fun votdStorageFlow(): Flow<Triple<Long, Int, Int>> {
        return DataStoreManager.flowMultiple(
            KEY_VOTD_DATE,
            KEY_VOTD_CHAPTER_NO,
            KEY_VOTD_VERSE_NO,
        ).map { result ->
            Triple(
                result.get(KEY_VOTD_DATE),
                result.get(KEY_VOTD_CHAPTER_NO),
                result.get(KEY_VOTD_VERSE_NO),
            )
        }.distinctUntilChanged()
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

    fun getRecommendedNotifDedupeEpochDay(): Long {
        return DataStoreManager.read(KEY_RECOMMENDED_NOTIF_EPOCH_DAY)
    }

    fun getRecommendedNotifDedupeSignature(): String {
        return DataStoreManager.read(KEY_RECOMMENDED_NOTIF_SIGNATURE)
    }

    suspend fun setRecommendedNotifDedupeState(epochDay: Long, signature: String) {
        DataStoreManager.write(KEY_RECOMMENDED_NOTIF_EPOCH_DAY, epochDay)
        DataStoreManager.write(KEY_RECOMMENDED_NOTIF_SIGNATURE, signature)
    }
}
