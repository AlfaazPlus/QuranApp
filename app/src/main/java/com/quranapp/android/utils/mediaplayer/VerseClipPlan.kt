package com.quranapp.android.utils.mediaplayer

import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

/**
 * Virtual timeline over a playlist of clipped [MediaItem]s.
 * Each item is a verse (or verse+translation pair) clipped from a full chapter file.
 * This class maps per-clip positions to a single continuous timeline for UI progress/seek.
 *
 * Verse identity is encoded in each item's [MediaItem.mediaId] as "chapterNo:verseNo".
 */
class VerseClipPlan private constructor(
    val items: List<MediaItem>,
    private val cumulativeStartMs: LongArray,
    private val clipDurationMs: LongArray,
    val virtualDurationMs: Long,
) {
    val isEmpty: Boolean get() = items.isEmpty()

    fun virtualPosition(player: ExoPlayer): Long {
        val i = player.currentMediaItemIndex
        if (i !in cumulativeStartMs.indices) return 0L
        return cumulativeStartMs[i] + player.currentPosition.coerceIn(0L, clipDurationMs[i])
    }

    fun seekToVirtualPosition(player: ExoPlayer, targetMs: Long) {
        if (isEmpty) return
        val target = targetMs.coerceIn(0L, virtualDurationMs)
        for (i in cumulativeStartMs.indices) {
            val clipEnd = cumulativeStartMs[i] + clipDurationMs[i]
            if (target < clipEnd) {
                player.seekTo(i, (target - cumulativeStartMs[i]).coerceAtLeast(0L))
                return
            }
        }
        val last = cumulativeStartMs.lastIndex
        player.seekTo(last, clipDurationMs[last])
    }

    /**
     * Returns the playlist index of the first clip whose mediaId contains [verseNo].
     */
    fun firstIndexForVerse(verseNo: Int): Int {
        items.forEachIndexed { index, item ->
            if (item.mediaId.endsWith(":$verseNo")) return index
        }
        return 0
    }

    companion object {
        fun from(items: List<MediaItem>): VerseClipPlan {
            val starts = LongArray(items.size)
            val durs = LongArray(items.size)
            var acc = 0L
            for (i in items.indices) {
                starts[i] = acc
                val cfg = items[i].clippingConfiguration
                val len = (cfg.endPositionMs - cfg.startPositionMs).coerceAtLeast(0L)
                durs[i] = len
                acc += len
            }
            return VerseClipPlan(items, starts, durs, acc)
        }
    }
}
