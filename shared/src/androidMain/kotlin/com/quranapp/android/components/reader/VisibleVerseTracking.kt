package com.quranapp.android.components.reader

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.quranapp.android.db.entities.quran.AyahWordEntity
import com.quranapp.android.models.ChapterVersePair
import com.quranapp.android.utils.quran.QuranMeta
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
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

internal suspend fun LazyListState.onScrollIdle(
    isNavigating: () -> Boolean,
    onIdle: (index: Int) -> Unit,
) {
    var lastIndex: Int? = null
    var sawScroll = false
    var wasNavigating = false

    snapshotFlow {
        val navigating = isNavigating()
        val scrolling = isScrollInProgress
        val index = firstVisibleItemIndex

        (index.toLong() shl 2) or
                (if (scrolling) 2L else 0L) or
                (if (navigating) 1L else 0L)
    }
        .distinctUntilChanged()
        .collectLatest { packed ->
            val navigating = (packed and 1L) != 0L
            val scrolling = (packed and 2L) != 0L
            val index = (packed shr 2).toInt()

            if (navigating) {
                wasNavigating = true
                sawScroll = false
                lastIndex = index
                return@collectLatest
            }

            if (scrolling) {
                sawScroll = true
                return@collectLatest
            }

            val indexChanged = !wasNavigating && lastIndex != null && lastIndex != index

            wasNavigating = false
            lastIndex = index

            if (!sawScroll && !indexChanged) return@collectLatest
            sawScroll = false

            delay(180.milliseconds)

            if (isNavigating() || isScrollInProgress || firstVisibleItemIndex != index) {
                return@collectLatest
            }

            onIdle(index)
        }
}

internal suspend fun LazyListState.updateVisibleVerseWhenIdle(
    pageCount: Int,
    trackers: VerseVisibilityRegistry,
    viewport: VerseScrollAnchor,
    isNavigating: () -> Boolean,
    onVerseVisible: (ChapterVersePair) -> Unit,
) {
    onScrollIdle(isNavigating = isNavigating) { index ->
        if (index !in 0 until pageCount) return@onScrollIdle

        trackers.forPage(index + 1)
            ?.visibleVerse(viewport.viewportTopPx, viewport.viewportBottomPx)
            ?.let(onVerseVisible)
    }
}

internal suspend fun LazyListState.updatePageWhenIdle(
    pageCount: Int,
    isNavigating: () -> Boolean,
    onPageVisible: (Int) -> Unit,
) {
    onScrollIdle(isNavigating = isNavigating) { index ->
        if (index in 0 until pageCount) {
            onPageVisible(index + 1)
        }
    }
}

internal suspend fun ScrollState.updateVisibleVerseWhenIdle(
    tracker: VerseVisibilityTracker,
    viewport: VerseScrollAnchor,
    isNavigating: () -> Boolean,
    shouldSyncOnSettle: () -> Boolean,
    onSyncedOnSettle: () -> Unit,
    onVerseVisible: (ChapterVersePair) -> Unit,
) {
    var sawScroll = false
    var wasNavigating = false

    snapshotFlow {
        val navigating = isNavigating()
        val scrolling = isScrollInProgress
        val syncOnSettle = shouldSyncOnSettle()
        (if (syncOnSettle) 4 else 0) or
                (if (scrolling) 2 else 0) or
                (if (navigating) 1 else 0)
    }
        .distinctUntilChanged()
        .collectLatest { mask ->
            val navigating = (mask and 1) != 0
            val scrolling = (mask and 2) != 0
            val syncOnSettle = (mask and 4) != 0

            if (navigating) {
                wasNavigating = true
                sawScroll = false
                return@collectLatest
            }

            if (scrolling) {
                sawScroll = true
                return@collectLatest
            }

            val shouldUpdate = (!wasNavigating && sawScroll) || syncOnSettle
            wasNavigating = false
            if (!shouldUpdate) return@collectLatest
            sawScroll = false

            delay(180.milliseconds)
            if (isNavigating() || isScrollInProgress) return@collectLatest

            tracker.visibleVerse(viewport.viewportTopPx, viewport.viewportBottomPx)
                ?.let(onVerseVisible)

            if (syncOnSettle) {
                onSyncedOnSettle()
            }
        }
}
