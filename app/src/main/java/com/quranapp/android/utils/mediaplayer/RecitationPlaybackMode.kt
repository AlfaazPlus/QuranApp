package com.quranapp.android.utils.mediaplayer

enum class PlaybackScopeType(val value: String) {
    CONTINUOUS("continuous"),
    RANGE("range");

    companion object {
        fun fromValue(value: String): PlaybackScopeType =
            entries.firstOrNull { it.value == value } ?: CONTINUOUS
    }
}

sealed class RecitationPlaybackMode {
    val scopeType: PlaybackScopeType
        get() = when (this) {
            is Continuous -> PlaybackScopeType.CONTINUOUS
            is Range -> PlaybackScopeType.RANGE
        }

    data object Continuous : RecitationPlaybackMode()

    data class Range(
        val chapterNo: Int,
        val fromVerse: Int,
        val toVerse: Int,
    ) : RecitationPlaybackMode() {
        val isValid: Boolean get() = chapterNo > 0 && fromVerse > 0 && toVerse >= fromVerse
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
