package com.quranapp.android.utils.mediaplayer

import com.quranapp.android.platform.mediaplayer.AudioClip

/**
 * Virtual timeline over a playlist of clipped [AudioClip]s.
 * Each item is a verse (or verse+translation pair) clipped from a full chapter file.
 * This class maps per-clip positions to a single continuous timeline for UI progress/seek.
 *
 * Verse identity is encoded in each item's [AudioClip.id] with the verse number
 * as its final segment (for example, "kind:2:255").
 */
class VerseClipPlan private constructor(
    val items: List<AudioClip>,
    private val cumulativeStartMs: LongArray,
    private val clipDurationMs: LongArray,
    val virtualDurationMs: Long,
) {
    val isEmpty: Boolean get() = items.isEmpty()

    fun virtualPositionAt(mediaItemIndex: Int, positionInClipMs: Long): Long {
        val i = mediaItemIndex
        if (i !in cumulativeStartMs.indices) return 0L

        val clipDuration = clipDurationMs[i].coerceAtLeast(0L)
        val clampedPosition = positionInClipMs.coerceIn(0L, clipDuration)

        return safeAddNonNegative(cumulativeStartMs[i].coerceAtLeast(0L), clampedPosition)
    }

    /**
     * Finds the target clip index and offset within that clip for a virtual timestamp.
     */
    fun findClipForVirtualPosition(targetMs: Long): Pair<Int, Long> {
        if (isEmpty) return Pair(0, 0L)

        val maxDuration = virtualDurationMs.coerceAtLeast(0L)
        val target = targetMs.coerceIn(0L, maxDuration)

        for (i in cumulativeStartMs.indices) {
            val clipEnd = safeAddNonNegative(
                cumulativeStartMs[i].coerceAtLeast(0L),
                clipDurationMs[i].coerceAtLeast(0L),
            )

            if (target < clipEnd) {
                return Pair(i, (target - cumulativeStartMs[i]).coerceAtLeast(0L))
            }
        }

        val last = cumulativeStartMs.lastIndex
        return Pair(last, clipDurationMs[last])
    }

    /**
     * Returns true if any clip in this plan belongs to [verseNo].
     */
    fun hasVerse(verseNo: Int): Boolean =
        items.any { it.id.endsWith(":$verseNo") }

    /**
     * Returns the playlist index of the first clip whose mediaId contains [verseNo].
     */
    fun firstIndexForVerse(verseNo: Int): Int {
        items.forEachIndexed { index, item ->
            if (item.id.endsWith(":$verseNo")) return index
        }
        return 0
    }

    companion object {
        private fun safeAddNonNegative(a: Long, b: Long): Long {
            if (a < 0L || b < 0L) return 0L
            return if (a > Long.MAX_VALUE - b) Long.MAX_VALUE else a + b
        }

        fun from(items: List<AudioClip>): VerseClipPlan {
            val starts = LongArray(items.size)
            val durs = LongArray(items.size)
            var acc = 0L

            for (i in items.indices) {
                starts[i] = acc
                val len = items[i].clipDurationMs
                durs[i] = len
                acc = safeAddNonNegative(acc, len)
            }

            return VerseClipPlan(items, starts, durs, acc)
        }
    }
}
