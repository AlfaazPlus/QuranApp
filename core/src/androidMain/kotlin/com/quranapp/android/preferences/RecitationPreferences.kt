package com.quranapp.android.preferences

import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.utils.mediaplayer.AudioEndBehaviour
import com.quranapp.android.utils.mediaplayer.AudioOption

object RecitationPreferences {
    val KEY_RECITER = stringPreferencesKey("key.recitation.reciter")
    val KEY_TRANSLATION_RECITER =
        stringPreferencesKey("key.recitation_translation.reciter")
    val KEY_SPEED = floatPreferencesKey("key.recitation.speed")
    val KEY_REPEAT_COUNT =
        intPreferencesKey("key.recitation.repeat_count")
    val KEY_AUDIO_OPTION = stringPreferencesKey("key.recitation.option_audio")
    val KEY_AUDIO_END_BEHAVIOUR = stringPreferencesKey("key.recitation.end_behaviour")
    val KEY_VERSE_GROUP_SIZE =
        intPreferencesKey("key.recitation.verse_group_size")

    val KEY_LAST_PLAYED_CHAPTER = intPreferencesKey("recitation_last_played_chapter")
    val KEY_LAST_PLAYED_VERSE = intPreferencesKey("recitation_last_played_verse")

    const val RECITATION_MIN_REPEAT_COUNT = 0
    const val RECITATION_DEFAULT_REPEAT_COUNT = 0
    const val RECITATION_DEFAULT_VERSE_GROUP_SIZE = 1

    const val RECITATION_DEFAULT_SPEED: Float = 1.0f


    suspend fun getReciterId(): String? {
        val raw = DataStoreManager.readFirst(KEY_RECITER, "")
        return raw.ifEmpty { null }
    }

    suspend fun setReciterId(id: String) {
        DataStoreManager.write(KEY_RECITER, id)
    }


    suspend fun getTranslationReciterId(): String? {
        val raw = DataStoreManager.readFirst(KEY_TRANSLATION_RECITER, "")
        return raw.ifEmpty { null }
    }

    suspend fun setTranslationReciterId(id: String) {
        DataStoreManager.write(KEY_TRANSLATION_RECITER, id)
    }


    suspend fun getSpeed(): Float {
        return DataStoreManager.readFirst(KEY_SPEED, RECITATION_DEFAULT_SPEED).coerceAtLeast(0.1f)
    }

    suspend fun setSpeed(speed: Float) {
        DataStoreManager.write(KEY_SPEED, speed)
    }


    suspend fun getRepeatCount(): Int {
        return DataStoreManager.readFirst(
            KEY_REPEAT_COUNT, RECITATION_DEFAULT_REPEAT_COUNT
        ).coerceAtLeast(0)
    }

    suspend fun setRepeatCount(repeatCount: Int) {
        DataStoreManager.write(KEY_REPEAT_COUNT, repeatCount.coerceAtLeast(0))
    }


    suspend fun getAudioOption(): AudioOption {
        return AudioOption.fromValue(
            DataStoreManager.readFirst(KEY_AUDIO_OPTION, AudioOption.DEFAULT.value),
        )
    }

    suspend fun setAudioOption(option: AudioOption) {
        DataStoreManager.write(KEY_AUDIO_OPTION, option.value)
    }


    suspend fun getAudioEndBehaviour(): AudioEndBehaviour {
        return AudioEndBehaviour.fromValue(
            DataStoreManager.readFirst(KEY_AUDIO_END_BEHAVIOUR, AudioEndBehaviour.DEFAULT.value),
        )
    }

    suspend fun setAudioEndBehaviour(option: AudioEndBehaviour) {
        DataStoreManager.write(KEY_AUDIO_END_BEHAVIOUR, option.value)
    }


    suspend fun getVerseGroupSize(): Int {
        return DataStoreManager.readFirst(
            KEY_VERSE_GROUP_SIZE,
            RECITATION_DEFAULT_VERSE_GROUP_SIZE
        ).coerceAtLeast(1)
    }

    suspend fun setVerseGroupSize(size: Int) {
        DataStoreManager.write(KEY_VERSE_GROUP_SIZE, size.coerceAtLeast(1))
    }

    suspend fun getLastPlayedVerse(): ChapterVersePair? {
        val chapter = DataStoreManager.readFirst(KEY_LAST_PLAYED_CHAPTER, -1)
        val verse = DataStoreManager.readFirst(KEY_LAST_PLAYED_VERSE, -1)
        if (chapter == -1 || verse == -1) return null
        return ChapterVersePair(chapter, verse)
    }

    suspend fun setLastPlayedVerse(chapterNo: Int, verseNo: Int) {
        DataStoreManager.write(KEY_LAST_PLAYED_CHAPTER, chapterNo)
        DataStoreManager.write(KEY_LAST_PLAYED_VERSE, verseNo)
    }
}
