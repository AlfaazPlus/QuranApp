package com.quranapp.android.compose.components.reader

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranapp.android.compose.components.common.Loader
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.db.entities.quran.AyahWordEntity
import com.quranapp.android.utils.mediaplayer.RecitationController
import com.quranapp.android.utils.reader.MUSHAF_PAGE_HORIZONTAL_PADDING
import com.quranapp.android.utils.reader.PageBuilderParams
import com.quranapp.android.utils.reader.rememberQuranMushafId
import com.quranapp.android.utils.univ.MessageUtils
import com.quranapp.android.viewModels.ReaderViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import verticalFadingEdge


data class MushafLineLayout(
    val fittedStyle: TextStyle,
    val centeredGap: androidx.compose.ui.unit.Dp,
)

sealed class QuranPageLineItem {
    abstract val lineNo: Int

    data class Title(
        override val lineNo: Int, val chapterNo: Int
    ) : QuranPageLineItem()

    data class Bismillah(
        override val lineNo: Int
    ) : QuranPageLineItem()

    data class Text(
        override val lineNo: Int,
        val centered: Boolean,
        val words: List<AyahWordEntity>,
        val layout: MushafLineLayout
    ) : QuranPageLineItem()
}

data class QuranPageItem(
    val pageNo: Int,
    val juzNo: Int,
    val lines: List<QuranPageLineItem>,
)

private data class MushafPageMeasurementKey(
    val pageNo: Int,
    val script: String,
    val contentWidthPx: Int,
    val density: Float,
    val fontScale: Float,
    val styleHash: Int,
)


@Composable
fun ReaderLayoutPageMode(
    readerVm: ReaderViewModel,
    contentWidth: Dp,
) {
    val mushafId = rememberQuranMushafId()
    val uiState by readerVm.uiState.collectAsStateWithLifecycle()

    val pageCount by produceState(0) {
        value = readerVm.mushafPageCount(mushafId)
    }

    val context = LocalContext.current
    val pagerState = rememberPagerState(
        initialPage = uiState.currentPageNo?.let { it - 1 } ?: 0,
        pageCount = { pageCount },
    )
    val textMeasurer = rememberTextMeasurer(cacheSize = 2048)
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val density = LocalDensity.current

    LaunchedEffect(pagerState, pageCount) {
        snapshotFlow {
            listOf(
                pagerState.currentPage + 1,
                pagerState.targetPage + 1,
                pagerState.settledPage + 1,
            )
        }
            .distinctUntilChanged()
            .collect { anchorPages ->
                if (pageCount > 0) {
                    readerVm.prefetchMushafPages(
                        context, anchorPages, pageCount, PageBuilderParams(
                            context = context,
                            colors = colors,
                            type = typography,
                            textMeasurer = textMeasurer,
                            density = density,
                            contentWidthPx = with(density) {
                                (contentWidth - MUSHAF_PAGE_HORIZONTAL_PADDING * 2).roundToPx()
                            }
                        )
                    )
                }
            }
    }

    LaunchedEffect(context, pagerState, pageCount) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { currentPage ->
                val currentPageNo = currentPage + 1

                readerVm.updateState {
                    it.copy(
                        currentPageNo = if (pageCount > 0) currentPageNo else null
                    )
                }

                if (pageCount > 0) {
                    readerVm.updateLastKnownVerseFromPage(currentPageNo)
                }
            }
    }

    val navigateToPage by readerVm.navigateToPage.collectAsStateWithLifecycle()

    LaunchedEffect(navigateToPage, pageCount) {
        val target = navigateToPage ?: return@LaunchedEffect
        if (target in 1..pageCount) {
            pagerState.scrollToPage(target - 1)
            readerVm.consumePageNavigation()
        }
    }

    HorizontalPager(
        state = pagerState,
        beyondViewportPageCount = 1,
        reverseLayout = true,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) { page ->
        PageModePage(
            readerVm = readerVm,
            pageNo = page + 1,
            contentWidth,
        )
    }
}

@Composable
private fun PageModePage(
    readerVm: ReaderViewModel,
    pageNo: Int,
    contentWidth: Dp,
) {
    val item = readerVm.pageItems[pageNo]

    if (item == null) {
        return Loader(true)
    }

    val scrollState = rememberScrollState()

    val playerState = LocalRecitationState.current
    val isPlaying = playerState.isAnyPlaying
    val playingVerse = playerState.playingVerse

    val playingWordKeys = remember(isPlaying, item.lines, playingVerse) {
        if (!isPlaying) return@remember emptySet()

        item.lines
            .filterIsInstance<QuranPageLineItem.Text>()
            .flatMap { it.words }
            .filter { word -> playingVerse.doesEqual(word.ayahId) }
            .map { word -> word.ayahId to word.wordIndex }
            .toSet()
    }

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = contentWidth)
                .verticalFadingEdge(scrollState, color = colorScheme.surface, length = 48.dp),
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Column(
                    Modifier
                        .verticalScroll(scrollState)
                        .padding(
                            start = MUSHAF_PAGE_HORIZONTAL_PADDING,
                            end = MUSHAF_PAGE_HORIZONTAL_PADDING,
                            top = 16.dp,
                            bottom = 64.dp
                        )
                        .fillMaxWidth(),
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        item.lines.forEach { line ->
                            key(line.lineNo) {
                                when (line) {
                                    is QuranPageLineItem.Title -> ChapterTitle(line.chapterNo)
                                    is QuranPageLineItem.Bismillah -> Bismillah()
                                    is QuranPageLineItem.Text -> MushafLineText(
                                        textLine = line,
                                        layout = line.layout,
                                        playingWordKeys = playingWordKeys,
                                        controller = playerState.controller
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MushafLineText(
    textLine: QuranPageLineItem.Text,
    layout: MushafLineLayout,
    playingWordKeys: Set<Pair<Int, Int>>,
    controller: RecitationController,
) {
    val words = textLine.words
    val fittedStyle = layout.fittedStyle
    val centeredGap = layout.centeredGap

    if (textLine.centered) {
        Box(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(
                    centeredGap, Alignment.CenterHorizontally
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (word in words) {
                    Word(
                        word,
                        fittedStyle,
                        isHighlighted = (word.ayahId to word.wordIndex) in playingWordKeys,
                        controller
                    )
                }
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (word in words) {
                Word(
                    word,
                    fittedStyle,
                    isHighlighted = (word.ayahId to word.wordIndex) in playingWordKeys,
                    controller
                )
            }
        }
    }
}

@Composable
private fun Word(
    word: AyahWordEntity,
    fittedStyle: TextStyle,
    isHighlighted: Boolean,
    controller: RecitationController
) {
    val context = LocalContext.current

    Text(
        text = word.text,
        color = colorScheme.onBackground,
        style = fittedStyle,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier
            .background(
                if (isHighlighted) colorScheme.primary.alpha(0.4f)
                else Color.Transparent
            )
            .clickable {
                if (word.isLastWordOfAyah) {
                    MessageUtils.showRemovableToast(context, "LAST WORD", Toast.LENGTH_LONG)
                } else {
                    MessageUtils.showRemovableToast(context, word.text, Toast.LENGTH_LONG)
                }
            },
    )
}