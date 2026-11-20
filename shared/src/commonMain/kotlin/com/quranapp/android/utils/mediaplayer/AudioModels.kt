package com.quranapp.android.utils.mediaplayer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AudioOption(val value: String) {
    @SerialName("only_quran")
    ONLY_QURAN("only_quran"),

    @SerialName("only_translation")
    ONLY_TRANSLATION("only_translation"),

    @SerialName("both")
    BOTH("both");

    companion object {
        val DEFAULT = ONLY_QURAN

        fun fromValue(value: String): AudioOption {
            return entries.firstOrNull { it.value == value } ?: DEFAULT
        }
    }
}

@Serializable
data class PlayerSettings(
    val speed: Float = 1.0f,
    val repeatCount: Int = 1,
    val audioEndBehaviour: AudioEndBehaviour = AudioEndBehaviour.DEFAULT,
    val audioOption: AudioOption = AudioOption.DEFAULT,
    val reciter: String? = null,
    val translationReciter: String? = null,
)

@Serializable
data class AudioResolutionRequest(
    val chapterNo: Int,
    val settings: PlayerSettings,
)

@Serializable
enum class AudioEndBehaviour(val value: String) {
    @SerialName("stop_playback")
    STOP_PLAYBACK("stop_playback"),

    @SerialName("next_chapter")
    NEXT_CHAPTER("next_chapter"),

    @SerialName("repeat_chapter")
    REPEAT_CHAPTER("repeat_chapter");

    companion object {
        val DEFAULT = STOP_PLAYBACK

        fun fromValue(value: String): AudioEndBehaviour {
            return entries.firstOrNull { it.value == value } ?: DEFAULT
        }
    }
}

@Serializable
enum class RecitationAudioKind(val type: String) {
    @SerialName("quran")
    QURAN("quran"),

    @SerialName("transl")
    TRANSLATION("transl");

    companion object {
        fun fromType(type: String): RecitationAudioKind? =
            entries.firstOrNull { it.type == type }
    }
}
