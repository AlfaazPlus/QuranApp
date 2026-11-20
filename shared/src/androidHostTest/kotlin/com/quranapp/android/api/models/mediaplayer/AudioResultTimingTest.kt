package com.quranapp.android.api.models.mediaplayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AudioResultTimingTest {

    @Test
    fun `getNextSegmentBoundaryMs handles inside, gaps, and end of verse`() {
        val vt = VerseTiming(
            verseNo = 1,
            startMs = 1000L,
            endMs = 5000L,
            segments = listOf(
                listOf(1L, 1200L, 2000L),
                listOf(2L, 2500L, 3500L),
                listOf(3L, 3500L, 4600L),
            ),
        )

        assertEquals(1200L, vt.getNextSegmentBoundaryMs(1000L))
        assertEquals(2000L, vt.getNextSegmentBoundaryMs(1200L))
        assertEquals(2000L, vt.getNextSegmentBoundaryMs(1999L))
        assertEquals(2500L, vt.getNextSegmentBoundaryMs(2000L))
        assertEquals(3500L, vt.getNextSegmentBoundaryMs(2800L))
        assertEquals(4600L, vt.getNextSegmentBoundaryMs(3500L))
        assertEquals(5000L, vt.getNextSegmentBoundaryMs(4600L))
        assertNull(vt.getNextSegmentBoundaryMs(5000L))
    }

    @Test
    fun `getNextSegmentBoundaryMs falls back to verse end when segments are absent`() {
        val vt = VerseTiming(
            verseNo = 1,
            startMs = 1000L,
            endMs = 3000L,
            segments = null,
        )

        assertEquals(3000L, vt.getNextSegmentBoundaryMs(1500L))
        assertNull(vt.getNextSegmentBoundaryMs(3000L))
    }

    @Test
    fun `getNextVerseStartMs handles inside verse, gap, and end of chapter`() {
        val timing = ChapterTimingMetadata(
            chapterNo = 1,
            durationMs = 10000L,
            verses = listOf(
                VerseTiming(verseNo = 1, startMs = 500L, endMs = 3000L),
                VerseTiming(verseNo = 2, startMs = 3400L, endMs = 6000L),
                VerseTiming(verseNo = 3, startMs = 6000L, endMs = 9000L),
            ),
        )

        assertEquals(500L, timing.getNextVerseStartMs(0L))
        assertEquals(3400L, timing.getNextVerseStartMs(1000L))
        assertEquals(3400L, timing.getNextVerseStartMs(3000L))
        assertEquals(6000L, timing.getNextVerseStartMs(4000L))
        assertNull(timing.getNextVerseStartMs(6000L))
        assertNull(timing.getNextVerseStartMs(9500L))
    }
}
