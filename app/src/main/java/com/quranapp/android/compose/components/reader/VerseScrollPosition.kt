package com.quranapp.android.compose.components.reader

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlin.time.Duration.Companion.milliseconds

internal class VerseScrollAnchor {
    var viewportTopPx by mutableIntStateOf(Int.MIN_VALUE)
    var viewportBottomPx by mutableIntStateOf(Int.MIN_VALUE)
    var targetTopPx by mutableIntStateOf(Int.MIN_VALUE)
    var targetBottomPx by mutableIntStateOf(Int.MIN_VALUE)

    fun beginTarget() {
        targetTopPx = Int.MIN_VALUE
        targetBottomPx = Int.MIN_VALUE
    }
}

private data class VerseScrollBounds(
    val targetTop: Int,
    val targetBottom: Int,
    val viewportTop: Int,
    val viewportBottom: Int,
)

@OptIn(FlowPreview::class)
internal suspend fun ScrollableState.scrollToVersePosition(
    anchor: VerseScrollAnchor,
) {
    val desiredTopPx = 100
    snapshotFlow {
        VerseScrollBounds(
            targetTop = anchor.targetTopPx,
            targetBottom = anchor.targetBottomPx,
            viewportTop = anchor.viewportTopPx,
            viewportBottom = anchor.viewportBottomPx,
        )
    }
        .filter { bounds ->
            bounds.targetTop != Int.MIN_VALUE && bounds.targetBottom != Int.MIN_VALUE &&
                    bounds.viewportTop != Int.MIN_VALUE && bounds.viewportBottom != Int.MIN_VALUE
        }
        .distinctUntilChanged()
        .onEach { bounds ->
            val targetIsVisible =
                bounds.targetTop >= bounds.viewportTop &&
                        bounds.targetBottom <= bounds.viewportBottom

            if (!targetIsVisible) {
                scrollBy((bounds.targetTop - bounds.viewportTop - desiredTopPx).toFloat())
            }
        }
        .debounce(80.milliseconds)
        .first()
}
