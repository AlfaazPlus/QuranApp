package com.quranapp.android.compose.utils.preferences

import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.alfaazplus.sunnah.ui.utils.shared_preference.DataStoreManager
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.compose.components.player.dialogs.AudioEndBehaviour
import com.quranapp.android.compose.components.player.dialogs.AudioOption

object RecitationPreferences {
    private val KEY_RECITER = stringPreferencesKey("key.recitation.reciter")
    private val KEY_TRANSLATION_RECITER =
        stringPreferencesKey("key.recitation_translation.reciter")
    private val KEY_SPEED = floatPreferencesKey("key.recitation.speed")
    private val KEY_REPEAT = booleanPreferencesKey("key.recitation.repeat")
    private val KEY_REPEAT_COUNT =
        intPreferencesKey("key.recitation.repeat_count")
    private val KEY_AUDIO_OPTION = stringPreferencesKey("key.recitation.option_audio")
    private val KEY_AUDIO_END_BEHAVIOUR = stringPreferencesKey("key.recitation.end_behaviour")
    private val KEY_VERSE_GROUP_SIZE =
        intPreferencesKey("key.recitation.verse_group_size")

    private val KEY_LAST_PLAYED_CHAPTER = intPreferencesKey("recitation_last_played_chapter")
    private val KEY_LAST_PLAYED_VERSE = intPreferencesKey("recitation_last_played_verse")

    const val RECITATION_MIN_REPEAT_COUNT = 0
    const val RECITATION_DEFAULT_REPEAT_COUNT = 0
    const val RECITATION_DEFAULT_VERSE_GROUP_SIZE = 1

    const val RECITATION_DEFAULT_SPEED: Float = 1.0f

    @Composable
    fun observeReciterId(): String? {
        val raw = DataStoreManager.observe(KEY_RECITER, "")
        return raw.ifEmpty { null }
    }

    fun getReciterId(): String? {
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

    fun getTranslationReciterId(): String? {
        val raw = DataStoreManager.read(KEY_TRANSLATION_RECITER, "")
        return raw.ifEmpty { null }
    }

    suspend fun setTranslationReciterId(id: String) {
        DataStoreManager.write(KEY_TRANSLATION_RECITER, id)
    }

    @Composable
    fun observeSpeed(): Float {
        return DataStoreManager.observe(KEY_SPEED, RECITATION_DEFAULT_SPEED)
    }

    fun getSpeed(): Float {
        return DataStoreManager.read(
            KEY_SPEED, RECITATION_DEFAULT_SPEED
        ).coerceAtLeast(0.1f)
    }

    suspend fun setSpeed(speed: Float) {
        DataStoreManager.write(KEY_SPEED, speed)
    }

    @Composable
    fun observeRepeatCount(): Int {
        return DataStoreManager.observe(
            KEY_REPEAT_COUNT, RECITATION_DEFAULT_REPEAT_COUNT
        ).coerceAtLeast(0)
    }

    suspend fun getRepeatCount(): Int {
        return DataStoreManager.read(
            KEY_REPEAT_COUNT, RECITATION_DEFAULT_REPEAT_COUNT
        ).coerceAtLeast(0)
    }

    suspend fun setRepeatCount(repeatCount: Int) {
        DataStoreManager.write(KEY_REPEAT_COUNT, repeatCount.coerceAtLeast(0))
    }

    @Composable
    fun observeAudioOption(): AudioOption {
        return DataStoreManager.observe(KEY_AUDIO_OPTION, AudioOption.DEFAULT.value).let {
            AudioOption.fromValue(it)
        }
    }

    fun getAudioOption(): AudioOption {
        return DataStoreManager.read(KEY_AUDIO_OPTION, AudioOption.DEFAULT.value).let {
            AudioOption.fromValue(it)
        }
    }

    suspend fun setAudioOption(option: AudioOption) {
        DataStoreManager.write(KEY_AUDIO_OPTION, option.value)
    }

    @Composable
    fun observeAudioEndBehaviour(): AudioEndBehaviour {
        return DataStoreManager.observe(KEY_AUDIO_END_BEHAVIOUR, AudioEndBehaviour.DEFAULT.value)
            .let {
                AudioEndBehaviour.fromValue(it)
            }
    }

    fun getAudioEndBehaviour(): AudioEndBehaviour {
        return DataStoreManager.read(KEY_AUDIO_END_BEHAVIOUR, AudioEndBehaviour.DEFAULT.value).let {
            AudioEndBehaviour.fromValue(it)
        }
    }

    suspend fun setAudioEndBehaviour(option: AudioEndBehaviour) {
        DataStoreManager.write(KEY_AUDIO_END_BEHAVIOUR, option.value)
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

    fun getLastPlayedVerse(): ChapterVersePair? {
        val chapter = DataStoreManager.read(KEY_LAST_PLAYED_CHAPTER, -1)
        val verse = DataStoreManager.read(KEY_LAST_PLAYED_VERSE, -1)

        if (chapter == -1 || verse == -1) return null

        return ChapterVersePair(chapter, verse)
    }

    suspend fun setLastPlayedVerse(chapterNo: Int, verseNo: Int) {
        DataStoreManager.write(KEY_LAST_PLAYED_CHAPTER, chapterNo)
        DataStoreManager.write(KEY_LAST_PLAYED_VERSE, verseNo)
    }
}