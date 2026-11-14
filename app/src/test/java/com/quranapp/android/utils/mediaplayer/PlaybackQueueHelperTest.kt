package com.quranapp.android.utils.mediaplayer

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackQueueHelperTest {
    private val chapterCounts = mapOf(
        1 to 7,
        2 to 286,
        3 to 200,
    )

    private val verseCountProvider: suspend (Int) -> Int = { chapterNo ->
        chapterCounts[chapterNo] ?: 0
    }

    @Test
    fun resolveVerseRange_validSingleAndMultiVerse_returnsRange() = runBlocking {
        assertEquals(
            255..255,
            PlaybackQueueHelper.resolveVerseRange(2, 255, 255, verseCountProvider),
        )
        assertEquals(
            255..257,
            PlaybackQueueHelper.resolveVerseRange(2, 255, 257, verseCountProvider),
        )
    }

    @Test
    fun resolveVerseRange_invalidOrOutOfBounds_returnsEmpty() = runBlocking {
        assertTrue(
            PlaybackQueueHelper.resolveVerseRange(0, 1, 5, verseCountProvider).isEmpty()
        )
        assertTrue(
            PlaybackQueueHelper.resolveVerseRange(2, 10, 5, verseCountProvider).isEmpty()
        )
        assertTrue(
            PlaybackQueueHelper.resolveVerseRange(1, 1, 99, verseCountProvider).isEmpty()
        )
        assertTrue(
            PlaybackQueueHelper.resolveVerseRange(999, 1, 2, verseCountProvider).isEmpty()
        )
    }

    @Test
    fun clampStartVerse_returnsStartIfInRangeElseFirst() {
        assertEquals(256, PlaybackQueueHelper.clampStartVerse(255..257, 256))
        assertEquals(255, PlaybackQueueHelper.clampStartVerse(255..257, 10))
        assertEquals(255, PlaybackQueueHelper.clampStartVerse(255..257, null))
        assertEquals(-1, PlaybackQueueHelper.clampStartVerse(IntRange.EMPTY, 1))
    }
}
