package com.quranapp.android.compose.components.reader

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranapp.android.compose.components.common.Loader
import com.quranapp.android.compose.components.reader.navigator.ReaderFooterNavigator
import com.quranapp.android.db.entities.BookmarkKey
import com.quranapp.android.db.entities.wbw.WbwWordEntity
import com.quranapp.android.db.relations.VerseWithDetails
import com.quranapp.android.utils.reader.MUSHAF_FONT_WIDTH_DP_MAX
import com.quranapp.android.viewModels.ReaderUiState
import com.quranapp.android.viewModels.ReaderViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

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


sealed class ReaderLayoutItem() {
    abstract val key: String

    data class ChapterInfo(val chapterNo: Int, override val key: String) : ReaderLayoutItem()
    data class Bismillah(override val key: String) : ReaderLayoutItem()
    data class IsVotd(override val key: String) : ReaderLayoutItem()
    data class ChapterTitle(val chapterNo: Int, override val key: String) : ReaderLayoutItem()

    data class VerseUI(
        val verse: VerseWithDetails,
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
    nestedScrollConnection: NestedScrollConnection,
    onSyncStateChanged: (Boolean) -> Unit = {},
) {
    val uiState by readerVm.uiState.collectAsStateWithLifecycle()
    val readerMode by readerVm.readerMode.collectAsState()

    if (readerMode == null) {
        return Loader(fill = true)
    }

    val prevMode = remember { mutableStateOf(readerMode) }

    LaunchedEffect(readerMode) {
        val prev = prevMode.value
        prevMode.value = readerMode

        if (prev != null && readerMode != null && prev != readerMode) {
            readerVm.handleModeTransition(readerMode!!)
        }
    }

    when (readerMode) {
        ReaderMode.Reading -> {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxHeight()
            ) {
                val contentWidth =
                    (maxWidth.coerceAtMost(MUSHAF_FONT_WIDTH_DP_MAX.dp))
                        .coerceAtLeast(1.dp)

                ReaderLayoutPageMode(
                    readerVm,
                    contentWidth,
                    nestedScrollConnection,
                    onSyncStateChanged,
                )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReaderLayoutVerseMode(
    readerVm: ReaderViewModel,
    uiState: ReaderUiState,
    nestedScrollConnection: NestedScrollConnection,
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

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { readerVm.updateLastKnownVerseFromItems(it) }
    }

    LaunchedEffect(prepared) {
        if (items.isNotEmpty()) {
            readerVm.updateLastKnownVerseFromItems(listState.firstVisibleItemIndex)
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
                listState.animateScrollToItem(currentPlayingIndex)
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
            readerVm.consumeVerseNavigation()
        }
    }

    LaunchedEffect(listState, autoScrollSpeed) {
        val speed = autoScrollSpeed

        while (speed != null) {
            listState.scrollBy(speed)
            delay(16L) // ~60 FPS
        }
    }

    TextStyleProvider(prepared.textStyles) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(
            modifier = Modifier
                .weight(1f)
                .widthIn(min = 100.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )

        Text(
            text = marker.text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp),
            textAlign = TextAlign.Center
        )

        HorizontalDivider(
            modifier = Modifier
                .weight(1f)
                .widthIn(min = 100.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
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
