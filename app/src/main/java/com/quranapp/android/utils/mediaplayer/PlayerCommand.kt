package com.quranapp.android.utils.mediaplayer

import android.os.Bundle
import com.quranapp.android.api.models.mediaplayer.RecitationAudioKind
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.compose.components.player.dialogs.AudioOption


sealed class BasePlayerCommand(
    val ACTION: String
) {
    abstract fun toBundle(): Bundle
}

data class StartCommand(
    val verse: ChapterVersePair?
) : BasePlayerCommand(ACTION) {
    override fun toBundle(): Bundle = Bundle().apply {
        if (verse != null) {
            putSerializable("verse", verse)
        }
    }

    companion object {
        const val ACTION = "START"

        fun fromBundle(bundle: Bundle): StartCommand? {
            val verse = bundle.getSerializable("verse", ChapterVersePair::class.java)
            return StartCommand(verse)
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
            val repeatCount = bundle.getInt("repeatCount", 1)
            if (repeatCount < 1) return null

            return SetRepeatCommand(repeatCount)
        }
    }
}

data class SetContinuePlayingCommand(
    val continuePlaying: Boolean
) : BasePlayerCommand(ACTION) {
    override fun toBundle(): Bundle = Bundle().apply {
        putBoolean("continuePlaying", continuePlaying)
    }

    companion object {
        const val ACTION = "SET_CONTINUE_PLAYING"

        fun fromBundle(bundle: Bundle): SetContinuePlayingCommand? {
            val continuePlaying = bundle.getBoolean("continuePlaying", false)

            return SetContinuePlayingCommand(continuePlaying)
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
            val kind = bundle.getSerializable(
                "kind",
                RecitationAudioKind::class.java,
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
            val positionMs = bundle.getLong("positionMs", -1L)
            if (positionMs < 0L) return null

            return SeekToPositionCommand(positionMs)
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


val ALL_PLAYER_ACTIONS = arrayOf(
    StartCommand.ACTION,
    SetAudioOptionCommand.ACTION,
    SetPlaybackSpeedCommand.ACTION,
    SetVerseGroupSizeCommand.ACTION,
    SetRepeatCommand.ACTION,
    SetContinuePlayingCommand.ACTION,
    SetReciterCommand.ACTION,
    SeekToPositionCommand.ACTION,
    StopCommand.ACTION,
    PreviousVerseCommand.ACTION,
    NextVerseCommand.ACTION,
)