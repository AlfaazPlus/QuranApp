package com.quranapp.android.api.models.mediaplayer

import android.net.Uri
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ReciterAudioType {
    @SerialName("verse")
    VERSE_BY_VERSE,

    @SerialName("chapter")
    FULL_CHAPTER;

    companion object {
        fun fromString(value: String?): ReciterAudioType {
            return when (value?.lowercase()) {
                "chapter", "full_chapter", "full-chapter" -> FULL_CHAPTER
                else -> VERSE_BY_VERSE
            }
        }
    }
}

/**
 * Timing information for a single verse within a chapter audio file.
 */
@Serializable
data class VerseTiming(
    @SerialName("verse")
    val verseNo: Int,

    @SerialName("start_ms")
    val startMs: Long,

    @SerialName("end_ms")
    val endMs: Long
) {
    /**
     * Check if a given position falls within this verse's time range.
     */
    fun containsPosition(positionMs: Long): Boolean {
        return positionMs >= startMs && positionMs < endMs
    }

    /**
     * Duration of this verse in milliseconds.
     */
    val durationMs: Long get() = endMs - startMs
}

/**
 * Metadata for chapter audio timing, enabling verse synchronization
 * when playing full chapter audio files.
 *
 * JSON format:
 * ```json
 * {
 *   "chapter": 1,
 *   "reciter": "mishary-rashid-alafasy",
 *   "duration_ms": 345000,
 *   "verses": [
 *     {"verse": 1, "start_ms": 0, "end_ms": 8500},
 *     {"verse": 2, "start_ms": 8500, "end_ms": 15200}
 *   ]
 * }
 * ```
 */
@Serializable
data class ChapterTimingMetadata(
    @SerialName("chapter")
    val chapterNo: Int,

    @SerialName("reciter")
    val reciterSlug: String,

    @SerialName("duration_ms")
    val durationMs: Long,

    /**
     * Verse timing information. Null or empty if timing data is not available.
     * When unavailable, playback works but verse sync is disabled.
     */
    @SerialName("verses")
    val verses: List<VerseTiming>? = null
) {
    val hasVerseTiming: Boolean get() = !verses.isNullOrEmpty()

    /**
     * Find the verse that contains the given playback position.
     * Returns null if no timing data or position is outside all verse ranges.
     */
    fun getVerseAtPosition(positionMs: Long): VerseTiming? {
        if (verses.isNullOrEmpty()) return null

        return verses.find { it.containsPosition(positionMs) }
    }

    /**
     * Get timing for a specific verse number.
     * Returns null if verse not found or timing unavailable.
     */
    fun getVerseTiming(verseNo: Int): VerseTiming? {
        return verses?.find { it.verseNo == verseNo }
    }

    /**
     * Check if this metadata has timing info for all verses in range.
     */
    fun hasCompleteTimingFor(fromVerse: Int, toVerse: Int): Boolean {
        if (verses.isNullOrEmpty()) return false

        return (fromVerse..toVerse).all { verseNo ->
            verses.any { it.verseNo == verseNo }
        }
    }

    companion object {
        /**
         * Filename format for cached timing metadata.
         */
        fun getCacheFileName(reciterSlug: String): String {
            return "${reciterSlug}_timing.json"
        }

        /**
         * Create metadata without verse timing (no sync available)
         */
        fun withoutTiming(
            chapterNo: Int,
            reciterSlug: String,
            durationMs: Long
        ): ChapterTimingMetadata {
            return ChapterTimingMetadata(
                chapterNo = chapterNo,
                reciterSlug = reciterSlug,
                durationMs = durationMs,
                verses = null
            )
        }
    }
}

class ChapterAudioResult(
    val audioUri: Uri,
    val chapterNo: Int,
    val reciterSlug: String,
    val timingMetadata: ChapterTimingMetadata?,
) {
    val hasVerseTiming: Boolean
        get() =
            timingMetadata?.hasVerseTiming ?: false

}

