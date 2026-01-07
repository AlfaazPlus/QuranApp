package com.quranapp.android.utils.mediaplayer

import android.os.Bundle
import com.quranapp.android.api.models.mediaplayer.ChapterTimingMetadata
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.utils.reader.recitation.RecitationUtils

enum class PlaybackMode {
    VERSE_BY_VERSE,
    FULL_CHAPTER;

    companion object {
        fun fromOrdinal(ordinal: Int): PlaybackMode {
            return entries.getOrElse(ordinal) { VERSE_BY_VERSE }
        }
    }
}

enum class PlayerInterationSource {
    HEADSET,
    USER
}

data class PlayerSettings(
    val speed: Float = 1.0f,
    val repeatVerse: Boolean = false,
    val continueRange: Boolean = true,
    val verseSync: Boolean = true,
    val audioOption: Int = RecitationUtils.AUDIO_OPTION_DEFAULT,
    val reciter: String? = null,
    val translationReciter: String? = null
)

sealed class PlayerEvent {
    data class Error(val message: String? = null) : PlayerEvent()
    data class Message(val message: String? = null) : PlayerEvent()
}


data class RecitationServiceState(
    val currentVerse: ChapterVersePair = ChapterVersePair.NONE,
    val isLoading: Boolean = false,
    val isPlaying: Boolean = false,
    val pausedByHeadset: Boolean = false,
    val playbackMode: PlaybackMode = PlaybackMode.VERSE_BY_VERSE,
    var timingMetadata: ChapterTimingMetadata? = null,

    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val settings: PlayerSettings = PlayerSettings(),
    // Event with timestamp - controller compares timestamps to detect new events
    val lastEvent: PlayerEvent? = null,
    val lastEventTimestamp: Long = 0L
) {
    val progressPercent: Float
        get() = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f


    fun isCurrentVerse(chapterNo: Int, verseNo: Int): Boolean {
        return currentVerse.chapterNo == chapterNo && currentVerse.verseNo == verseNo
    }

    fun getPreviousVerse(meta: QuranMeta): ChapterVersePair? {
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
            // If we are at the first verse of the chapter, go to the last verse of the previous chapter if possible.
            // Otherwise, change nothing.
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

    fun getNextVerse(
        meta: QuranMeta
    ): ChapterVersePair? {
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
            // If we are at the last verse of the chapter, go to the first verse of the next chapter if possible.
            // Otherwise, change nothing.
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
        putBoolean(KEY_IS_LOADING, isLoading)
        putBoolean(KEY_IS_PLAYING, isPlaying)
        putBoolean(KEY_PAUSED_BY_HEADSET, pausedByHeadset)
        putInt(KEY_PLAYBACK_MODE, playbackMode.ordinal)
        putBoolean(KEY_HAS_VERSE_TIMING, timingMetadata != null)
        putLong(KEY_CURRENT_POSITION, positionMs)
        putLong(KEY_DURATION, durationMs)
        putString(KEY_CURRENT_RECITER, settings.reciter)
        putString(KEY_CURRENT_TRANSLATION_RECITER, settings.translationReciter)
        putFloat(KEY_PLAYBACK_SPEED, settings.speed)
        putBoolean(KEY_REPEAT, settings.repeatVerse)
        putBoolean(KEY_CONTINUE, settings.continueRange)
        putBoolean(KEY_VERSE_SYNC, settings.verseSync)
        putInt(KEY_AUDIO_OPTION, settings.audioOption)
        // Event data
        putLong(KEY_EVENT_TIMESTAMP, lastEventTimestamp)
        when (lastEvent) {
            is PlayerEvent.Error -> {
                putString(KEY_EVENT_TYPE, "error")
                putString(KEY_EVENT_MESSAGE, lastEvent.message)
            }

            is PlayerEvent.Message -> {
                putString(KEY_EVENT_TYPE, "message")
                putString(KEY_EVENT_MESSAGE, lastEvent.message)
            }

            null -> putString(KEY_EVENT_TYPE, null)
        }
    }

    companion object {
        private const val KEY_CURRENT_CHAPTER = "state_current_chapter"
        private const val KEY_CURRENT_VERSE = "state_current_verse"
        private const val KEY_CURRENT_RECITER = "state_current_reciter"
        private const val KEY_CURRENT_TRANSLATION_RECITER = "state_current_translation_reciter"
        private const val KEY_IS_LOADING = "state_is_loading"
        private const val KEY_IS_PLAYING = "state_is_playing"
        private const val KEY_PAUSED_BY_HEADSET = "state_paused_by_headset"
        private const val KEY_PLAYBACK_MODE = "state_playback_mode"
        private const val KEY_HAS_VERSE_TIMING = "state_has_verse_timing"
        private const val KEY_CURRENT_POSITION = "state_current_position"
        private const val KEY_DURATION = "state_duration"
        private const val KEY_PLAYBACK_SPEED = "state_playback_speed"
        private const val KEY_REPEAT = "state_repeat"
        private const val KEY_CONTINUE = "state_continue"
        private const val KEY_VERSE_SYNC = "state_verse_sync"
        private const val KEY_AUDIO_OPTION = "state_audio_option"
        private const val KEY_EVENT_TYPE = "state_event_type"
        private const val KEY_EVENT_MESSAGE = "state_event_message"
        private const val KEY_EVENT_TIMESTAMP = "state_event_timestamp"

        val EMPTY = RecitationServiceState()

        fun fromBundle(bundle: Bundle): RecitationServiceState {
            // Parse event
            val eventType = bundle.getString(KEY_EVENT_TYPE)
            val eventMessage = bundle.getString(KEY_EVENT_MESSAGE)
            val eventTimestamp = bundle.getLong(KEY_EVENT_TIMESTAMP, 0L)

            val event: PlayerEvent? = when (eventType) {
                "error" -> PlayerEvent.Error(eventMessage)
                "message" -> PlayerEvent.Message(eventMessage)
                else -> null
            }

            return RecitationServiceState(
                currentVerse = ChapterVersePair(
                    chapterNo = bundle.getInt(KEY_CURRENT_CHAPTER, -1),
                    verseNo = bundle.getInt(KEY_CURRENT_VERSE, -1)
                ),
                isLoading = bundle.getBoolean(KEY_IS_LOADING, false),
                isPlaying = bundle.getBoolean(KEY_IS_PLAYING, false),
                pausedByHeadset = bundle.getBoolean(KEY_PAUSED_BY_HEADSET, false),
                playbackMode = PlaybackMode.fromOrdinal(bundle.getInt(KEY_PLAYBACK_MODE, 0)),
                positionMs = bundle.getLong(KEY_CURRENT_POSITION, 0L),
                durationMs = bundle.getLong(KEY_DURATION, 0L),
                settings = PlayerSettings(
                    speed = bundle.getFloat(KEY_PLAYBACK_SPEED, 1.0f),
                    repeatVerse = bundle.getBoolean(KEY_REPEAT, false),
                    continueRange = bundle.getBoolean(KEY_CONTINUE, true),
                    verseSync = bundle.getBoolean(KEY_VERSE_SYNC, true),
                    audioOption = bundle.getInt(
                        KEY_AUDIO_OPTION,
                        RecitationUtils.AUDIO_OPTION_DEFAULT
                    ),
                    reciter = bundle.getString(KEY_CURRENT_RECITER),
                    translationReciter = bundle.getString(KEY_CURRENT_TRANSLATION_RECITER)
                ),
                lastEvent = event,
                lastEventTimestamp = eventTimestamp
            )
        }
    }
}
