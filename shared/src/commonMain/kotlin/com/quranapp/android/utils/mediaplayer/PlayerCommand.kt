package com.quranapp.android.utils.mediaplayer


import com.quranapp.android.models.ChapterVersePair
import com.quranapp.android.preferences.RecitationPreferences.RECITATION_MIN_REPEAT_COUNT
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
sealed class BasePlayerCommand

@Serializable
@SerialName("START")
data class StartCommand(
    val verse: ChapterVersePair? = null,
    val mode: RecitationPlaybackMode = RecitationPlaybackMode.Continuous,
) : BasePlayerCommand() {
    companion object {
        const val ACTION = "START"
    }
}


@Serializable
@SerialName("SET_AUDIO_OPTION")
data class SetAudioOptionCommand(
    val audioOption: AudioOption,
) : BasePlayerCommand() {
    companion object {
        const val ACTION = "SET_AUDIO_OPTION"
    }
}

@Serializable
@SerialName("SET_VERSE_GROUP_SIZE")
data class SetVerseGroupSizeCommand(
    val verseGroupSize: Int,
) : BasePlayerCommand() {
    companion object {
        const val ACTION = "SET_VERSE_GROUP_SIZE"
    }
}

@Serializable
@SerialName("SET_PLAYBACK_SPEED")
data class SetPlaybackSpeedCommand(
    val speed: Float,
) : BasePlayerCommand() {
    companion object {
        const val ACTION = "SET_PLAYBACK_SPEED"
    }
}

@Serializable
@SerialName("SET_REPEAT_VERSE")
data class SetRepeatCommand(
    val repeatCount: Int,
) : BasePlayerCommand() {
    companion object {
        const val ACTION = "SET_REPEAT_VERSE"
    }
}

@Serializable
@SerialName("SET_RECITOR")
data class SetReciterCommand(
    val reciter: String,
    val kind: RecitationAudioKind,
) : BasePlayerCommand() {
    companion object {
        const val ACTION = "SET_RECITOR"
    }
}

@Serializable
@SerialName("SEEK_TO_POSITION")
data class SeekToPositionCommand(
    val positionMs: Long,
) : BasePlayerCommand() {
    companion object {
        const val ACTION = "SEEK_TO_POSITION"
    }
}

@Serializable
@SerialName("SET_AUDIO_END_BEHAVIOUR")
data class SetAudioEndBehaviourCommand(
    val behaviour: AudioEndBehaviour,
) : BasePlayerCommand() {
    companion object {
        const val ACTION = "SET_AUDIO_END_BEHAVIOUR"
    }
}

@Serializable
@SerialName("STOP")
object StopCommand : BasePlayerCommand() {
    const val ACTION = "STOP"
}

@Serializable
@SerialName("PREVIOUS_VERSE")
object PreviousVerseCommand : BasePlayerCommand() {
    const val ACTION = "PREVIOUS_VERSE"
}

@Serializable
@SerialName("NEXT_VERSE")
object NextVerseCommand : BasePlayerCommand() {
    const val ACTION = "NEXT_VERSE"
}

@Serializable
@SerialName("CLEAR_MODE")
object ClearModeCommand : BasePlayerCommand() {
    const val ACTION = "CLEAR_MODE"
}

private val playerCommandJson = Json {
    classDiscriminator = "type"
    ignoreUnknownKeys = true
    encodeDefaults = true
}

fun BasePlayerCommand.encode(): String =
    playerCommandJson.encodeToString(this)

fun decodePlayerCommand(payload: String): BasePlayerCommand? =
    runCatching {
        playerCommandJson.decodeFromString<BasePlayerCommand>(payload)
    }
        .getOrNull()
        ?.validated()

fun BasePlayerCommand.validated(): BasePlayerCommand? =
    when (this) {
        is StartCommand -> {
            copy(
                verse = verse?.takeIf { it.isValid },
                mode = when (val currentMode = mode) {
                    is RecitationPlaybackMode.Range ->
                        currentMode.takeIf { it.isValid }
                            ?: RecitationPlaybackMode.Continuous

                    RecitationPlaybackMode.Continuous ->
                        RecitationPlaybackMode.Continuous
                }
            )
        }

        is SetVerseGroupSizeCommand ->
            takeIf { verseGroupSize >= 1 }

        is SetPlaybackSpeedCommand ->
            takeIf { speed.isFinite() && speed > 0f }

        is SetRepeatCommand ->
            takeIf { repeatCount >= RECITATION_MIN_REPEAT_COUNT }

        is SetReciterCommand ->
            takeIf { reciter.isNotBlank() }

        is SeekToPositionCommand ->
            takeIf { positionMs >= 0L }

        else -> this
    }
