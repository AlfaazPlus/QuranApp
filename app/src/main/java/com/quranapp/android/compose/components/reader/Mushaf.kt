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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
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
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.compose.utils.ThemeUtils
import com.quranapp.android.db.entities.quran.AyahWordEntity
import com.quranapp.android.utils.quran.QuranMeta
import com.quranapp.android.utils.reader.ComposeUiConfig
import com.quranapp.android.utils.reader.MUSHAF_PAGE_HORIZONTAL_PADDING
import com.quranapp.android.utils.reader.PageBuilderParams
import com.quranapp.android.utils.reader.atlas.AtlasGlyphPlacement
import com.quranapp.android.utils.reader.atlas.getForWord
import com.quranapp.android.utils.reader.mushafShowsRuledPageDecoration
import com.quranapp.android.viewModels.ReaderViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import verticalFadingEdge


data class MushafLineLayout(
    val fittedStyle: TextStyle,
    val centeredGap: Dp,
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
        val atlasPlacements: Map<Int, List<AtlasGlyphPlacement>>,
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
    val contentWidthPx: Int,
    val density: Float,
    val fontScale: Float,
    val styleHash: Int,
)


@Composable
fun ReaderLayoutPageMode(
    readerVm: ReaderViewModel,
    contentWidth: Dp,
    nestedScrollConnection: NestedScrollConnection?,
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
    val colors by rememberUpdatedState(MaterialTheme.colorScheme)
    val typography by rememberUpdatedState(MaterialTheme.typography)
    val density = LocalDensity.current
    val isDark = ThemeUtils.observeDarkTheme()

    val pageBuilderParams =
        remember(colors, typography, textMeasurer, density, contentWidth, isDark) {
            PageBuilderParams(
                uiConfig = ComposeUiConfig(
                    context = context,
                    colors = colors,
                    type = typography,
                    textMeasurer = textMeasurer,
                    density = density,
                ),
                contentWidthPx = with(density) {
                    (contentWidth - MUSHAF_PAGE_HORIZONTAL_PADDING * 2).roundToPx()
                },
                isDark = isDark,
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
    val navigateToVerse by readerVm.navigateToVerse.collectAsStateWithLifecycle()
    val latestNavigateToVerse by rememberUpdatedState(navigateToVerse)

    LaunchedEffect(context, pagerState, pageCount, navigateToPage) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { currentPage ->
                // While a programmatic scroll is pending, the pager may still report page 1 until
                // [pageCount] is ready; do not overwrite [currentPageNo] from the initial open.
                if (navigateToPage != null || latestNavigateToVerse != null) return@collect

                val currentPageNo = currentPage + 1

                readerVm.updateCurrentPageNo(currentPageNo)

                if (pageCount > 0) {
                    readerVm.updateLastKnownVerseFromPage(currentPageNo)
                }
            }
    }

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

    var verseTarget by remember { mutableStateOf<Pair<ChapterVersePair, Int>?>(null) }
    val verseAnchor = remember { VerseScrollAnchor() }

    LaunchedEffect(navigateToVerse, pageCount, sessionLayout, mushafSession.version) {
        val targetVerse = navigateToVerse ?: return@LaunchedEffect
        if (pageCount <= 0) return@LaunchedEffect
        verseTarget = null

        val targetPage = readerVm.resolvePageNo(targetVerse.chapterNo, targetVerse.verseNo)
            ?: run {
                readerVm.consumeVerseNavigation()
                return@LaunchedEffect
            }

        verseAnchor.beginTarget()

        val clampedPage = targetPage.coerceIn(1, pageCount)

        if (pagerState.settledPage != clampedPage - 1) {
            pagerState.scrollToPage(clampedPage - 1)
        }

        readerVm.updateCurrentPageNo(clampedPage)

        val pageItem = snapshotFlow { pageItems[clampedPage] }
            .first { it != null } ?: return@LaunchedEffect
        val hasTargetVerse = pageItem.lines.any { line ->
            line is QuranPageLineItem.Text && line.words.any { it.ayahId == targetVerse.ayahId }
        }

        if (!hasTargetVerse) {
            readerVm.consumeVerseNavigation()
            return@LaunchedEffect
        }

        verseTarget = targetVerse to clampedPage
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

        readerVm.requestVerseNavigation(
            playingVerseMushaf.chapterNo,
            playingVerseMushaf.verseNo,
        )
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
                        contentWidth = contentWidth,
                        ruledPageDecoration = ruledPageDecoration,
                        nestedScrollConnection = nestedScrollConnection,
                        targetVerse = verseTarget?.takeIf { it.second == pageNo }?.first,
                        verseAnchor = verseAnchor,
                        onVersePositioned = { verse ->
                            if (verseTarget?.first == verse) {
                                readerVm.consumeVerseNavigation()
                                verseTarget = null
                            }
                        },
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
    nestedScrollConnection: NestedScrollConnection?,
    targetVerse: ChapterVersePair?,
    verseAnchor: VerseScrollAnchor,
    onVersePositioned: (ChapterVersePair) -> Unit,
) {
    if (item == null) {
        return Loader(true)
    }

    val scrollState = rememberScrollState()

    val playerState = LocalRecitation.current
    val isPlaying = playerState.isAnyPlaying
    val playingVerse = playerState.playingVerse
    val playingWordIndex = if (isPlaying) playerState.playingWordIndex else -1
    val targetWordIndex = targetVerse?.let { item.firstWordIndexForAyah(it.ayahId) }

    LaunchedEffect(targetVerse, item.pageNo) {
        val verse = targetVerse ?: return@LaunchedEffect
        scrollState.scrollToVersePosition(verseAnchor)
        onVersePositioned(verse)
    }

    val playingAyahId = remember(isPlaying, playingVerse) {
        if (!isPlaying || !playingVerse.isValid) -1
        else QuranMeta.getAyahId(playingVerse.chapterNo, playingVerse.verseNo)
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
                    .then(
                        if (targetVerse == null) Modifier
                        else Modifier.onGloballyPositioned { coordinates ->
                            val top = coordinates.positionInRoot().y.toInt()
                            verseAnchor.viewportTopPx = top
                            verseAnchor.viewportBottomPx = top + coordinates.size.height
                        }
                    )
                    .verticalFadingEdge(scrollState, color = colorScheme.surface, length = 24.dp),
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Column(
                        Modifier
                            .verticalScroll(scrollState)
                            .padding(top = 16.dp, bottom = 128.dp)
                            .then(
                                if (nestedScrollConnection == null) Modifier
                                else Modifier.nestedScroll(nestedScrollConnection)
                            )
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
                                                        playingAyahId = playingAyahId,
                                                        playingWordIndex = playingWordIndex,
                                                        ruledPageDecoration = ruledPageDecoration,
                                                        targetVerse = targetVerse,
                                                        targetWordIndex = targetWordIndex,
                                                        verseAnchor = verseAnchor,
                                                    )
                                                }
                                            } else {
                                                MushafLineContent(
                                                    line = line,
                                                    playingAyahId = playingAyahId,
                                                    playingWordIndex = playingWordIndex,
                                                    ruledPageDecoration = ruledPageDecoration,
                                                    targetVerse = targetVerse,
                                                    targetWordIndex = targetWordIndex,
                                                    verseAnchor = verseAnchor,
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

private fun QuranPageItem.firstWordIndexForAyah(ayahId: Int): Int? {
    for (line in lines) {
        if (line is QuranPageLineItem.Text) {
            for (word in line.words) {
                if (word.ayahId == ayahId) return word.wordIndex
            }
        }
    }

    return null
}

@Composable
private fun MushafLineContent(
    line: QuranPageLineItem,
    playingAyahId: Int,
    playingWordIndex: Int,
    ruledPageDecoration: Boolean,
    targetVerse: ChapterVersePair?,
    targetWordIndex: Int?,
    verseAnchor: VerseScrollAnchor,
) {
    when (line) {
        is QuranPageLineItem.Title -> ChapterTitle(line.chapterNo, ruledPageDecoration)
        is QuranPageLineItem.Bismillah -> Bismillah()
        is QuranPageLineItem.Text -> MushafLineText(
            textLine = line,
            layout = line.layout,
            playingAyahId = playingAyahId,
            playingWordIndex = playingWordIndex,
            targetVerse = targetVerse,
            targetWordIndex = targetWordIndex,
            verseAnchor = verseAnchor,
        )
    }
}

@Composable
private fun MushafLineText(
    textLine: QuranPageLineItem.Text,
    layout: MushafLineLayout,
    playingAyahId: Int,
    playingWordIndex: Int,
    targetVerse: ChapterVersePair?,
    targetWordIndex: Int?,
    verseAnchor: VerseScrollAnchor,
) {
    val fittedStyle = layout.fittedStyle
    val centeredGap = layout.centeredGap

    if (textLine.centered) {
        Box(Modifier.fillMaxWidth()) {
            MushafWordsRow(
                textLine = textLine,
                fittedStyle = fittedStyle,
                playingAyahId = playingAyahId,
                playingWordIndex = playingWordIndex,
                targetVerse = targetVerse,
                targetWordIndex = targetWordIndex,
                verseAnchor = verseAnchor,
                horizontalArrangement = Arrangement.spacedBy(
                    centeredGap, Alignment.CenterHorizontally
                ),
                modifier = Modifier.align(Alignment.Center),
            )
        }
    } else {
        MushafWordsRow(
            textLine = textLine,
            fittedStyle = fittedStyle,
            playingAyahId = playingAyahId,
            playingWordIndex = playingWordIndex,
            targetVerse = targetVerse,
            targetWordIndex = targetWordIndex,
            verseAnchor = verseAnchor,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MushafWordsRow(
    textLine: QuranPageLineItem.Text,
    fittedStyle: TextStyle,
    playingAyahId: Int,
    playingWordIndex: Int,
    targetVerse: ChapterVersePair?,
    targetWordIndex: Int?,
    verseAnchor: VerseScrollAnchor,
    horizontalArrangement: Arrangement.Horizontal,
    modifier: Modifier = Modifier,
) {
    val words = textLine.words
    val targetAyahId = targetVerse?.ayahId
    val bounds = remember(words) { RecitationWordBounds(words.size) }
    val trackPositions = playingAyahId >= 0

    val positionModifiers = remember(bounds, trackPositions) {
        Array(bounds.wordCount) { index ->
            if (!trackPositions) {
                Modifier
            } else {
                Modifier.onGloballyPositioned { coordinates ->
                    val pos = coordinates.positionInParent()
                    val sz = coordinates.size
                    bounds.update(
                        index = index,
                        left = pos.x,
                        top = pos.y,
                        right = pos.x + sz.width,
                        bottom = pos.y + sz.height,
                    )
                }
            }
        }
    }

    val verseHighlightColor = colorScheme.primary.alpha(0.2f)
    val wordHighlightColor = colorScheme.primary.alpha(0.3f)
    val highlightCorner = with(LocalDensity.current) {
        CornerRadius(8.dp.toPx(), 8.dp.toPx())
    }

    val wbwState = LocalWbwState.current
    val quranTextStyles = LocalQuranTextStyle.current
    val shouldShowTooltip = !wbwState.isWbwSheetOpen

    Row(
        modifier = modifier.drawBehind {
            drawRecitationHighlights(
                bounds = bounds,
                words = words,
                ayahId = playingAyahId,
                wordIndex = playingWordIndex,
                splitRows = false,
                verseColor = verseHighlightColor,
                wordColor = wordHighlightColor,
                corner = highlightCorner,
            )
        },
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for ((index, word) in words.withIndex()) {
            val targetWordModifier = if (
                targetVerse != null &&
                word.ayahId == targetAyahId &&
                (word.wordIndex == targetWordIndex || word.isLastWordOfAyah)
            ) {
                Modifier.onGloballyPositioned { coordinates ->
                    val top = coordinates.positionInRoot().y.toInt()
                    if (word.wordIndex == targetWordIndex) {
                        verseAnchor.targetTopPx = top
                    }
                    if (word.isLastWordOfAyah) {
                        verseAnchor.targetBottomPx = top + coordinates.size.height
                    }
                }
            } else {
                Modifier
            }
            val wordModifier = positionModifiers[index].then(targetWordModifier)

            if (wbwState.activeTooltipWord == word && shouldShowTooltip) {
                WbwTooltip(
                    word = word,
                    onDismiss = { wbwState.onDismissTooltip() },
                    onOpenSheet = {
                        val pair = QuranMeta.getVerseNoFromAyahId(word.ayahId)

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
                        atlasPlacements = textLine.atlasPlacements.getForWord(word),
                        fittedStyle = fittedStyle,
                        onClick = { wbwState.onWordClick(word) },
                        modifier = wordModifier,
                    )
                }
            } else {
                Word(
                    active = wbwState.activeTooltipWord == word,
                    word = word,
                    atlasPlacements = textLine.atlasPlacements.getForWord(word),
                    fittedStyle = fittedStyle,
                    onClick = { wbwState.onWordClick(word) },
                    modifier = wordModifier,
                )
            }
        }
    }
}

@Composable
private fun Word(
    active: Boolean,
    word: AyahWordEntity,
    atlasPlacements: List<AtlasGlyphPlacement>?,
    fittedStyle: TextStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    QuranWordText(
        modifier = modifier
            .background(
                if (active) colorScheme.primary.alpha(0.3f) else Color.Transparent,
                shape = shapes.small
            )
            .clickable { onClick() }
            .padding(vertical = 1.dp),
        word = word,
        atlasPlacements = atlasPlacements,
        style = fittedStyle,
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
