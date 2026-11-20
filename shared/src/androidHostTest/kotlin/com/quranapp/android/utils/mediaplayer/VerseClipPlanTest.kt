package com.quranapp.android.utils.mediaplayer

import com.quranapp.android.platform.mediaplayer.AudioClip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VerseClipPlanTest {

    @Test
    fun `virtual timeline calculates cumulative positions and durations accurately`() {
        val clips = listOf(
            AudioClip(id = "quran:1:1", uri = "file:///ch1.mp3", startMs = 0L, endMs = 3000L),
            AudioClip(id = "transl:1:1", uri = "file:///tr1.mp3", startMs = 1000L, endMs = 5000L),
            AudioClip(id = "quran:1:2", uri = "file:///ch1.mp3", startMs = 3000L, endMs = 7000L),
        )

        val plan = VerseClipPlan.from(clips)

        assertFalse(plan.isEmpty)
        // Clip 0 duration: 3000ms (0..3000)
        // Clip 1 duration: 4000ms (3000..7000)
        // Clip 2 duration: 4000ms (7000..11000)
        // Total virtual duration: 11000ms
        assertEquals(11000L, plan.virtualDurationMs)

        // Position within clip 0
        assertEquals(0L, plan.virtualPositionAt(0, 0L))
        assertEquals(1500L, plan.virtualPositionAt(0, 1500L))
        assertEquals(3000L, plan.virtualPositionAt(0, 3000L))

        // Position within clip 1 (cumulative start is 3000)
        assertEquals(3000L, plan.virtualPositionAt(1, 0L))
        assertEquals(5000L, plan.virtualPositionAt(1, 2000L))
        assertEquals(7000L, plan.virtualPositionAt(1, 4000L))

        // Position within clip 2 (cumulative start is 7000)
        assertEquals(7000L, plan.virtualPositionAt(2, 0L))
        assertEquals(9000L, plan.virtualPositionAt(2, 2000L))
        assertEquals(11000L, plan.virtualPositionAt(2, 4000L))
    }

    @Test
    fun `findClipForVirtualPosition maps virtual seek target to correct clip index and offset`() {
        val clips = listOf(
            AudioClip(id = "quran:1:1", uri = "file:///ch1.mp3", startMs = 0L, endMs = 3000L),
            AudioClip(id = "transl:1:1", uri = "file:///tr1.mp3", startMs = 1000L, endMs = 5000L),
            AudioClip(id = "quran:1:2", uri = "file:///ch1.mp3", startMs = 3000L, endMs = 7000L),
        )

        val plan = VerseClipPlan.from(clips)

        // At virtual 0ms -> clip 0, offset 0
        val seek0 = plan.findClipForVirtualPosition(0L)
        assertEquals(0, seek0.first)
        assertEquals(0L, seek0.second)

        // At virtual 2500ms -> clip 0, offset 2500
        val seek1 = plan.findClipForVirtualPosition(2500L)
        assertEquals(0, seek1.first)
        assertEquals(2500L, seek1.second)

        // At virtual 4500ms -> clip 1, offset 1500 (since clip 1 starts at 3000)
        val seek2 = plan.findClipForVirtualPosition(4500L)
        assertEquals(1, seek2.first)
        assertEquals(1500L, seek2.second)

        // At virtual 8000ms -> clip 2, offset 1000 (since clip 2 starts at 7000)
        val seek3 = plan.findClipForVirtualPosition(8000L)
        assertEquals(2, seek3.first)
        assertEquals(1000L, seek3.second)

        // Past total duration (15000ms) -> clamps to last clip, end of clip
        val seekEnd = plan.findClipForVirtualPosition(15000L)
        assertEquals(2, seekEnd.first)
        assertEquals(4000L, seekEnd.second)
    }

    @Test
    fun `verse query methods identify verses and first index correctly`() {
        val clips = listOf(
            AudioClip(id = "quran:1:1", uri = "file:///ch1.mp3", startMs = 0L, endMs = 3000L),
            AudioClip(id = "transl:1:1", uri = "file:///tr1.mp3", startMs = 1000L, endMs = 5000L),
            AudioClip(id = "quran:1:2", uri = "file:///ch1.mp3", startMs = 3000L, endMs = 7000L),
        )

        val plan = VerseClipPlan.from(clips)

        assertTrue(plan.hasVerse(1))
        assertTrue(plan.hasVerse(2))
        assertFalse(plan.hasVerse(3))

        assertEquals(0, plan.firstIndexForVerse(1))
        assertEquals(2, plan.firstIndexForVerse(2))
    }
}
