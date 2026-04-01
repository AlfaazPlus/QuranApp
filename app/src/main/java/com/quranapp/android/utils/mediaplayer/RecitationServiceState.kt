package com.quranapp.android.utils.mediaplayer

import android.os.Bundle
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.utils.reader.recitation.RecitationUtils

enum class PlayerInterationSource {
    HEADSET,
    USER
}

data class PlayerSettings(
    val speed: Float = 1.0f,
    val repeatCount: Int = 1,
    val continueRange: Boolean = true,
    val audioOption: Int = RecitationUtils.AUDIO_OPTION_DEFAULT,
    val reciter: String? = null,
    val translationReciter: String? = null
)

sealed class PlayerEvent {
    data class Error(val message: String? = null) : PlayerEvent()
    data class Message(val message: String? = null) : PlayerEvent()
}


data class RecitationServiceState(
    val currentVerse: ChapterVersePair = ChapterVersePair(1, 1),
    val isResolving: Boolean = false,
    val pausedByHeadset: Boolean = false,

    val settings: PlayerSettings = PlayerSettings(),
) {
    fun isCurrentVerse(chapterNo: Int, verseNo: Int): Boolean {
        return currentVerse.chapterNo == chapterNo && currentVerse.verseNo == verseNo
    }

    fun getPreviousVerse(meta: QuranMeta): ChapterVersePair? {
        if (!currentVerse.isValid) return null

        val currentChapterNo = currentVerse.chapterNo
        val currentVerseNo = currentVerse.verseNo

        if (
            !QuranMeta.isChapterValid(currentChapterNo) ||
            !meta.isVerseValid4Chapter(
                currentChapterNo,
                currentVerseNo
            )
        ) {
            return null
        }

        var previousChapterNo = currentChapterNo
        var previousVerseNo = currentVerseNo - 1

        if (previousVerseNo < 1) {
            if (QuranMeta.isChapterValid(previousChapterNo - 1)) {
                previousChapterNo--
                previousVerseNo = meta.getChapterVerseCount(previousChapterNo)
            } else {
                previousVerseNo = -1
            }
        }

        if (previousChapterNo == -1 || previousVerseNo == -1) {
            return null
        }

        return ChapterVersePair(previousChapterNo, previousVerseNo)
    }

    fun getNextVerse(meta: QuranMeta): ChapterVersePair? {
        if (!currentVerse.isValid) return null

        val currentChapterNo = currentVerse.chapterNo
        val currentVerseNo = currentVerse.verseNo

        if (
            !QuranMeta.isChapterValid(currentChapterNo) ||
            !meta.isVerseValid4Chapter(
                currentChapterNo,
                currentVerseNo
            )
        ) {
            return null
        }

        var nextChapterNo = currentChapterNo
        var nextVerseNo = currentVerseNo + 1

        if (nextVerseNo > meta.getChapterVerseCount(nextChapterNo)) {
            if (QuranMeta.isChapterValid(nextChapterNo + 1)) {
                nextChapterNo++
                nextVerseNo = 1
            } else {
                nextVerseNo = -1
            }
        }

        if (nextChapterNo == -1 || nextVerseNo == -1) {
            return null
        }

        return ChapterVersePair(nextChapterNo, nextVerseNo)
    }

    fun getPreviousChapter(
        meta: QuranMeta
    ): ChapterVersePair? {
        val currentChapterNo = currentVerse.chapterNo

        if (!QuranMeta.isChapterValid(currentChapterNo)) {
            return null
        }

        var previousChapterNo = currentChapterNo
        var previousVerseNo = -1

        if (QuranMeta.isChapterValid(previousChapterNo - 1)) {
            previousChapterNo--
            previousVerseNo = 1
        }

        if (previousChapterNo == -1 || previousVerseNo == -1) {
            return null
        }

        return ChapterVersePair(previousChapterNo, previousVerseNo)
    }

    fun getNextChapter(
        meta: QuranMeta
    ): ChapterVersePair? {
        val currentChapterNo = currentVerse.chapterNo

        if (!QuranMeta.isChapterValid(currentChapterNo)) {
            return null
        }

        var nextChapterNo = currentChapterNo
        var nextVerseNo = -1

        if (QuranMeta.isChapterValid(nextChapterNo + 1)) {
            nextChapterNo++
            nextVerseNo = 1
        }

        if (nextChapterNo == -1 || nextVerseNo == -1) {
            return null
        }

        return ChapterVersePair(nextChapterNo, nextVerseNo)
    }

    fun toBundle(): Bundle = Bundle().apply {
        putInt(KEY_CURRENT_CHAPTER, currentVerse.chapterNo)
        putInt(KEY_CURRENT_VERSE, currentVerse.verseNo)
        putBoolean(KEY_IS_RESOLVING, isResolving)
        putBoolean(KEY_PAUSED_BY_HEADSET, pausedByHeadset)
        putString(KEY_CURRENT_RECITER, settings.reciter)
        putString(KEY_CURRENT_TRANSLATION_RECITER, settings.translationReciter)
        putFloat(KEY_PLAYBACK_SPEED, settings.speed)
        putInt(KEY_REPEAT_COUNT, settings.repeatCount)
        putBoolean(KEY_CONTINUE, settings.continueRange)
        putInt(KEY_AUDIO_OPTION, settings.audioOption)
    }

    companion object {
        private const val KEY_CURRENT_CHAPTER = "state_current_chapter"
        private const val KEY_CURRENT_VERSE = "state_current_verse"
        private const val KEY_CURRENT_RECITER = "state_current_reciter"
        private const val KEY_CURRENT_TRANSLATION_RECITER = "state_current_translation_reciter"
        private const val KEY_IS_RESOLVING = "state_is_resolving"
        private const val KEY_PAUSED_BY_HEADSET = "state_paused_by_headset"
        private const val KEY_PLAYBACK_SPEED = "state_playback_speed"
        private const val KEY_REPEAT_COUNT = "state_repeat_count"
        private const val KEY_CONTINUE = "state_continue"
        private const val KEY_AUDIO_OPTION = "state_audio_option"
        val EMPTY = RecitationServiceState()

        fun fromBundle(bundle: Bundle): RecitationServiceState {
            return RecitationServiceState(
                currentVerse = ChapterVersePair(
                    chapterNo = bundle.getInt(KEY_CURRENT_CHAPTER, -1),
                    verseNo = bundle.getInt(KEY_CURRENT_VERSE, -1),
                ),
                isResolving = bundle.getBoolean(KEY_IS_RESOLVING, false),
                pausedByHeadset = bundle.getBoolean(KEY_PAUSED_BY_HEADSET, false),
                settings = PlayerSettings(
                    speed = bundle.getFloat(KEY_PLAYBACK_SPEED, 1.0f),
                    repeatCount = bundle.getInt(KEY_REPEAT_COUNT, 1).coerceAtLeast(1),
                    continueRange = bundle.getBoolean(KEY_CONTINUE, true),
                    audioOption = bundle.getInt(
                        KEY_AUDIO_OPTION,
                        RecitationUtils.AUDIO_OPTION_DEFAULT
                    ),
                    reciter = bundle.getString(KEY_CURRENT_RECITER),
                    translationReciter = bundle.getString(KEY_CURRENT_TRANSLATION_RECITER)
                ),
            )
        }
    }
}
