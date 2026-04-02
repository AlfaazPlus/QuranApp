package com.quranapp.android.compose.utils.preferences

import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.alfaazplus.sunnah.ui.utils.shared_preference.DataStoreManager
import com.quranapp.android.utils.reader.recitation.RecitationUtils

/**
 * Recitation settings for Compose UI, stored in DataStore via [com.alfaazplus.sunnah.ui.utils.shared_preference.DataStoreManager].
 *
 * Legacy reader, services, and [com.quranapp.android.utils.sharedPrefs.SPReader] keep using
 * SharedPreferences (`sp_reader_recitation_options`); that path is unchanged.
 */
object RecitationPreferences {

    private val KEY_RECITER = stringPreferencesKey(RecitationUtils.KEY_RECITATION_RECITER)
    private val KEY_TRANSLATION_RECITER =
        stringPreferencesKey(RecitationUtils.KEY_RECITATION_TRANSLATION_RECITER)
    private val KEY_SPEED = floatPreferencesKey(RecitationUtils.KEY_RECITATION_SPEED)
    private val KEY_REPEAT = booleanPreferencesKey(RecitationUtils.KEY_RECITATION_REPEAT)
    private val KEY_REPEAT_COUNT =
        intPreferencesKey("key.recitation.repeat_count")
    private val KEY_CONTINUE_CHAPTER =
        booleanPreferencesKey(RecitationUtils.KEY_RECITATION_CONTINUE_CHAPTER)
    private val KEY_SCROLL_SYNC = booleanPreferencesKey(RecitationUtils.KEY_RECITATION_SCROLL_SYNC)
    private val KEY_AUDIO_OPTION = intPreferencesKey(RecitationUtils.KEY_RECITATION_AUDIO_OPTION)
    private val KEY_VERSE_GROUP_SIZE =
        intPreferencesKey("key.recitation.verse_group_size")

    const val RECITATION_DEFAULT_REPEAT_COUNT = 1
    const val RECITATION_DEFAULT_VERSE_GROUP_SIZE = 1

    @Composable
    fun observeReciterId(): String? {
        val raw = DataStoreManager.observe(KEY_RECITER, "")
        return raw.ifEmpty { null }
    }

    suspend fun getReciterId(): String? {
        val raw = DataStoreManager.read(KEY_RECITER, "")
        return raw.ifEmpty { null }
    }

    suspend fun setReciterId(id: String) {
        DataStoreManager.write(KEY_RECITER, id)
    }

    @Composable
    fun observeTranslationReciterId(): String? {
        val raw = DataStoreManager.observe(KEY_TRANSLATION_RECITER, "")
        return raw.ifEmpty { null }
    }

    suspend fun getTranslationReciterId(): String? {
        val raw = DataStoreManager.read(KEY_TRANSLATION_RECITER, "")
        return raw.ifEmpty { null }
    }

    suspend fun setTranslationReciterId(id: String) {
        DataStoreManager.write(KEY_TRANSLATION_RECITER, id)
    }

    @Composable
    fun observeRecitationSpeed(): Float {
        return DataStoreManager.observe(KEY_SPEED, RecitationUtils.RECITATION_DEFAULT_SPEED)
    }

    suspend fun getRecitationSpeed(): Float {
        return DataStoreManager.read(
            KEY_SPEED, RecitationUtils.RECITATION_DEFAULT_SPEED
        ).coerceAtLeast(0.1f)
    }

    suspend fun setRecitationSpeed(speed: Float) {
        DataStoreManager.write(KEY_SPEED, speed)
    }

    @Composable
    fun observeRecitationRepeatCount(): Int {
        return DataStoreManager.observe(
            KEY_REPEAT_COUNT, RECITATION_DEFAULT_REPEAT_COUNT
        ).coerceAtLeast(0)
    }

    suspend fun getRecitationRepeatCount(): Int {
        return DataStoreManager.read(
            KEY_REPEAT_COUNT, RECITATION_DEFAULT_REPEAT_COUNT
        ).coerceAtLeast(0)
    }

    suspend fun setRecitationRepeatCount(repeatCount: Int) {
        DataStoreManager.write(KEY_REPEAT_COUNT, repeatCount.coerceAtLeast(0))
    }

    @Composable
    fun observeRecitationContinueChapter(): Boolean {
        return DataStoreManager.observe(
            KEY_CONTINUE_CHAPTER,
            RecitationUtils.RECITATION_DEFAULT_CONTINUE_CHAPTER
        )
    }

    suspend fun setRecitationContinueChapter(continueChapter: Boolean) {
        DataStoreManager.write(KEY_CONTINUE_CHAPTER, continueChapter)
    }

    @Composable
    fun observeRecitationScrollSync(): Boolean {
        return DataStoreManager.observe(
            KEY_SCROLL_SYNC,
            RecitationUtils.RECITATION_DEFAULT_VERSE_SYNC
        )
    }

    suspend fun setRecitationScrollSync(sync: Boolean) {
        DataStoreManager.write(KEY_SCROLL_SYNC, sync)
    }

    @Composable
    fun observeRecitationAudioOption(): Int {
        return DataStoreManager.observe(KEY_AUDIO_OPTION, RecitationUtils.AUDIO_OPTION_DEFAULT)
    }

    suspend fun getRecitationAudioOption(): Int {
        return DataStoreManager.read(KEY_AUDIO_OPTION, RecitationUtils.AUDIO_OPTION_DEFAULT)
    }

    suspend fun setRecitationAudioOption(option: Int) {
        DataStoreManager.write(KEY_AUDIO_OPTION, option)
    }

    @Composable
    fun observeVerseGroupSize(): Int {
        return DataStoreManager.observe(
            KEY_VERSE_GROUP_SIZE,
            RECITATION_DEFAULT_VERSE_GROUP_SIZE
        )
    }

    suspend fun getVerseGroupSize(): Int {
        return DataStoreManager.read(
            KEY_VERSE_GROUP_SIZE,
            RECITATION_DEFAULT_VERSE_GROUP_SIZE
        ).coerceAtLeast(1)
    }

    suspend fun setVerseGroupSize(size: Int) {
        DataStoreManager.write(KEY_VERSE_GROUP_SIZE, size.coerceAtLeast(1))
    }
}