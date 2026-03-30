package com.quranapp.android.utils.mediaplayer

import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.alfaazplus.sunnah.ui.utils.shared_preference.DataStoreManager
import com.quranapp.android.utils.reader.recitation.RecitationUtils

/**
 * Recitation settings for Compose UI, stored in DataStore via [DataStoreManager].
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
    private val KEY_CONTINUE_CHAPTER =
        booleanPreferencesKey(RecitationUtils.KEY_RECITATION_CONTINUE_CHAPTER)
    private val KEY_SCROLL_SYNC = booleanPreferencesKey(RecitationUtils.KEY_RECITATION_SCROLL_SYNC)
    private val KEY_AUDIO_OPTION = intPreferencesKey(RecitationUtils.KEY_RECITATION_AUDIO_OPTION)

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

    suspend fun setRecitationSpeed(speed: Float) {
        DataStoreManager.write(KEY_SPEED, speed)
    }

    @Composable
    fun observeRecitationRepeatVerse(): Boolean {
        return DataStoreManager.observe(KEY_REPEAT, RecitationUtils.RECITATION_DEFAULT_REPEAT)
    }

    suspend fun setRecitationRepeatVerse(repeatVerse: Boolean) {
        DataStoreManager.write(KEY_REPEAT, repeatVerse)
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
}
