package com.quranapp.android.compose.components.reader

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranapp.android.components.quran.subcomponents.Verse
import com.quranapp.android.compose.components.common.Loader
import com.quranapp.android.db.entities.BookmarkKey
import com.quranapp.android.viewModels.ReaderUiState
import com.quranapp.android.viewModels.ReaderViewModel
import kotlinx.coroutines.delay

enum class ReaderMode(val value: String) {
    VerseByVerse("mode_vbv"),
    Reading("mode_reading"),
    Translation("mode_translation");

    companion object {
        fun fromValue(value: String): ReaderMode {
            return values().find { it.value == value } ?: ReaderMode.VerseByVerse
        }
    }
}


sealed class ReaderLayoutItem(var key: String? = null) {
    data class ChapterInfo(val chapterNo: Int) : ReaderLayoutItem()
    data object Bismillah : ReaderLayoutItem()
    data object IsVotd : ReaderLayoutItem()
    data class ChapterTitle(val chapterNo: Int) : ReaderLayoutItem()
    data class VerseUI(
        val verse: Verse,
        val parsedQuranText: AnnotatedString? = null,
        val parsedTranslationTexts: List<Pair<String, AnnotatedString>> = emptyList(),
        val isLastInGroup: Boolean = false,
    ) : ReaderLayoutItem()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReaderLayout(
    readerVm: ReaderViewModel,
) {
    val uiState by readerVm.uiState.collectAsStateWithLifecycle()
    val readerMode by readerVm.readerMode.collectAsState()

    if (readerMode == null) {
        return Loader(fill = true)
    }

    when (uiState.transientReaderMode ?: readerMode) {
        ReaderMode.Reading -> {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            ) {
                val horizontalPadding = 16.dp * 2

                val contentWidthPx = with(LocalDensity.current) {
                    (maxWidth - horizontalPadding).coerceAtLeast(1.dp).roundToPx()
                }

                ReaderLayoutPageMode(readerVm, contentWidthPx)
            }
        }

        ReaderMode.Translation -> {}
        else -> ReaderLayoutVerseMode(readerVm, uiState)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReaderLayoutVerseMode(
    readerVm: ReaderViewModel,
    uiState: ReaderUiState,
) {
    val listState = rememberLazyListState()
    val items by readerVm.verseByVerseItems.collectAsStateWithLifecycle()
    val allBookmarks by readerVm.bookmarksRepository.getBookmarksFlow()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    // Build only the keys that exist in the current list
    val visibleVerseKeys = remember(items) {
        items.asSequence()
            .filterIsInstance<ReaderLayoutItem.VerseUI>()
            .map {
                BookmarkKey(
                    chapterNo = it.verse.chapterNo,
                    fromVerse = it.verse.verseNo,
                    toVerse = it.verse.verseNo
                )
            }
            .toHashSet()
    }

    val bookmarkedVerseKeys = remember(allBookmarks, visibleVerseKeys) {
        allBookmarks.asSequence()
            .map {
                BookmarkKey(
                    chapterNo = it.chapterNo,
                    fromVerse = it.fromVerseNo,
                    toVerse = it.toVerseNo
                )
            }
            .filterNotNull()
            .filter { it in visibleVerseKeys }
            .toHashSet()
    }

    var autoScrollSpeed by readerVm.autoScrollSpeed
    var playerVerseSync by readerVm.playerVerseSync

    val playerState = LocalRecitationState.current

    val isPlaying = playerState.isAnyPlaying
    val playingVerse = playerState.playingVerse

    LaunchedEffect(listState, autoScrollSpeed, playerVerseSync, isPlaying, playingVerse, items) {
        if (autoScrollSpeed == null && playerVerseSync && isPlaying) {
            val currentPlayingIndex = items.indexOfFirst { item ->
                item is ReaderLayoutItem.VerseUI &&
                        item.verse.chapterNo == playingVerse.chapterNo &&
                        item.verse.verseNo == playingVerse.verseNo
            }

            listState.animateScrollToItem(currentPlayingIndex)
        }
    }

    LaunchedEffect(listState, autoScrollSpeed) {
        val speed = autoScrollSpeed

        while (speed != null) {
            listState.scrollBy(speed)
            delay(16L) // ~60 FPS
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
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
            key = { item -> item.key ?: "" },
        ) { item ->
            TranslationRow(readerVm, item, bookmarkedVerseKeys)
        }
    }
}

@Composable
private fun TranslationRow(
    readerVm: ReaderViewModel,
    item: ReaderLayoutItem,
    bookmarkedVerseKeys: Set<BookmarkKey>
) {
    when (item) {
        ReaderLayoutItem.Bismillah -> Bismillah()
        ReaderLayoutItem.IsVotd -> IsVotd()
        is ReaderLayoutItem.ChapterInfo -> ChapterInfoCard(item.chapterNo)
        is ReaderLayoutItem.ChapterTitle -> ChapterTitle(item.chapterNo)
        is ReaderLayoutItem.VerseUI -> {
            val isBookmarked = BookmarkKey(
                chapterNo = item.verse.chapterNo,
                fromVerse = item.verse.verseNo,
                toVerse = item.verse.verseNo
            ) in bookmarkedVerseKeys

            VerseView(
                verseUi = item,
                isBookmarked = isBookmarked,
                showDivider = !item.isLastInGroup,
            )
        }
    }
}
