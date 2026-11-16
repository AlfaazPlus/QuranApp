package com.quranapp.android.utils.mediaplayer

enum class AudioOption(val value: String) {
    ONLY_QURAN("only_quran"),
    ONLY_TRANSLATION("only_translation"),
    BOTH("both");

    companion object {
        val DEFAULT = ONLY_QURAN

        fun fromValue(value: String): AudioOption {
            return entries.firstOrNull { it.value == value } ?: ONLY_QURAN
        }
    }
}

enum class AudioEndBehaviour(val value: String) {
    STOP_PLAYBACK("stop_playback"),
    NEXT_CHAPTER("next_chapter"),
    REPEAT_CHAPTER("repeat_chapter");

    companion object {
        val DEFAULT = STOP_PLAYBACK

        fun fromValue(value: String): AudioEndBehaviour {
            return entries.firstOrNull { it.value == value } ?: STOP_PLAYBACK
        }
    }
}
