package com.quranapp.android.compose.components.reader

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.db.entities.quran.AyahWordEntity
import com.quranapp.android.utils.quran.QuranMeta
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import java.util.HashMap
import kotlin.time.Duration.Companion.milliseconds

internal class VerseVisibilityTracker(ayahIds: IntArray) {
    private val ids = ayahIds
    private val indices = HashMap<Int, Int>(ayahIds.size)
    private val tops = IntArray(ayahIds.size) { Int.MIN_VALUE }
    private val bottoms = IntArray(ayahIds.size) { Int.MIN_VALUE }

    init {
        for (index in ayahIds.indices) indices[ayahIds[index]] = index
    }

    fun recordVerseStart(ayahId: Int, top: Int) {
        val index = indices[ayahId] ?: return
        tops[index] = top
    }

    fun recordVerseEnd(ayahId: Int, bottom: Int) {
        val index = indices[ayahId] ?: return
        bottoms[index] = bottom
    }

    fun recordBounds(ayahId: Int, top: Int, bottom: Int) {
        val index = indices[ayahId] ?: return
        tops[index] = top
        bottoms[index] = bottom
    }

    fun recordRow(words: List<AyahWordEntity>, top: Int, bottom: Int) {
        var previousAyahId = -1
        for (word in words) {
            if (word.ayahId == previousAyahId) continue
            recordBounds(word.ayahId, top, bottom)
            previousAyahId = word.ayahId
        }
    }

    fun visibleVerse(viewportTop: Int, viewportBottom: Int): ChapterVersePair? {
        var firstVerseBelowTop = -1

        for (index in ids.indices) {
            val top = tops[index]
            val bottom = bottoms[index]
            if (top == Int.MIN_VALUE || bottom == Int.MIN_VALUE) continue
            if (top <= viewportTop && bottom > viewportTop) return versePair(ids[index])
            if (firstVerseBelowTop < 0 && top > viewportTop && top < viewportBottom) {
                firstVerseBelowTop = index
            }
        }

        return firstVerseBelowTop.takeIf { it >= 0 }?.let { versePair(ids[it]) }
    }

    private fun versePair(ayahId: Int): ChapterVersePair =
        QuranMeta.getVerseNoFromAyahId(ayahId).let { (chapterNo, verseNo) ->
            ChapterVersePair(chapterNo, verseNo)
        }
}

internal class VerseVisibilityRegistry {
    private val trackers = HashMap<Int, VerseVisibilityTracker>()
    var version by mutableIntStateOf(0)
        private set

    fun register(pageNo: Int, tracker: VerseVisibilityTracker) {
        trackers[pageNo] = tracker
        version++
    }

    fun unregister(pageNo: Int, tracker: VerseVisibilityTracker) {
        if (trackers[pageNo] !== tracker) return
        trackers.remove(pageNo)
        version++
    }

    fun forPage(pageNo: Int): VerseVisibilityTracker? = trackers[pageNo]
}

internal suspend fun LazyListState.updateVisibleVerseWhenIdle(
    pageCount: Int,
    trackers: VerseVisibilityRegistry,
    viewport: VerseScrollAnchor,
    onVerseVisible: (ChapterVersePair) -> Unit,
) {
    snapshotFlow {
        trackers.version
        isScrollInProgress to firstVisibleItemIndex
    }
        .distinctUntilChanged()
        .collectLatest { (scrolling, index) ->
            if (scrolling || index !in 0 until pageCount) return@collectLatest

            delay(180.milliseconds)

            if (isScrollInProgress || firstVisibleItemIndex != index) return@collectLatest

            trackers.forPage(index + 1)
                ?.visibleVerse(viewport.viewportTopPx, viewport.viewportBottomPx)
                ?.let(onVerseVisible)
        }
}
