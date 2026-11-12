package com.quranapp.android.compose.components.reader

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranapp.android.R
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.compose.components.common.Loader
import com.quranapp.android.compose.components.reader.navigator.ReaderFooterNavigator
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.db.entities.user.BookmarkKey
import com.quranapp.android.db.entities.wbw.WbwWordEntity
import com.quranapp.android.db.relations.VerseWithDetails
import com.quranapp.android.utils.reader.MUSHAF_FONT_WIDTH_DP_MAX
import com.quranapp.android.utils.reader.atlas.AtlasGlyphPlacement
import com.quranapp.android.viewModels.ReaderUiState
import com.quranapp.android.viewModels.ReaderViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.time.Duration.Companion.milliseconds

enum class ReaderMode(val value: String) {
    VerseByVerse("mode_vbv"),
    Reading("mode_reading"),
    Translation("mode_translation");

    companion object {
        fun fromValue(value: String): ReaderMode {
            return entries.find { it.value == value } ?: VerseByVerse
        }

        fun fromLegacyStyleInt(style: Int): ReaderMode = when (style) {
            0x2 -> Reading
            else -> VerseByVerse
        }
    }
}

/* Layout used inside [ReaderMode.Reading]. */
enum class ReadingLayout(val value: String) {
    Page("page"),
    Flow("flow");

    companion object {
        fun fromValue(value: String): ReadingLayout {
            return entries.find { it.value == value } ?: Page
        }
    }
}


sealed class ReaderLayoutItem() {
    abstract val key: String

    data class ChapterInfo(val chapterNo: Int, override val key: String) : ReaderLayoutItem()
    data class Bismillah(override val key: String) : ReaderLayoutItem()
    data class IsVotd(override val key: String) : ReaderLayoutItem()
    data class ChapterTitle(val chapterNo: Int, override val key: String) : ReaderLayoutItem()

    data class VerseUI(
        val verse: VerseWithDetails,
        val atlasPlacements: Map<Int, List<AtlasGlyphPlacement>>,
        // list of Pair<langCode, text>
        val parsedTranslationTexts: List<Pair<String, AnnotatedString>> = emptyList(),
        val wbwByWordIndex: Map<Int, WbwWordEntity>? = null,
        val showDivider: Boolean = true,
        override val key: String
    ) : ReaderLayoutItem()

    data class SectionMarker(
        val text: String,
        override val key: String,
    ) : ReaderLayoutItem()
}

data class ReaderPreparedData(
    val items: List<ReaderLayoutItem>,
    /** Quran text style per mushaf page for Arabic in this reader session. */
    val textStyles: Map<Int, TextStyle> = emptyMap(),
)


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReaderLayout(
    readerVm: ReaderViewModel,
    nestedScrollConnection: NestedScrollConnection?,
    onSyncStateChanged: (Boolean) -> Unit = {},
) {
    val uiState by readerVm.uiState.collectAsStateWithLifecycle()
    val readerMode by readerVm.readerMode.collectAsState()
    if (readerMode == null) {
        return Loader(fill = true)
    }

    val highlightedVerse = readerVm.transientHighlightedVerse
    val highlightAlpha = remember { Animatable(1f) }
    val highlightAlphaState = remember(highlightAlpha) {
        derivedStateOf { highlightAlpha.value }
    }
    val transientHighlight = remember(highlightedVerse, highlightAlphaState) {
        TransientVerseHighlight(highlightedVerse, highlightAlphaState)
    }

    LaunchedEffect(highlightedVerse) {
        if (highlightedVerse == null) {
            highlightAlpha.snapTo(1f)
            return@LaunchedEffect
        }

        highlightAlpha.snapTo(1f)

        repeat(2) {
            highlightAlpha.animateTo(0.08f, tween(450))
            highlightAlpha.animateTo(1f, tween(450))
        }

        highlightAlpha.animateTo(0f, tween(1000))
    }

    val prevMode = remember { mutableStateOf(readerMode) }

    LaunchedEffect(readerMode) {
        val prev = prevMode.value
        prevMode.value = readerMode

        if (prev != null && readerMode != null && prev != readerMode) {
            readerVm.handleModeTransition(readerMode!!)
        }
    }

    CompositionLocalProvider(
        LocalTransientVerseHighlight provides transientHighlight
    ) {
        when (readerMode) {
            ReaderMode.Reading -> {
                val readingLayout = ReaderPreferences.observeReadingLayout()

                if (readingLayout == ReadingLayout.Flow) {
                    ReaderLayoutReadingFlow(
                        readerVm,
                        nestedScrollConnection,
                        onSyncStateChanged,
                    )
                } else {
                    BoxWithConstraints(modifier = Modifier.fillMaxHeight()) {
                        val contentWidth = maxWidth
                            .coerceAtMost(MUSHAF_FONT_WIDTH_DP_MAX.dp)
                            .coerceAtLeast(1.dp)

                        ReaderLayoutPageMode(
                            readerVm,
                            contentWidth,
                            nestedScrollConnection,
                            onSyncStateChanged,
                        )
                    }
                }
            }

            ReaderMode.Translation -> {
                ReaderLayoutTranslationPageMode(
                    readerVm,
                    nestedScrollConnection,
                    onSyncStateChanged,
                )
            }

            else -> ReaderLayoutVerseMode(
                readerVm,
                uiState,
                nestedScrollConnection,
                onSyncStateChanged,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReaderLayoutVerseMode(
    readerVm: ReaderViewModel,
    uiState: ReaderUiState,
    nestedScrollConnection: NestedScrollConnection?,
    onSyncStateChanged: (Boolean) -> Unit,
) {
    val listState = rememberLazyListState()
    val prepared by readerVm.verseByVersePrepared.collectAsStateWithLifecycle()
    val items = prepared.items

    val allBookmarks by readerVm.userRepository.getBookmarksFlow()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val bookmarkedVerseKeys = remember(allBookmarks) {
        allBookmarks.map {
            BookmarkKey(
                chapterNo = it.chapterNo,
                fromVerse = it.fromVerseNo,
                toVerse = it.toVerseNo
            )
        }.toHashSet()
    }

    LaunchedEffect(listState, items) {
        snapshotFlow { listState.isScrollInProgress to listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collectLatest { (scrolling, index) ->
                if (scrolling) return@collectLatest

                delay(180.milliseconds)

                if (listState.isScrollInProgress || listState.firstVisibleItemIndex != index) {
                    return@collectLatest
                }

                val visibleItems = listState.layoutInfo.visibleItemsInfo
                for (visibleItem in visibleItems) {
                    val verseItem = items.getOrNull(visibleItem.index) as? ReaderLayoutItem.VerseUI
                        ?: continue
                    readerVm.updateLastKnownVerse(ChapterVersePair(verseItem.verse))
                    break
                }
            }
    }

    LaunchedEffect(uiState.viewType) {
        snapshotFlow { uiState.viewType }
            .distinctUntilChanged()
            .collect { listState.scrollToItem(0) }
    }

    var autoScrollSpeed by readerVm.autoScrollSpeed
    var playerVerseSync by readerVm.playerVerseSync
    val playerState = LocalRecitation.current

    val isPlaying = playerState.isAnyPlaying
    val playingVerse = playerState.playingVerse

    LaunchedEffect(listState, autoScrollSpeed, playerVerseSync, isPlaying, playingVerse, items) {
        if (autoScrollSpeed == null && playerVerseSync && isPlaying && playingVerse.isValid) {
            val currentPlayingIndex = items.indexOfFirst { item ->
                item is ReaderLayoutItem.VerseUI &&
                        item.verse.chapterNo == playingVerse.chapterNo &&
                        item.verse.verseNo == playingVerse.verseNo
            }

            if (currentPlayingIndex >= 0) {
                val layoutInfo = listState.layoutInfo
                val playingItem = layoutInfo.visibleItemsInfo
                    .firstOrNull { it.index == currentPlayingIndex }

                val isFullyVisible = playingItem != null &&
                        playingItem.offset >= layoutInfo.viewportStartOffset &&
                        playingItem.offset + playingItem.size <= layoutInfo.viewportEndOffset

                if (!isFullyVisible) {
                    // Keep some already-read content above the active ayah so the
                    // transition is easier to follow than repeatedly pinning it to the top.
                    val viewportHeight =
                        layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
                    listState.animateScrollToItem(
                        index = currentPlayingIndex
                    )
                }
            }
        }
    }

    LaunchedEffect(listState, items, playingVerse) {
        snapshotFlow {
            if (!playingVerse.isValid) return@snapshotFlow false

            val idx = items.indexOfFirst { item ->
                item is ReaderLayoutItem.VerseUI &&
                        item.verse.chapterNo == playingVerse.chapterNo &&
                        item.verse.verseNo == playingVerse.verseNo
            }

            if (idx < 0) return@snapshotFlow false

            return@snapshotFlow true
        }
            .distinctUntilChanged()
            .collect { onSyncStateChanged(it) }
    }

    val navigateToVerse by readerVm.navigateToVerse.collectAsStateWithLifecycle()

    LaunchedEffect(navigateToVerse, items) {
        val (chapterNo, verseNo) = navigateToVerse ?: return@LaunchedEffect

        val idx = items.indexOfFirst { item ->
            item is ReaderLayoutItem.VerseUI &&
                    item.verse.chapterNo == chapterNo &&
                    item.verse.verseNo == verseNo
        }

        if (idx >= 0) {
            val optimalIdx = items.findOptimalScrollIndex(idx)
            listState.scrollToItem(optimalIdx)
            readerVm.completeVerseNavigation(ChapterVersePair(chapterNo, verseNo))
        }
    }

    LaunchedEffect(listState, autoScrollSpeed) {
        val speed = autoScrollSpeed

        while (speed != null) {
            listState.scrollBy(speed)
            delay(16.milliseconds) // ~60 FPS
        }
    }

    TextStyleProvider(prepared.textStyles) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (nestedScrollConnection == null) Modifier
                    else Modifier.nestedScroll(nestedScrollConnection)
                )
                .pointerInput(autoScrollSpeed) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.any { it.pressed }) {
                                autoScrollSpeed = null
                            }
                        }
                    }
                },
            contentPadding = PaddingValues(top = 16.dp, bottom = 240.dp)
        ) {
            items(
                items = items,
                key = { item -> item.key },
            ) { item ->
                TranslationRow(readerVm, item, bookmarkedVerseKeys)
            }

            if (uiState.viewType != null && items.isNotEmpty()) {
                item(key = "verse_reader_nav_footer") {
                    ReaderFooterNavigator(
                        readerVm = readerVm,
                        viewType = uiState.viewType,
                        listState = listState,
                    )
                }
            }
        }
    }
}


@Composable
private fun TranslationRow(
    readerVm: ReaderViewModel,
    item: ReaderLayoutItem,
    bookmarkedVerseKeys: Set<BookmarkKey>,
) {
    when (item) {
        is ReaderLayoutItem.Bismillah -> Bismillah()
        is ReaderLayoutItem.IsVotd -> IsVotd()
        is ReaderLayoutItem.ChapterInfo -> ChapterInfoCard(item.chapterNo)
        is ReaderLayoutItem.ChapterTitle -> ChapterTitle(item.chapterNo)
        is ReaderLayoutItem.SectionMarker -> SectionMarkerRow(item)
        is ReaderLayoutItem.VerseUI -> {
            val isBookmarked = BookmarkKey(
                chapterNo = item.verse.chapterNo,
                fromVerse = item.verse.verseNo,
                toVerse = item.verse.verseNo
            ) in bookmarkedVerseKeys

            VerseView(
                verseUi = item,
                isBookmarked = isBookmarked,
                showDivider = item.showDivider,
            )
        }
    }
}

@Composable
private fun SectionMarkerRow(marker: ReaderLayoutItem.SectionMarker) {
    if (marker.text.isEmpty()) return

    val decorationTint = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {

        Image(
            painterResource(R.drawable.vector_1),
            contentDescription = null,
            modifier = Modifier.height(24.dp),
            colorFilter = ColorFilter.tint(decorationTint)
        )

        Text(
            text = marker.text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp),
            textAlign = TextAlign.Center
        )

        Image(
            painterResource(R.drawable.vector_1),
            contentDescription = null,
            modifier = Modifier
                .height(24.dp)
                .scale(-1f, 1f),
            colorFilter = ColorFilter.tint(decorationTint)
        )
    }
}

private fun List<ReaderLayoutItem>.findOptimalScrollIndex(targetIndex: Int): Int {
    if (targetIndex <= 0) return targetIndex.fastCoerceAtLeast(0)

    var optimalIndex = targetIndex
    var i = targetIndex - 1

    while (i >= 0) {
        when (this[i]) {
            is ReaderLayoutItem.ChapterInfo,
            is ReaderLayoutItem.Bismillah,
            is ReaderLayoutItem.IsVotd,
            is ReaderLayoutItem.ChapterTitle -> {
                optimalIndex = i
                i--
            }

            else -> break
        }
    }

    return optimalIndex
}
