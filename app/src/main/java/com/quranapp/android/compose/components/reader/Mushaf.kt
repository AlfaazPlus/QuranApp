package com.quranapp.android.compose.components.reader

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
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
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
import com.quranapp.android.compose.components.reader.dialogs.WbwSheetData
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.db.entities.quran.AyahWordEntity
import com.quranapp.android.utils.quran.QuranUtils
import com.quranapp.android.utils.reader.MUSHAF_PAGE_HORIZONTAL_PADDING
import com.quranapp.android.utils.reader.PageBuilderParams
import com.quranapp.android.utils.reader.mushafShowsRuledPageDecoration
import com.quranapp.android.viewModels.ReaderViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart
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
    val cacheKey: String,
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
    val mushafSession by readerVm.mushafSession.collectAsState()
    val pageItems by readerVm.pageItems.collectAsState()

    val sessionLayout = mushafSession.layout
    val pageCount = mushafSession.pageCount
    val ruledPageDecoration = mushafSession.layout.scriptCode.mushafShowsRuledPageDecoration()

    val context = LocalContext.current
    val pagerState = rememberPagerState(
        initialPage = mushafSession.currentPageNo?.let { it - 1 } ?: 0,
        pageCount = { pageCount },
    )

    val textMeasurer = rememberTextMeasurer(cacheSize = 2048)
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val density = LocalDensity.current

    val pageBuilderParams = remember(colors, typography, textMeasurer, density, contentWidth) {
        PageBuilderParams(
            context = context,
            colors = colors,
            type = typography,
            textMeasurer = textMeasurer,
            density = density,
            contentWidthPx = with(density) {
                (contentWidth - MUSHAF_PAGE_HORIZONTAL_PADDING * 2).roundToPx()
            }
        )
    }

    LaunchedEffect(pagerState, pageBuilderParams, mushafSession.version) {
        snapshotFlow {
            listOf(
                pagerState.currentPage + 1,
                pagerState.targetPage + 1,
                pagerState.settledPage + 1,
            )
        }
            .onStart {
                emit(
                    listOf(mushafSession.currentPageNo ?: 1)
                )
            }
            .distinctUntilChanged()
            .collect { anchorPages ->
                readerVm.fetchMushafPages(
                    context, anchorPages, pageBuilderParams
                )
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

                readerVm.updateCurrentPageNo(currentPageNo)

                if (pageCount > 0) {
                    readerVm.updateLastKnownVerseFromPage(currentPageNo)
                }
            }
    }

    val navigateToVerse by readerVm.navigateToVerse.collectAsStateWithLifecycle()

    LaunchedEffect(navigateToPage, pageCount) {
        val targetPage = navigateToPage ?: return@LaunchedEffect

        if (pageCount <= 0) return@LaunchedEffect

        val clamped = targetPage.coerceIn(1, pageCount)

        try {
            pagerState.scrollToPage(clamped - 1)
            readerVm.updateCurrentPageNo(clamped)

        } finally {
            readerVm.consumePageNavigation()
        }
    }

    LaunchedEffect(navigateToVerse, pageCount) {
        val targetVerse = navigateToVerse ?: return@LaunchedEffect
        if (pageCount <= 0) return@LaunchedEffect

        val targetPage = readerVm.resolvePageNo(targetVerse.chapterNo, targetVerse.verseNo)
            ?: run {
                readerVm.consumeVerseNavigation()
                return@LaunchedEffect
            }

        readerVm.consumeVerseNavigation()
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
        ) {
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
            ) { page ->
                val pageNo = page + 1
                val item = pageItems[pageNo]

                key(sessionLayout, page) {
                    PageModePage(
                        item = item,
                        contentWidth,
                        ruledPageDecoration,
                        nestedScrollConnection,
                    )
                }
            }

        }
    }
}

@Composable
private fun PageModePage(
    item: QuranPageItem?,
    contentWidth: Dp,
    ruledPageDecoration: Boolean,
    nestedScrollConnection: NestedScrollConnection,
) {
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

    TextStyleProvider(emptyMap()) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = contentWidth)
                    .verticalFadingEdge(scrollState, color = colorScheme.surface, length = 24.dp),
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
                                                val hPadding =
                                                    if (line is QuranPageLineItem.Title) 0.dp
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
                                                        ruledPageDecoration = ruledPageDecoration,
                                                    )
                                                }
                                            } else {
                                                MushafLineContent(
                                                    line = line,
                                                    playingWordKeys = playingWordKeys,
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
}

@Composable
private fun MushafLineContent(
    line: QuranPageLineItem,
    playingWordKeys: Set<Pair<Int, Int>>,
    ruledPageDecoration: Boolean,
) {
    when (line) {
        is QuranPageLineItem.Title -> ChapterTitle(line.chapterNo, ruledPageDecoration)
        is QuranPageLineItem.Bismillah -> Bismillah()
        is QuranPageLineItem.Text -> MushafLineText(
            textLine = line,
            layout = line.layout,
            playingWordKeys = playingWordKeys,
        )
    }
}

@Composable
private fun MushafLineText(
    textLine: QuranPageLineItem.Text,
    layout: MushafLineLayout,
    playingWordKeys: Set<Pair<Int, Int>>,
) {
    val words = textLine.words
    val fittedStyle = layout.fittedStyle
    val centeredGap = layout.centeredGap

    if (textLine.centered) {
        Box(Modifier.fillMaxWidth()) {
            MushafWordsRow(
                words = words,
                fittedStyle = fittedStyle,
                playingWordKeys = playingWordKeys,
                horizontalArrangement = Arrangement.spacedBy(
                    centeredGap, Alignment.CenterHorizontally
                ),
                modifier = Modifier.align(Alignment.Center),
            )
        }
    } else {
        MushafWordsRow(
            words = words,
            fittedStyle = fittedStyle,
            playingWordKeys = playingWordKeys,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MushafWordsRow(
    words: List<AyahWordEntity>,
    fittedStyle: TextStyle,
    playingWordKeys: Set<Pair<Int, Int>>,
    horizontalArrangement: Arrangement.Horizontal,
    modifier: Modifier = Modifier,
) {
    val wordRects = remember(words) {
        mutableStateListOf<Rect?>().apply { repeat(words.size) { add(null) } }
    }

    val highlightRects = mergedMushafHighlightRects(words, wordRects, playingWordKeys)
    val highlightColor = colorScheme.primary.alpha(0.3f)

    val wbwState = LocalWbwState.current
    val quranTextStyles = LocalQuranTextStyle.current
    val shouldShowTooltip = !wbwState.isWbwSheetOpen

    Row(
        modifier = modifier.drawBehind {
            for (rect in highlightRects) {
                drawRoundRect(
                    color = highlightColor,
                    topLeft = Offset(rect.left, rect.top),
                    size = Size(rect.width, rect.height),
                )
            }
        },
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for ((index, word) in words.withIndex()) {
            val positionModifier = Modifier.onGloballyPositioned { coordinates ->
                val pos = coordinates.positionInParent()
                val sz = coordinates.size
                wordRects[index] = Rect(
                    pos.x,
                    pos.y,
                    pos.x + sz.width,
                    pos.y + sz.height,
                )
            }

            if (wbwState.activeTooltipWord == word && shouldShowTooltip) {
                WbwTooltip(
                    word = word,
                    onDismiss = { wbwState.onDismissTooltip() },
                    onOpenSheet = {
                        val pair = QuranUtils.getVerseNoFromAyahId(word.ayahId)

                        wbwState.toggleWbwSheet(
                            WbwSheetData(
                                chapterNo = pair.first,
                                verseNo = pair.second,
                                wordIndex = word.wordIndex,
                            )
                        )
                    },
                    textStyles = quranTextStyles,
                ) {
                    Word(
                        active = true,
                        word = word,
                        fittedStyle = fittedStyle,
                        onClick = { wbwState.onWordClick(word) },
                        modifier = positionModifier,
                    )
                }
            } else {
                Word(
                    active = wbwState.activeTooltipWord == word,
                    word = word,
                    fittedStyle = fittedStyle,
                    onClick = { wbwState.onWordClick(word) },
                    modifier = positionModifier,
                )
            }
        }
    }
}

@Composable
private fun Word(
    active: Boolean,
    word: AyahWordEntity,
    fittedStyle: TextStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = word.text,
        color = colorScheme.onBackground,
        style = fittedStyle,
        maxLines = 1,
        softWrap = false,
        modifier = modifier
            .background(
                if (active) colorScheme.primary.alpha(0.3f) else Color.Transparent,
                shape = shapes.small
            )
            .clickable { onClick() }
    )
}

private fun mergedMushafHighlightRects(
    words: List<AyahWordEntity>,
    wordRects: List<Rect?>,
    playingWordKeys: Set<Pair<Int, Int>>,
): List<Rect> {
    if (words.isEmpty()) return emptyList()

    val result = mutableListOf<Rect>()
    var i = 0

    while (i < words.size) {
        val w = words[i]
        val highlighted = (w.ayahId to w.wordIndex) in playingWordKeys

        if (!highlighted) {
            i++
            continue
        }

        var rect = wordRects.getOrNull(i) ?: run {
            i++
            continue
        }

        val ayahId = w.ayahId

        var j = i + 1

        while (j < words.size) {
            val w2 = words[j]
            if ((w2.ayahId to w2.wordIndex) !in playingWordKeys || w2.ayahId != ayahId) {
                break
            }
            val r2 = wordRects.getOrNull(j) ?: break
            rect = Rect(
                minOf(rect.left, r2.left),
                minOf(rect.top, r2.top),
                maxOf(rect.right, r2.right),
                maxOf(rect.bottom, r2.bottom),
            )
            j++
        }

        result.add(rect)

        i = j
    }

    return result
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