package com.quranapp.android.compose.components.reader

import androidx.compose.foundation.background
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.mediaplayer.RecitationController
import com.quranapp.android.utils.reader.MUSHAF_PAGE_HORIZONTAL_PADDING
import com.quranapp.android.utils.reader.PageBuilderParams
import com.quranapp.android.utils.reader.mushafShowsRuledPageDecoration
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
    nestedScrollConnection: NestedScrollConnection,
    onSyncStateChanged: (Boolean) -> Unit = {},
) {
    val uiState by readerVm.uiState.collectAsStateWithLifecycle()
    val mushafLayoutKey by readerVm.mushafLayoutKey
    val ruledPageDecoration = mushafLayoutKey.scriptCode.mushafShowsRuledPageDecoration()

    val pageCount by produceState(0, mushafLayoutKey) {
        value = readerVm.mushafPageCount(mushafLayoutKey.toMushafId())
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

    LaunchedEffect(pagerState, pageCount, mushafLayoutKey) {
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
                    readerVm.fetchMushafPages(
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

    val navigateToPage by readerVm.navigateToPage.collectAsStateWithLifecycle()

    LaunchedEffect(context, pagerState, pageCount, navigateToPage) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { currentPage ->
                // While a programmatic scroll is pending, the pager may still report page 1 until
                // [pageCount] is ready; do not overwrite [currentPageNo] from the initial open.
                if (navigateToPage != null) return@collect

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

    val navigateToVerse by readerVm.navigateToVerse.collectAsStateWithLifecycle()

    LaunchedEffect(navigateToPage, pageCount) {
        val targetPage = navigateToPage ?: return@LaunchedEffect
        if (targetPage in 1..pageCount) {
            pagerState.scrollToPage(targetPage - 1)
            readerVm.consumePageNavigation()
            Log.d("scrollToPage", targetPage)
        }
    }

    LaunchedEffect(navigateToVerse, pageCount) {
        val targetVerse = navigateToVerse ?: return@LaunchedEffect

        val targetPage = readerVm.resolvePageNo(targetVerse.chapterNo, targetVerse.verseNo)
            ?: return@LaunchedEffect

        Log.d("requestPageNavigation", targetPage)

        readerVm.requestPageNavigation(targetPage)
    }

    val playerState = LocalRecitation.current
    val isPlayingMushaf = playerState.isAnyPlaying
    val playingVerseMushaf = playerState.playingVerse
    var playerVerseSync by readerVm.playerVerseSync

    LaunchedEffect(playerVerseSync, isPlayingMushaf, playingVerseMushaf, pageCount) {
        if (!playerVerseSync || !isPlayingMushaf || !playingVerseMushaf.isValid || pageCount <= 0) {
            return@LaunchedEffect
        }

        val targetPage =
            readerVm.resolvePageNo(playingVerseMushaf.chapterNo, playingVerseMushaf.verseNo)
                ?: return@LaunchedEffect
        if (targetPage !in 1..pageCount) return@LaunchedEffect

        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { settledIdx ->
                val currentPage = settledIdx + 1
                if (currentPage != targetPage) {
                    readerVm.requestPageNavigation(targetPage)
                }
            }
    }

    LaunchedEffect(playingVerseMushaf, pageCount) {
        if (!playingVerseMushaf.isValid || pageCount <= 0) {
            onSyncStateChanged(false)
            return@LaunchedEffect
        }

        val expectedPage =
            readerVm.resolvePageNo(playingVerseMushaf.chapterNo, playingVerseMushaf.verseNo)

        if (expectedPage == null || expectedPage !in 1..pageCount) {
            onSyncStateChanged(false)
            return@LaunchedEffect
        }

        snapshotFlow { pagerState.settledPage + 1 }
            .distinctUntilChanged()
            .collect { current ->
                onSyncStateChanged(current == expectedPage)
            }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) { page ->
            PageModePage(
                readerVm = readerVm,
                pageNo = page + 1,
                contentWidth,
                ruledPageDecoration,
                nestedScrollConnection,
            )
        }
    }
}

@Composable
private fun PageModePage(
    readerVm: ReaderViewModel,
    pageNo: Int,
    contentWidth: Dp,
    ruledPageDecoration: Boolean,
    nestedScrollConnection: NestedScrollConnection,
) {
    val item = readerVm.pageItems[pageNo]

    if (item == null) {
        return Loader(true)
    }

    val scrollState = rememberScrollState()

    val playerState = LocalRecitation.current
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
                        .nestedScroll(nestedScrollConnection)
                        .padding(top = 16.dp, bottom = 64.dp)
                        .then(
                            if (ruledPageDecoration) {
                                Modifier
                            } else {
                                Modifier.padding(
                                    start = MUSHAF_PAGE_HORIZONTAL_PADDING,
                                    end = MUSHAF_PAGE_HORIZONTAL_PADDING,
                                )
                            }
                        )
                        .fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (ruledPageDecoration) {
                                    Modifier.mushafPageOuterFrameBorder(
                                        color = colorScheme.outline.alpha(0.5f),
                                        strokeWidth = 1.dp,
                                    )
                                } else {
                                    Modifier
                                }
                            ),
                    ) {
                        Column(Modifier.fillMaxWidth()) {
                            item.lines.forEachIndexed { index, line ->
                                key(line.lineNo) {
                                    val showLineRuleBelow =
                                        ruledPageDecoration && index < item.lines.lastIndex
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .then(
                                                if (showLineRuleBelow) {
                                                    Modifier.mushafHorizontalRuleBelow(
                                                        color = colorScheme.outline.alpha(0.5f),
                                                        strokeWidth = 1.dp,
                                                    )
                                                } else {
                                                    Modifier
                                                }
                                            ),
                                    ) {
                                        if (ruledPageDecoration) {
                                            val hPadding = if (line is QuranPageLineItem.Title) 0.dp
                                            else MUSHAF_PAGE_HORIZONTAL_PADDING

                                            Column(
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(
                                                        start = hPadding,
                                                        end = hPadding,
                                                    ),
                                            ) {
                                                MushafLineContent(
                                                    line = line,
                                                    playingWordKeys = playingWordKeys,
                                                    controller = playerState.controller,
                                                    ruledPageDecoration
                                                )
                                            }
                                        } else {
                                            MushafLineContent(
                                                line = line,
                                                playingWordKeys = playingWordKeys,
                                                controller = playerState.controller,
                                                ruledPageDecoration = ruledPageDecoration,
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
    }
}

@Composable
private fun MushafLineContent(
    line: QuranPageLineItem,
    playingWordKeys: Set<Pair<Int, Int>>,
    controller: RecitationController,
    ruledPageDecoration: Boolean,
) {
    when (line) {
        is QuranPageLineItem.Title -> ChapterTitle(line.chapterNo, ruledPageDecoration)
        is QuranPageLineItem.Bismillah -> Bismillah()
        is QuranPageLineItem.Text -> MushafLineText(
            textLine = line,
            layout = line.layout,
            playingWordKeys = playingWordKeys,
            controller = controller,
        )
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
//    val context = LocalContext.current

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
        /*.clickable {
            if (word.isLastWordOfAyah) {
                MessageUtils.showRemovableToast(context, "LAST WORD", Toast.LENGTH_LONG)
            } else {
                MessageUtils.showRemovableToast(context, word.text, Toast.LENGTH_LONG)
            }
        },*/
    )
}

private fun Modifier.mushafHorizontalRuleBelow(
    color: Color,
    strokeWidth: Dp,
): Modifier = drawBehind {
    val h = strokeWidth.toPx()
    val y = size.height - h / 2f
    drawLine(
        color = color,
        start = Offset(0f, y),
        end = Offset(size.width, y),
        strokeWidth = h,
    )
}

private fun Modifier.mushafPageOuterFrameBorder(
    color: Color,
    strokeWidth: Dp,
): Modifier = drawBehind {
    val w = strokeWidth.toPx()
    val half = w / 2f
    drawRect(
        color = color,
        topLeft = Offset(half, half),
        size = Size(size.width - w, size.height - w),
        style = Stroke(width = w),
    )
}