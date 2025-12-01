package com.quranapp.android.utils.mediaplayer

import android.os.Bundle
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.utils.reader.recitation.RecitationUtils

/**
 * Playback mode for the recitation service.
 */
enum class PlaybackMode {
    VERSE_BY_VERSE,
    FULL_CHAPTER;

    companion object {
        fun fromOrdinal(ordinal: Int): PlaybackMode {
            return entries.getOrElse(ordinal) { VERSE_BY_VERSE }
        }
    }
}

/**
 * Command to start playback. Contains all info needed to play.
 * Send this to the service and it handles everything.
 */
data class PlaybackCommand(
    val chapter: Int,
    val verse: Int,
) {
    fun toBundle(): Bundle = Bundle().apply {
        putInt("chapter", chapter)
        putInt("verse", verse)
    }

    companion object {
        fun fromBundle(bundle: Bundle): PlaybackCommand? {
            val chapter = bundle.getInt("chapter", -1)
            val verse = bundle.getInt("verse", -1)
            if (chapter < 1 || verse < 1) return null

            return PlaybackCommand(
                chapter = chapter,
                verse = verse,
            )
        }
    }
}

/**
 * Current verse being played/highlighted.
 * UI components subscribe to this to highlight verses.
 */
data class CurrentVerse(
    val chapter: Int = -1,
    val verse: Int = -1
) {
    val isValid: Boolean get() = chapter > 0 && verse > 0

    fun toPair(): ChapterVersePair = ChapterVersePair(chapter, verse)

    companion object {
        val NONE = CurrentVerse()
        fun from(pair: ChapterVersePair) = CurrentVerse(pair.chapterNo, pair.verseNo)
    }
}

/**
 * Playback progress for seekbar UI.
 */
data class PlaybackProgress(
    val positionMs: Long = 0L,
    val durationMs: Long = 0L
) {
    val progressPercent: Float
        get() = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
}

/**
 * Player settings that can be changed during playback.
 */
data class PlaybackSettingsU(
    val speed: Float = 1.0f,
    val repeatVerse: Boolean = false,
    val continueRange: Boolean = true,
    val verseSync: Boolean = true,
    val audioOption: Int = RecitationUtils.AUDIO_OPTION_DEFAULT
)

/**
 * Player status - is it playing, loading, etc.
 */
data class PlayerStatus(
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val playbackMode: PlaybackMode = PlaybackMode.VERSE_BY_VERSE,
    val hasVerseTiming: Boolean = false
)

/**
 * Reciter info.
 */
data class ReciterInfo(
    val reciter: String? = null,
    val translationReciter: String? = null
)

/**
 * Player event - errors, messages, etc.
 * Service broadcasts these, UI decides how to display.
 */
sealed class PlayerEvent {
    data class Error(val messageResId: Int, val message: String? = null) : PlayerEvent()
    data class Message(val messageResId: Int, val message: String? = null) : PlayerEvent()
}

/**
 * Complete state - aggregates all sub-states.
 * Used for serialization to/from MediaSession.
 */
data class RecitationServiceState(
    val currentVerse: CurrentVerse = CurrentVerse.NONE,
    val reciterInfo: ReciterInfo = ReciterInfo(),
    val status: PlayerStatus = PlayerStatus(),
    val progress: PlaybackProgress = PlaybackProgress(),
    val settings: PlaybackSettings = PlaybackSettings(),
    // Event with timestamp - controller compares timestamps to detect new events
    val lastEvent: PlayerEvent? = null,
    val lastEventTimestamp: Long = 0L
) {
    // Convenience accessors
    val currentChapter: Int get() = currentVerse.chapter
    val currentVerseNo: Int get() = currentVerse.verse
    val isPlaying: Boolean get() = status.isPlaying
    val isLoading: Boolean get() = status.isLoading

    fun toBundle(): Bundle = Bundle().apply {
        putInt(KEY_CURRENT_CHAPTER, currentVerse.chapter)
        putInt(KEY_CURRENT_VERSE, currentVerse.verse)
        putString(KEY_CURRENT_RECITER, reciterInfo.reciter)
        putString(KEY_CURRENT_TRANSLATION_RECITER, reciterInfo.translationReciter)
        putBoolean(KEY_IS_LOADING, status.isLoading)
        putBoolean(KEY_IS_PLAYING, status.isPlaying)
        putInt(KEY_PLAYBACK_MODE, status.playbackMode.ordinal)
        putBoolean(KEY_HAS_VERSE_TIMING, status.hasVerseTiming)
        putLong(KEY_CURRENT_POSITION, progress.positionMs)
        putLong(KEY_DURATION, progress.durationMs)
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
                putInt(KEY_EVENT_RES_ID, lastEvent.messageResId)
                putString(KEY_EVENT_MESSAGE, lastEvent.message)
            }

            is PlayerEvent.Message -> {
                putString(KEY_EVENT_TYPE, "message")
                putInt(KEY_EVENT_RES_ID, lastEvent.messageResId)
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
        private const val KEY_EVENT_RES_ID = "state_event_res_id"
        private const val KEY_EVENT_MESSAGE = "state_event_message"
        private const val KEY_EVENT_TIMESTAMP = "state_event_timestamp"

        val EMPTY = RecitationServiceState()

        fun fromBundle(bundle: Bundle): RecitationServiceState {
            // Parse event
            val eventType = bundle.getString(KEY_EVENT_TYPE)
            val eventResId = bundle.getInt(KEY_EVENT_RES_ID, 0)
            val eventMessage = bundle.getString(KEY_EVENT_MESSAGE)
            val eventTimestamp = bundle.getLong(KEY_EVENT_TIMESTAMP, 0L)

            val event: PlayerEvent? = when (eventType) {
                "error" -> PlayerEvent.Error(eventResId, eventMessage)
                "message" -> PlayerEvent.Message(eventResId, eventMessage)
                else -> null
            }

            return RecitationServiceState(
                currentVerse = CurrentVerse(
                    chapter = bundle.getInt(KEY_CURRENT_CHAPTER, -1),
                    verse = bundle.getInt(KEY_CURRENT_VERSE, -1)
                ),
                reciterInfo = ReciterInfo(
                    reciter = bundle.getString(KEY_CURRENT_RECITER),
                    translationReciter = bundle.getString(KEY_CURRENT_TRANSLATION_RECITER)
                ),
                status = PlayerStatus(
                    isPlaying = bundle.getBoolean(KEY_IS_PLAYING, false),
                    isLoading = bundle.getBoolean(KEY_IS_LOADING, false),
                    playbackMode = PlaybackMode.fromOrdinal(bundle.getInt(KEY_PLAYBACK_MODE, 0)),
                    hasVerseTiming = bundle.getBoolean(KEY_HAS_VERSE_TIMING, false)
                ),
                progress = PlaybackProgress(
                    positionMs = bundle.getLong(KEY_CURRENT_POSITION, 0L),
                    durationMs = bundle.getLong(KEY_DURATION, 0L)
                ),
                settings = PlaybackSettings(
                    speed = bundle.getFloat(KEY_PLAYBACK_SPEED, 1.0f),
                    repeatVerse = bundle.getBoolean(KEY_REPEAT, false),
                    continueRange = bundle.getBoolean(KEY_CONTINUE, true),
                    verseSync = bundle.getBoolean(KEY_VERSE_SYNC, true),
                    audioOption = bundle.getInt(
                        KEY_AUDIO_OPTION,
                        RecitationUtils.AUDIO_OPTION_DEFAULT
                    )
                ),
                lastEvent = event,
                lastEventTimestamp = eventTimestamp
            )
        }
    }
}
