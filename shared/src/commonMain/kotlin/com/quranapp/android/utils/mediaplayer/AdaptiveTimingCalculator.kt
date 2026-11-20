package com.quranapp.android.utils.mediaplayer

import com.quranapp.android.api.models.mediaplayer.ChapterTimingMetadata
import com.quranapp.android.api.models.mediaplayer.VerseTiming
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Adaptive boundary-driven timing calculation for verse and word synchronization.
 * Calculates the exact sleep duration until the next word/verse boundary instead
 * of busy-polling at fixed 60Hz frame intervals.
 * Essential for preserving battery and preventing UI jank on low-RAM/CPU devices.
 */
object AdaptiveTimingCalculator {
    private const val MIN_DELAY_MS = 1L
    private const val DEFAULT_POLL_WITH_WORDS_MS = 50L
    private const val DEFAULT_POLL_WITHOUT_WORDS_MS = 200L
    private const val MAX_POLL_WITH_WORDS_MS = 250L
    private const val MAX_POLL_WITHOUT_WORDS_MS = 500L

    fun calculateDelay(
        positionMs: Long,
        playbackSpeed: Float,
        activeTiming: VerseTiming?,
        chapterTiming: ChapterTimingMetadata?,
        isClippedPlaylist: Boolean,
        hasWordTiming: Boolean,
    ): Duration {
        val nextBoundaryMs = if (hasWordTiming) {
            activeTiming?.getNextSegmentBoundaryMs(positionMs)
                ?: if (!isClippedPlaylist) chapterTiming?.getNextVerseStartMs(positionMs) else null
        } else if (!isClippedPlaylist) {
            activeTiming?.takeIf { positionMs < it.endMs }?.endMs
                ?: chapterTiming?.getNextVerseStartMs(positionMs)
        } else {
            null
        }

        val speed = playbackSpeed.coerceAtLeast(0.1f)

        val delayMs = if (nextBoundaryMs != null && nextBoundaryMs > positionMs) {
            val remainingRealMs = ((nextBoundaryMs - positionMs) / speed).toLong()
            val maxPoll = if (hasWordTiming) MAX_POLL_WITH_WORDS_MS else MAX_POLL_WITHOUT_WORDS_MS
            remainingRealMs.coerceIn(MIN_DELAY_MS, maxPoll)
        } else {
            if (hasWordTiming) DEFAULT_POLL_WITH_WORDS_MS else DEFAULT_POLL_WITHOUT_WORDS_MS
        }

        return delayMs.milliseconds
    }
}
