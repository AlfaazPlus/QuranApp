package com.quranapp.android.utils.mediaplayer

import android.os.Bundle


sealed class BasePlayerCommand(
    val ACTION: String
) {
    abstract fun toBundle(): Bundle
}

data class PlayCommand(
    val chapterNo: Int,
    val verseNo: Int,
) : BasePlayerCommand(ACTION) {
    override fun toBundle(): Bundle = Bundle().apply {
        putInt("chapterNo", chapterNo)
        putInt("verseNo", verseNo)
    }

    companion object {
        const val ACTION = "PLAY_VERSE"

        fun fromBundle(bundle: Bundle): PlayCommand? {
            val chapterNo = bundle.getInt("chapterNo", -1)
            val verseNo = bundle.getInt("verseNo", -1)
            if (chapterNo < 1 || verseNo < 1) return null

            return PlayCommand(
                chapterNo = chapterNo,
                verseNo = verseNo,
            )
        }
    }
}

data class SetAudioOptionCommand(
    val audioOption: Int
) : BasePlayerCommand(ACTION) {
    override fun toBundle(): Bundle = Bundle().apply {
        putInt("audioOption", audioOption)
    }

    companion object {
        const val ACTION = "SET_AUDIO_OPTION"

        fun fromBundle(bundle: Bundle): SetAudioOptionCommand? {
            val audioOption = bundle.getInt("audioOption", -1)
            if (audioOption < 0) return null

            return SetAudioOptionCommand(audioOption)
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
    val repeat: Boolean
) : BasePlayerCommand(ACTION) {
    override fun toBundle(): Bundle = Bundle().apply {
        putBoolean("repeat", repeat)
    }

    companion object {
        const val ACTION = "SET_REPEAT_VERSE"

        fun fromBundle(bundle: Bundle): SetRepeatCommand? {
            val repeat = bundle.getBoolean("repeat", false)

            return SetRepeatCommand(repeat)
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

data class SetVerseSyncCommand(
    val verseSync: Boolean
) : BasePlayerCommand(ACTION) {
    override fun toBundle(): Bundle = Bundle().apply {
        putBoolean("verseSync", verseSync)
    }

    companion object {
        const val ACTION = "SET_VERSE_SYNC"

        fun fromBundle(bundle: Bundle): SetVerseSyncCommand? {
            val verseSync = bundle.getBoolean("verseSync", false)

            return SetVerseSyncCommand(verseSync)
        }
    }
}

data class SetRecitorCommand(
    val reciter: String
) : BasePlayerCommand(ACTION) {
    override fun toBundle(): Bundle = Bundle().apply {
        putString("reciter", reciter)
    }

    companion object {
        const val ACTION = "SET_RECITOR"

        fun fromBundle(bundle: Bundle): SetRecitorCommand? {
            val reciter = bundle.getString("reciter") ?: return null

            return SetRecitorCommand(reciter)
        }
    }
}

data class SetTranslationRecitorCommand(
    val translationReciter: String
) : BasePlayerCommand(ACTION) {
    override fun toBundle(): Bundle = Bundle().apply {
        putString("translationReciter", translationReciter)
    }

    companion object {
        const val ACTION = "SET_TRANSLATION_RECITOR"

        fun fromBundle(bundle: Bundle): SetTranslationRecitorCommand? {
            val translationReciter = bundle.getString("translationReciter") ?: return null

            return SetTranslationRecitorCommand(translationReciter)
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

object CancelLoadingCommand : BasePlayerCommand("CANCEL_LOADING") {
    override fun toBundle(): Bundle = Bundle()
}

object PreviousVerseCommand : BasePlayerCommand("PREVIOUS_VERSE") {
    override fun toBundle(): Bundle = Bundle()
}

object NextVerseCommand : BasePlayerCommand("NEXT_VERSE") {
    override fun toBundle(): Bundle = Bundle()
}


val ALL_PLAYER_ACTIONS = arrayOf(
    PlayCommand.ACTION,
    SetAudioOptionCommand.ACTION,
    SetPlaybackSpeedCommand.ACTION,
    SetRepeatCommand.ACTION,
    SetContinuePlayingCommand.ACTION,
    SetVerseSyncCommand.ACTION,
    SetRecitorCommand.ACTION,
    SetTranslationRecitorCommand.ACTION,
    SeekToPositionCommand.ACTION,
    StopCommand.ACTION,
    CancelLoadingCommand.ACTION,
    PreviousVerseCommand.ACTION,
    NextVerseCommand.ACTION,
)