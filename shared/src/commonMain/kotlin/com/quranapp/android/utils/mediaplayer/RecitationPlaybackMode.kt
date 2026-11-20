package com.quranapp.android.utils.mediaplayer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PlaybackScopeType(val value: String) {
    @SerialName("continuous")
    CONTINUOUS("continuous"),

    @SerialName("range")
    RANGE("range");

    companion object {
        fun fromValue(value: String): PlaybackScopeType =
            entries.firstOrNull { it.value == value } ?: CONTINUOUS
    }
}

@Serializable
sealed class RecitationPlaybackMode {
    val scopeType: PlaybackScopeType
        get() = when (this) {
            is Continuous -> PlaybackScopeType.CONTINUOUS
            is Range -> PlaybackScopeType.RANGE
        }

    @Serializable
    data object Continuous : RecitationPlaybackMode()

    @Serializable
    data class Range(
        val chapterNo: Int,
        val fromVerse: Int,
        val toVerse: Int,
    ) : RecitationPlaybackMode() {
        val isValid: Boolean
            get() = chapterNo > 0 &&
                    fromVerse > 0 &&
                    toVerse >= fromVerse
    }
}

internal object PlaybackQueueHelper {
    suspend fun resolveVerseRange(
        chapterNo: Int,
        fromVerse: Int,
        toVerse: Int,
        chapterVerseCount: suspend (Int) -> Int,
    ): IntRange {
        if (chapterNo <= 0 || fromVerse <= 0 || toVerse < fromVerse) return IntRange.EMPTY

        val count = chapterVerseCount(chapterNo)
        if (count <= 0 || fromVerse > count || toVerse > count) return IntRange.EMPTY

        return fromVerse..toVerse
    }

    fun clampStartVerse(range: IntRange, startVerse: Int?): Int {
        if (range.isEmpty()) return -1
        return if (startVerse != null && startVerse in range) startVerse else range.first
    }
}
