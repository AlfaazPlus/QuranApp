package com.quranapp.android.utils.mediaplayer

import android.os.Bundle
import com.quranapp.android.api.models.mediaplayer.RecitationAudioKind
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.compose.components.player.dialogs.AudioEndBehaviour
import com.quranapp.android.compose.components.player.dialogs.AudioOption
import com.quranapp.android.compose.utils.preferences.RecitationPreferences.RECITATION_DEFAULT_REPEAT_COUNT
import com.quranapp.android.compose.utils.preferences.RecitationPreferences.RECITATION_MIN_REPEAT_COUNT
import com.quranapp.android.utils.extensions.serializableExtra


sealed class BasePlayerCommand(
    val ACTION: String
) {
    abstract fun toBundle(): Bundle
}

data class StartCommand(
    val verse: ChapterVersePair? = null,
    val mode: RecitationPlaybackMode = RecitationPlaybackMode.Continuous,
) : BasePlayerCommand(ACTION) {
    override fun toBundle(): Bundle = Bundle().apply {
        if (verse != null) {
            putInt(KEY_CHAPTER, verse.chapterNo)
            putInt(KEY_VERSE, verse.verseNo)
        }

        putString(KEY_MODE, mode.scopeType.value)

        when (mode) {
            is RecitationPlaybackMode.Continuous -> Unit
            is RecitationPlaybackMode.Range -> {
                putInt(KEY_RANGE_CHAPTER, mode.chapterNo)
                putInt(KEY_RANGE_FROM_VERSE, mode.fromVerse)
                putInt(KEY_RANGE_TO_VERSE, mode.toVerse)
            }
        }
    }

    companion object {
        const val ACTION = "START"
        private const val KEY_CHAPTER = "chapterNo"
        private const val KEY_VERSE = "verseNo"
        private const val KEY_MODE = "playbackMode"
        private const val KEY_RANGE_CHAPTER = "rangeChapter"
        private const val KEY_RANGE_FROM_VERSE = "rangeFromVerse"
        private const val KEY_RANGE_TO_VERSE = "rangeToVerse"

        fun fromBundle(bundle: Bundle): StartCommand? {
            val chapterNo = bundle.getInt(KEY_CHAPTER, -1)
            val verseNo = bundle.getInt(KEY_VERSE, -1)
            val verse = ChapterVersePair(chapterNo, verseNo).takeIf { it.isValid }

            val mode = when (bundle.getString(KEY_MODE)?.let { PlaybackScopeType.fromValue(it) }) {
                PlaybackScopeType.RANGE -> {
                    val range = RecitationPlaybackMode.Range(
                        chapterNo = bundle.getInt(KEY_RANGE_CHAPTER, -1),
                        fromVerse = bundle.getInt(KEY_RANGE_FROM_VERSE, -1),
                        toVerse = bundle.getInt(KEY_RANGE_TO_VERSE, -1),
                    )
                    if (range.isValid) range else RecitationPlaybackMode.Continuous
                }

                else -> RecitationPlaybackMode.Continuous
            }

            return StartCommand(
                verse = verse,
                mode = mode,
            )
        }
    }
}

data class SetAudioOptionCommand(
    val audioOption: AudioOption
) : BasePlayerCommand(ACTION) {
    override fun toBundle(): Bundle = Bundle().apply {
        putString("audioOption", audioOption.value)
    }

    companion object {
        const val ACTION = "SET_AUDIO_OPTION"

        fun fromBundle(bundle: Bundle): SetAudioOptionCommand? {
            val audioOption = bundle.getString("audioOption")
            if (audioOption == null) return null

            return SetAudioOptionCommand(AudioOption.fromValue(audioOption))
        }
    }
}

data class SetVerseGroupSizeCommand(
    val verseGroupSize: Int
) : BasePlayerCommand(ACTION) {
    override fun toBundle(): Bundle = Bundle().apply {
        putInt("verseGroupSize", verseGroupSize)
    }

    companion object {
        const val ACTION = "SET_VERSE_GROUP_SIZE"

        fun fromBundle(bundle: Bundle): SetVerseGroupSizeCommand? {
            val verseGroupSize = bundle.getInt("verseGroupSize", -1)
            if (verseGroupSize < 1) return null

            return SetVerseGroupSizeCommand(verseGroupSize)
        }
    }
}

data class SetPlaybackSpeedCommand(
    val speed: Float
) : BasePlayerCommand(ACTION) {
    override fun toBundle(): Bundle = Bundle().apply {
        putFloat("speed", speed)
    }

    companion object {
        const val ACTION = "SET_PLAYBACK_SPEED"

        fun fromBundle(bundle: Bundle): SetPlaybackSpeedCommand? {
            val speed = bundle.getFloat("speed", -1f)
            if (speed <= 0f) return null

            return SetPlaybackSpeedCommand(speed)
        }
    }
}

data class SetRepeatCommand(
    val repeatCount: Int
) : BasePlayerCommand(ACTION) {
    override fun toBundle(): Bundle = Bundle().apply {
        putInt("repeatCount", repeatCount)
    }

    companion object {
        const val ACTION = "SET_REPEAT_VERSE"

        fun fromBundle(bundle: Bundle): SetRepeatCommand? {
            val repeatCount =
                bundle.getInt("repeatCount", RECITATION_DEFAULT_REPEAT_COUNT)
            if (repeatCount < RECITATION_MIN_REPEAT_COUNT) return null

            return SetRepeatCommand(repeatCount)
        }
    }
}

data class SetReciterCommand(
    val reciter: String,
    val kind: RecitationAudioKind
) : BasePlayerCommand(ACTION) {
    override fun toBundle(): Bundle = Bundle().apply {
        putString("reciter", reciter)
        putSerializable("kind", kind)
    }

    companion object {
        const val ACTION = "SET_RECITOR"

        fun fromBundle(bundle: Bundle): SetReciterCommand? {
            val reciter = bundle.getString("reciter") ?: return null
            val kind = bundle.serializableExtra<RecitationAudioKind>(
                "kind",
            ) ?: return null

            return SetReciterCommand(reciter, kind)
        }
    }
}

data class SeekToPositionCommand(
    val positionMs: Long
) : BasePlayerCommand(ACTION) {
    override fun toBundle(): Bundle = Bundle().apply {
        putLong("positionMs", positionMs)
    }

    companion object {
        const val ACTION = "SEEK_TO_POSITION"

        fun fromBundle(bundle: Bundle): SeekToPositionCommand? {
            if (!bundle.containsKey("positionMs")) return null
            return SeekToPositionCommand(bundle.getLong("positionMs"))
        }
    }
}

data class SetAudioEndBehaviourCommand(
    val behaviour: AudioEndBehaviour
) : BasePlayerCommand(ACTION) {
    override fun toBundle(): Bundle = Bundle().apply {
        putString("audioEndBehaviour", behaviour.value)
    }

    companion object {
        const val ACTION = "SET_AUDIO_END_BEHAVIOUR"

        fun fromBundle(bundle: Bundle): SetAudioEndBehaviourCommand? {
            val behaviour = bundle.getString("audioEndBehaviour")
            if (behaviour == null) return null

            return SetAudioEndBehaviourCommand(AudioEndBehaviour.fromValue(behaviour))
        }
    }
}

object StopCommand : BasePlayerCommand("STOP") {
    override fun toBundle(): Bundle = Bundle()
}

object PreviousVerseCommand : BasePlayerCommand("PREVIOUS_VERSE") {
    override fun toBundle(): Bundle = Bundle()
}

object NextVerseCommand : BasePlayerCommand("NEXT_VERSE") {
    override fun toBundle(): Bundle = Bundle()
}

object ClearModeCommand : BasePlayerCommand("CLEAR_MODE") {
    override fun toBundle(): Bundle = Bundle()
}


val ALL_PLAYER_ACTIONS = arrayOf(
    StartCommand.ACTION,
    SetAudioOptionCommand.ACTION,
    SetPlaybackSpeedCommand.ACTION,
    SetVerseGroupSizeCommand.ACTION,
    SetRepeatCommand.ACTION,
    SetAudioEndBehaviourCommand.ACTION,
    SetReciterCommand.ACTION,
    SeekToPositionCommand.ACTION,
    StopCommand.ACTION,
    PreviousVerseCommand.ACTION,
    NextVerseCommand.ACTION,
    ClearModeCommand.ACTION,
)
