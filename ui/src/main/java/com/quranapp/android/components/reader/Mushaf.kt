package com.quranapp.android.components.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import com.quranapp.android.components.common.Loader
import com.quranapp.android.components.reader.dialogs.WbwSheetData
import com.quranapp.android.db.entities.quran.AyahWordEntity
import com.quranapp.android.extensions.verticalFadingEdge
import com.quranapp.android.reader.MushafLineLayout
import com.quranapp.android.reader.QuranPageItem
import com.quranapp.android.reader.QuranPageLineItem
import com.quranapp.android.theme.ThemeUtils
import com.quranapp.android.theme.alpha
import com.quranapp.android.utils.quran.QuranMeta
import com.quranapp.android.utils.reader.ComposeUiConfig
import com.quranapp.android.utils.reader.MUSHAF_PAGE_HORIZONTAL_PADDING
import com.quranapp.android.utils.reader.PageBuilderParams
import com.quranapp.android.utils.reader.atlas.AtlasGlyphPlacement
import com.quranapp.android.utils.reader.atlas.getForWord
import com.quranapp.android.utils.reader.mushafShowsRuledPageDecoration
import com.quranapp.android.viewmodel.ReaderViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart

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

    val navigateToPageState = readerVm.navigateToPage.collectAsStateWithLifecycle()
    val navigateToPage by navigateToPageState
    val navigateToVerseState = readerVm.navigateToVerse.collectAsStateWithLifecycle()
    val navigateToVerse by navigateToVerseState
    val pendingVisibleVerseSyncPage = remember { mutableStateOf<Int?>(null) }
    val isNavigating = remember(navigateToPageState, navigateToVerseState) {
        { navigateToPageState.value != null || navigateToVerseState.value != null }
    }

    LaunchedEffect(pagerState) {
        var lastSettledPage: Int? = null
        var sawScroll = false

        snapshotFlow {
            (pagerState.settledPage shl 1) or (if (pagerState.isScrollInProgress) 1 else 0)
        }
            .distinctUntilChanged()
            .collect { packed ->
                val scrolling = (packed and 1) != 0
                val settledPage = packed shr 1

                // While a programmatic scroll is pending, the pager may still report page 1 until
                // [pageCount] is ready; do not overwrite [currentPageNo] from the initial open.
                if (scrolling) {
                    sawScroll = true
                    return@collect
                }

                val pageChanged = lastSettledPage != null && lastSettledPage != settledPage
                lastSettledPage = settledPage
                if (!sawScroll && !pageChanged) return@collect
                sawScroll = false

                if (isNavigating()) return@collect

                val currentPageNo = settledPage + 1

                readerVm.updateCurrentPageNo(currentPageNo)
                pendingVisibleVerseSyncPage.value = currentPageNo
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
            readerVm.consumePageNavigation(targetPage)
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
                readerVm.consumeVerseNavigation(targetVerse)
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
            readerVm.consumeVerseNavigation(targetVerse)
            return@LaunchedEffect
        }

        verseTarget = targetVerse to clampedPage
    }

    val playerState = LocalRecitation.current
    var playerVerseSync by readerVm.playerVerseSync

    LaunchedEffect(playerVerseSync, pageCount, playerState) {
        if (!playerVerseSync || pageCount <= 0) {
            return@LaunchedEffect
        }

        snapshotFlow {
            playerState.isAnyPlayingState.value to playerState.playingVerseState.value
        }
            .distinctUntilChanged()
            .collectLatest { (isPlayingMushaf, playingVerseMushaf) ->
                if (!isPlayingMushaf || !playingVerseMushaf.isValid) {
                    return@collectLatest
                }

                val targetPage =
                    readerVm.resolvePageNo(playingVerseMushaf.chapterNo, playingVerseMushaf.verseNo)
                        ?: return@collectLatest
                if (targetPage !in 1..pageCount) return@collectLatest

                readerVm.requestVerseNavigation(
                    playingVerseMushaf.chapterNo,
                    playingVerseMushaf.verseNo,
                    highlightAfterNavigation = false,
                )
            }
    }

    LaunchedEffect(pageCount, playerState) {
        if (pageCount <= 0) {
            onSyncStateChanged(false)
            return@LaunchedEffect
        }

        snapshotFlow { playerState.playingVerseState.value }
            .distinctUntilChanged()
            .collectLatest { playingVerseMushaf ->
                if (!playingVerseMushaf.isValid) {
                    onSyncStateChanged(false)
                    return@collectLatest
                }

                val expectedPage =
                    readerVm.resolvePageNo(playingVerseMushaf.chapterNo, playingVerseMushaf.verseNo)

                if (expectedPage == null || expectedPage !in 1..pageCount) {
                    onSyncStateChanged(false)
                    return@collectLatest
                }

                snapshotFlow { pagerState.settledPage + 1 }
                    .distinctUntilChanged()
                    .collect { current ->
                        onSyncStateChanged(current == expectedPage)
                    }
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
                        trackVisibleVerses = page == pagerState.settledPage,
                        isNavigating = isNavigating,
                        pendingVisibleVerseSyncPage = pendingVisibleVerseSyncPage,
                        onVisibleVerse = readerVm::updateLastKnownVerse,
                        onVersePositioned = { verse ->
                            if (verseTarget?.first == verse) {
                                readerVm.completeVerseNavigation(verse)
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
    trackVisibleVerses: Boolean,
    isNavigating: () -> Boolean,
    pendingVisibleVerseSyncPage: MutableState<Int?>,
    onVisibleVerse: (ChapterVersePair) -> Unit,
    onVersePositioned: (ChapterVersePair) -> Unit,
) {
    if (item == null) {
        return Loader(true)
    }

    val scrollState = rememberScrollState()

    val playerState = LocalRecitation.current
    val playingWordIndexState = playerState.playingWordIndexState
    val targetWordIndex = targetVerse?.let { item.firstWordIndexForAyah(it.ayahId) }
    val pageAyahIds = remember(item) {
        item.lines.asSequence()
            .filterIsInstance<QuranPageLineItem.Text>()
            .flatMap { it.words.asSequence() }
            .map { it.ayahId }
            .distinct()
            .toList()
            .toIntArray()
    }
    val visibilityTracker = remember(item, pageAyahIds) {
        VerseVisibilityTracker(pageAyahIds)
    }

    LaunchedEffect(scrollState, trackVisibleVerses, visibilityTracker, item.pageNo) {
        if (!trackVisibleVerses) return@LaunchedEffect

        scrollState.updateVisibleVerseWhenIdle(
            tracker = visibilityTracker,
            viewport = verseAnchor,
            isNavigating = isNavigating,
            shouldSyncOnSettle = { pendingVisibleVerseSyncPage.value == item.pageNo },
            onSyncedOnSettle = {
                if (pendingVisibleVerseSyncPage.value == item.pageNo) {
                    pendingVisibleVerseSyncPage.value = null
                }
            },
            onVerseVisible = onVisibleVerse,
        )
    }

    LaunchedEffect(targetVerse, item.pageNo) {
        val verse = targetVerse ?: return@LaunchedEffect
        scrollState.scrollToVersePosition(verseAnchor)
        onVersePositioned(verse)
    }

    val playingAyahId by remember(pageAyahIds, playerState) {
        derivedStateOf {
            if (!playerState.isAnyPlayingState.value) return@derivedStateOf -1

            val playingVerse = playerState.playingVerseState.value

            if (!playingVerse.isValid) return@derivedStateOf -1

            val ayahId = QuranMeta.getAyahId(playingVerse.chapterNo, playingVerse.verseNo)

            if (pageAyahIds.contains(ayahId)) ayahId else -1
        }
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
                        if (!trackVisibleVerses && targetVerse == null) Modifier
                        else Modifier.onGloballyPositioned { coordinates ->
                            val top = coordinates.positionInRoot().y.toInt()
                            verseAnchor.viewportTopPx = top
                            verseAnchor.viewportBottomPx = top + coordinates.size.height
                        }
                    )
                    .verticalFadingEdge(scrollState, length = 24.dp),
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
                                                        playingWordIndexState = playingWordIndexState,
                                                        ruledPageDecoration = true,
                                                        targetVerse = targetVerse,
                                                        targetWordIndex = targetWordIndex,
                                                        verseAnchor = verseAnchor,
                                                        visibilityTracker = visibilityTracker.takeIf { trackVisibleVerses },
                                                    )
                                                }
                                            } else {
                                                MushafLineContent(
                                                    line = line,
                                                    playingAyahId = playingAyahId,
                                                    playingWordIndexState = playingWordIndexState,
                                                    ruledPageDecoration = false,
                                                    targetVerse = targetVerse,
                                                    targetWordIndex = targetWordIndex,
                                                    verseAnchor = verseAnchor,
                                                    visibilityTracker = visibilityTracker.takeIf { trackVisibleVerses },
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
    playingWordIndexState: State<Int>,
    ruledPageDecoration: Boolean,
    targetVerse: ChapterVersePair?,
    targetWordIndex: Int?,
    verseAnchor: VerseScrollAnchor,
    visibilityTracker: VerseVisibilityTracker?,
) {
    when (line) {
        is QuranPageLineItem.Title -> ChapterTitle(line.chapterNo, ruledPageDecoration)
        is QuranPageLineItem.Bismillah -> Bismillah()
        is QuranPageLineItem.Text -> MushafLineText(
            textLine = line,
            layout = line.layout,
            playingAyahId = playingAyahId,
            playingWordIndexState = playingWordIndexState,
            targetVerse = targetVerse,
            targetWordIndex = targetWordIndex,
            verseAnchor = verseAnchor,
            visibilityTracker = visibilityTracker,
        )
    }
}

@Composable
private fun MushafLineText(
    textLine: QuranPageLineItem.Text,
    layout: MushafLineLayout,
    playingAyahId: Int,
    playingWordIndexState: State<Int>,
    targetVerse: ChapterVersePair?,
    targetWordIndex: Int?,
    verseAnchor: VerseScrollAnchor,
    visibilityTracker: VerseVisibilityTracker?,
) {
    val fittedStyle = layout.fittedStyle
    val centeredGap = layout.centeredGap

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val fullLineContainerWidthPx = with(LocalDensity.current) {
            maxWidth.toPx() + MUSHAF_PAGE_HORIZONTAL_PADDING.toPx() * 2
        }

        if (textLine.centered) {
            MushafWordsRow(
                textLine = textLine,
                fittedStyle = fittedStyle,
                playingAyahId = playingAyahId,
                playingWordIndexState = playingWordIndexState,
                targetVerse = targetVerse,
                targetWordIndex = targetWordIndex,
                verseAnchor = verseAnchor,
                visibilityTracker = visibilityTracker,
                horizontalArrangement = Arrangement.spacedBy(
                    centeredGap, Alignment.CenterHorizontally
                ),
                fullLineContainerWidthPx = fullLineContainerWidthPx,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            MushafWordsRow(
                textLine = textLine,
                fittedStyle = fittedStyle,
                playingAyahId = playingAyahId,
                playingWordIndexState = playingWordIndexState,
                targetVerse = targetVerse,
                targetWordIndex = targetWordIndex,
                verseAnchor = verseAnchor,
                visibilityTracker = visibilityTracker,
                horizontalArrangement = Arrangement.SpaceBetween,
                fullLineContainerWidthPx = fullLineContainerWidthPx,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun MushafWordsRow(
    textLine: QuranPageLineItem.Text,
    fittedStyle: TextStyle,
    playingAyahId: Int,
    playingWordIndexState: State<Int>,
    targetVerse: ChapterVersePair?,
    targetWordIndex: Int?,
    verseAnchor: VerseScrollAnchor,
    visibilityTracker: VerseVisibilityTracker?,
    horizontalArrangement: Arrangement.Horizontal,
    fullLineContainerWidthPx: Float,
    modifier: Modifier = Modifier,
) {
    val words = textLine.words
    val targetAyahId = targetVerse?.ayahId

    val highlightState = LocalTransientVerseHighlight.current
    val navigationAyahId = highlightState.verse?.ayahId
        ?.takeIf { ayahId -> words.any { it.ayahId == ayahId } }
    val highlightAlpha = highlightState.alpha
    val bounds = remember(words) { RecitationWordBounds(words.size) }

    val positionModifiers = remember(bounds, navigationAyahId, playingAyahId) {
        Array(bounds.wordCount) { index ->
            if (words[index].ayahId != navigationAyahId && words[index].ayahId != playingAyahId) {
                Modifier
            } else {
                Modifier.onGloballyPositioned { coordinates ->
                    val pos = coordinates.positionInParent()
                    val sz = coordinates.size

                    bounds.update(
                        index = index,
                        left = pos.x - 2,
                        top = pos.y + 4,
                        right = pos.x + sz.width + 2,
                        bottom = pos.y + sz.height - 4,
                    )
                }
            }
        }
    }

    val verseHighlightColor = colorScheme.primary.alpha(VERSE_HIGHLIGHT_ALPHA)
    val wordHighlightColor = colorScheme.primary.alpha(VERSE_WORD_HIGHLIGHT_ALPHA)
    val highlightCorner = with(LocalDensity.current) {
        CornerRadius(8.dp.toPx(), 8.dp.toPx())
    }
    val wbwState = LocalWbwState.current
    val quranTextStyles = LocalQuranTextStyle.current
    val shouldShowTooltip = !wbwState.isWbwSheetOpen

    Row(
        modifier = modifier
            .then(
                if (visibilityTracker == null) Modifier
                else Modifier.onGloballyPositioned { coordinates ->
                    val top = coordinates.positionInRoot().y.toInt()
                    visibilityTracker.recordRow(words, top, top + coordinates.size.height)
                }
            )
            .drawBehind {
                val primaryAyahId = navigationAyahId ?: playingAyahId

                if (primaryAyahId >= 0) {
                    drawRecitationHighlights(
                        bounds = bounds,
                        words = words,
                        ayahId = primaryAyahId,
                        wordIndex = if (navigationAyahId == null || navigationAyahId == playingAyahId) {
                            playingWordIndexState.value
                        } else {
                            -1
                        },
                        splitRows = false,
                        verseColor = if (navigationAyahId != null) {
                            verseHighlightColor.copy(alpha = verseHighlightColor.alpha * highlightAlpha.value)
                        } else {
                            verseHighlightColor
                        },
                        wordColor = wordHighlightColor,
                        corner = highlightCorner,
                        extendEdges = true,
                        fullLineContainerWidthPx = fullLineContainerWidthPx,
                    )
                }
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
                when {
                    active -> colorScheme.primary.alpha(0.3f)
                    else -> Color.Transparent
                },
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
