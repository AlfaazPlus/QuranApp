package com.quranapp.android.compose.components.reader

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranapp.android.compose.components.reader.dialogs.WbwSheetData
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.compose.utils.ThemeUtils
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.db.entities.quran.AyahWordEntity
import com.quranapp.android.db.relations.SurahWithLocalizations
import com.quranapp.android.utils.quran.QuranMeta
import com.quranapp.android.utils.reader.QuranTextStyleParams
import com.quranapp.android.utils.reader.atlas.AtlasGlyphPlacement
import com.quranapp.android.utils.reader.atlas.getForWord
import com.quranapp.android.utils.reader.getQuranTextStyle
import com.quranapp.android.viewModels.ReaderViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart

sealed class ReadingFlowSection {
    object Divider : ReadingFlowSection()
    data class Title(val swl: SurahWithLocalizations) : ReadingFlowSection()
    object Bismillah : ReadingFlowSection()

    data class Words(
        val words: List<AyahWordEntity>,
        val atlasPlacements: Map<Int, List<AtlasGlyphPlacement>>,
    ) : ReadingFlowSection()
}

data class ReadingFlowPageItem(
    val pageNo: Int,
    val juzNo: Int,
    val hizbNos: List<Int>,
    val chapterNames: String,
    val sections: List<ReadingFlowSection>,
    val cacheKey: String,
)

@Composable
fun ReaderLayoutReadingFlow(
    readerVm: ReaderViewModel,
    nestedScrollConnection: NestedScrollConnection?,
    onSyncStateChanged: (Boolean) -> Unit = {},
) {
    val mushafSession by readerVm.mushafSession.collectAsState()
    val readingFlowPageItems by readerVm.readingFlowPageItems.collectAsState()

    val sessionLayout = mushafSession.layout
    val pageCount = mushafSession.pageCount

    val context = LocalContext.current
    val isDark = ThemeUtils.observeDarkTheme()

    val initialPageIndex =
        mushafSession.currentPageNo?.minus(1)?.coerceAtLeast(0) ?: 0
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialPageIndex)

    var previousMushafKey by remember { mutableStateOf(sessionLayout) }

    LaunchedEffect(sessionLayout, pageCount, mushafSession.currentPageNo) {
        val layoutJustChanged = previousMushafKey != sessionLayout
        if (!layoutJustChanged) return@LaunchedEffect
        if (pageCount <= 0) return@LaunchedEffect

        val pageNo = mushafSession.currentPageNo ?: return@LaunchedEffect
        val idx = pageNo.coerceIn(1, pageCount) - 1

        if (listState.firstVisibleItemIndex != idx) {
            listState.scrollToItem(idx)
        }

        previousMushafKey = sessionLayout
    }

    LaunchedEffect(listState, mushafSession.version, isDark) {
        snapshotFlow {
            val visible = listState.layoutInfo.visibleItemsInfo

            if (visible.isEmpty()) {
                listOf(listState.firstVisibleItemIndex + 1)
            } else {
                visible.map { it.index + 1 }
            }
        }
            .onStart {
                emit(listOf(mushafSession.currentPageNo ?: 1))
            }
            .distinctUntilChanged()
            .collect { anchorPages ->
                readerVm.fetchReadingFlowPages(context, anchorPages, isDark)
            }
    }

    val navigateToPage by readerVm.navigateToPage.collectAsStateWithLifecycle()
    val navigateToVerse by readerVm.navigateToVerse.collectAsStateWithLifecycle()
    val latestNavigateToVerse by rememberUpdatedState(navigateToVerse)

    LaunchedEffect(context, listState, pageCount, navigateToPage) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { currentIndex ->
                if (navigateToPage != null || latestNavigateToVerse != null) return@collect
                if (pageCount <= 0) return@collect

                val currentPageNo = (currentIndex + 1).coerceIn(1, pageCount)
                readerVm.updateCurrentPageNo(currentPageNo)
                readerVm.updateLastKnownVerseFromReadingFlowPage(currentPageNo)
            }
    }

    LaunchedEffect(navigateToPage, pageCount) {
        val targetPage = navigateToPage ?: return@LaunchedEffect
        if (pageCount <= 0) return@LaunchedEffect

        val clamped = targetPage.coerceIn(1, pageCount)

        try {
            listState.scrollToItem(clamped - 1)
            readerVm.updateCurrentPageNo(clamped)
        } finally {
            readerVm.consumePageNavigation()
        }
    }

    var verseBringIntoViewTarget by remember { mutableStateOf<Pair<ChapterVersePair, Int>?>(null) }
    val verseAnchor = remember { VerseScrollAnchor() }

    LaunchedEffect(navigateToVerse, pageCount, sessionLayout, mushafSession.version) {
        val targetVerse = navigateToVerse ?: return@LaunchedEffect
        if (pageCount <= 0) return@LaunchedEffect

        verseBringIntoViewTarget = null

        val targetPage = readerVm.resolvePageNo(targetVerse.chapterNo, targetVerse.verseNo)
            ?: run {
                readerVm.consumeVerseNavigation()
                return@LaunchedEffect
            }

        verseAnchor.beginTarget()

        val clampedPage = targetPage.coerceIn(1, pageCount)

        val targetIndex = clampedPage - 1

        val targetPageIsVisible = listState.layoutInfo.visibleItemsInfo
            .any { it.index == targetIndex }
        if (!targetPageIsVisible) listState.scrollToItem(targetIndex)

        readerVm.updateCurrentPageNo(clampedPage)

        val pageItem = snapshotFlow { readingFlowPageItems[clampedPage] }
            .first { it != null } ?: return@LaunchedEffect

        val hasTargetVerse = pageItem.sections.any { section ->
            section is ReadingFlowSection.Words && section.words.any { it.ayahId == targetVerse.ayahId }
        }

        if (!hasTargetVerse) {
            readerVm.consumeVerseNavigation()
            return@LaunchedEffect
        }

        verseBringIntoViewTarget = targetVerse to clampedPage

        listState.scrollToVersePosition(verseAnchor)
        readerVm.consumeVerseNavigation()

        verseBringIntoViewTarget = null
    }

    val playerState = LocalRecitation.current
    val isPlaying = playerState.isAnyPlaying
    val playingVerse = playerState.playingVerse
    var playerVerseSync by readerVm.playerVerseSync

    LaunchedEffect(playerVerseSync, isPlaying, playingVerse, pageCount) {
        if (!playerVerseSync || !isPlaying || !playingVerse.isValid || pageCount <= 0) {
            return@LaunchedEffect
        }

        val targetPage = readerVm.resolvePageNo(playingVerse.chapterNo, playingVerse.verseNo)
            ?: return@LaunchedEffect
        if (targetPage !in 1..pageCount) return@LaunchedEffect

        readerVm.requestVerseNavigation(playingVerse.chapterNo, playingVerse.verseNo)
    }

    LaunchedEffect(playingVerse, pageCount) {
        if (!playingVerse.isValid || pageCount <= 0) {
            onSyncStateChanged(false)
            return@LaunchedEffect
        }

        val expectedPage = readerVm.resolvePageNo(playingVerse.chapterNo, playingVerse.verseNo)

        if (expectedPage == null || expectedPage !in 1..pageCount) {
            onSyncStateChanged(false)
            return@LaunchedEffect
        }

        snapshotFlow { listState.firstVisibleItemIndex + 1 }
            .distinctUntilChanged()
            .collect { current ->
                onSyncStateChanged(current == expectedPage)
            }
    }

    TextStyleProvider(emptyMap()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    val top = coordinates.positionInRoot().y.toInt()
                    verseAnchor.viewportTopPx = top
                    verseAnchor.viewportBottomPx = top + coordinates.size.height
                }
                .then(
                    if (nestedScrollConnection == null) Modifier
                    else Modifier.nestedScroll(nestedScrollConnection)
                ),
            contentPadding = PaddingValues(top = 16.dp, bottom = 240.dp),
        ) {
            items(
                count = pageCount,
                key = { index -> sessionLayout to index },
            ) { pageIndex ->
                val pageNo = pageIndex + 1
                val item = readingFlowPageItems[pageNo]

                key(sessionLayout, pageIndex) {
                    if (pageIndex > 0) {
                        Spacer(Modifier.height(12.dp))
                    }

                    ReadingFlowPage(
                        item = item,
                        targetVerse = verseBringIntoViewTarget
                            ?.takeIf { it.second == pageNo }
                            ?.first,
                        onWordPositioned = { verse, top, bottom, isStart, isEnd ->
                            if (verseBringIntoViewTarget?.first == verse) {
                                if (isStart) verseAnchor.targetTopPx = top
                                if (isEnd) verseAnchor.targetBottomPx = bottom
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadingFlowPage(
    item: ReadingFlowPageItem?,
    targetVerse: ChapterVersePair?,
    onWordPositioned: (ChapterVersePair, Int, Int, Boolean, Boolean) -> Unit,
) {
    if (item == null) {
        TranslationPageLoadingSkeleton()
        return
    }

    val style = rememberReadingFlowStyle(item.pageNo)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        shape = shapes.small,
        color = colorScheme.surface,
        border = BorderStroke(1.dp, colorScheme.outlineVariant),
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.fillMaxWidth()) {
            TranslationBookPageHeader(
                pageNo = item.pageNo,
                chapterNames = item.chapterNames,
                juzNo = item.juzNo,
                hizbNos = item.hizbNos,
            )

            HorizontalDivider(color = colorScheme.outlineVariant)

            item.sections.forEach { section ->
                when (section) {
                    ReadingFlowSection.Bismillah -> Bismillah()
                    ReadingFlowSection.Divider -> HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = colorScheme.outlineVariant,
                    )

                    is ReadingFlowSection.Title -> TranslationReaderChapterTitle(section.swl)
                    is ReadingFlowSection.Words -> ReadingFlowWords(
                        words = section.words,
                        atlasPlacements = section.atlasPlacements,
                        style = style,
                        targetVerse = targetVerse,
                        onWordPositioned = onWordPositioned,
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberReadingFlowStyle(pageNo: Int): TextStyle {
    val context = LocalContext.current
    val script = ReaderPreferences.observeQuranScript()
    val sizeMultiplier = ReaderPreferences.observeArabicTextSizeMultiplier()
    val isDark = ThemeUtils.observeDarkTheme()
    val colors = colorScheme
    val type = MaterialTheme.typography
    val fontResolver = LocalReaderViewModel.current.fontResolver

    return remember(script, sizeMultiplier, isDark, pageNo, colors, type) {
        getQuranTextStyle(
            QuranTextStyleParams(
                context = context,
                fontResolver = fontResolver,
                colors = colors,
                type = type,
                pageNo = pageNo,
                script = script,
                sizeMultiplier = sizeMultiplier,
                isDark = isDark,
            )
        )
    }
}

@Composable
private fun ReadingFlowWords(
    words: List<AyahWordEntity>,
    atlasPlacements: Map<Int, List<AtlasGlyphPlacement>>,
    style: TextStyle,
    targetVerse: ChapterVersePair?,
    onWordPositioned: (ChapterVersePair, Int, Int, Boolean, Boolean) -> Unit,
) {
    val playerState = LocalRecitation.current
    val isPlaying = playerState.isAnyPlaying
    val playingVerse = playerState.playingVerse
    val playingWordIndex = playerState.playingWordIndex
    val wbwState = LocalWbwState.current
    val shouldShowTooltip = !wbwState.isWbwSheetOpen
    val targetAyahId = targetVerse?.ayahId
    val targetWordIndex = if (targetAyahId == null) null
    else words.firstOrNull { it.ayahId == targetAyahId }?.wordIndex

    val playingAyahId = remember(isPlaying, playingVerse) {
        if (!isPlaying || !playingVerse.isValid) -1
        else QuranMeta.getAyahId(playingVerse.chapterNo, playingVerse.verseNo)
    }

    val bounds = remember(words) { RecitationWordBounds(words.size) }
    val trackPositions = playingAyahId >= 0

    // Stable modifiers so word-index ticks don't recompose every word cell.
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

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
                .drawBehind {
                    drawRecitationHighlights(
                        bounds = bounds,
                        words = words,
                        ayahId = playingAyahId,
                        wordIndex = playingWordIndex,
                        splitRows = true,
                        verseColor = verseHighlightColor,
                        wordColor = wordHighlightColor,
                        corner = highlightCorner,
                    )
                },
            horizontalArrangement = Arrangement.Start,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            for ((index, word) in words.withIndex()) {
                ReadingFlowWordCell(
                    word = word,
                    atlasPlacements = atlasPlacements.getForWord(word),
                    style = style,
                    showTooltip = shouldShowTooltip && wbwState.activeTooltipWord == word,
                    modifier = positionModifiers[index],
                    targetVerse = targetVerse?.takeIf {
                        word.ayahId == targetAyahId &&
                            (word.wordIndex == targetWordIndex || word.isLastWordOfAyah)
                    },
                    targetIsStart = word.ayahId == targetAyahId && word.wordIndex == targetWordIndex,
                    targetIsEnd = word.ayahId == targetAyahId && word.isLastWordOfAyah,
                    onWordPositioned = onWordPositioned,
                )
            }
        }
    }
}

@Composable
private fun ReadingFlowWordCell(
    word: AyahWordEntity,
    atlasPlacements: List<AtlasGlyphPlacement>?,
    style: TextStyle,
    showTooltip: Boolean,
    modifier: Modifier = Modifier,
    targetVerse: ChapterVersePair?,
    targetIsStart: Boolean,
    targetIsEnd: Boolean,
    onWordPositioned: (ChapterVersePair, Int, Int, Boolean, Boolean) -> Unit,
) {
    val wbwState = LocalWbwState.current
    val active = wbwState.activeTooltipWord == word
    val targetPositionModifier = targetVerse?.let { verse ->
        Modifier.onGloballyPositioned { coordinates ->
            val top = coordinates.positionInRoot().y.toInt()
            onWordPositioned(
                verse,
                top,
                top + coordinates.size.height,
                targetIsStart,
                targetIsEnd,
            )
        }
    } ?: Modifier

    val wordContent: @Composable () -> Unit = {
        ReadingFlowWord(
            word = word,
            atlasPlacements = atlasPlacements,
            style = style,
            active = active,
            onClick = { wbwState.onWordClick(word) },
            modifier = modifier.then(targetPositionModifier),
        )
    }

    if (showTooltip) {
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
            textStyles = LocalQuranTextStyle.current,
            anchor = wordContent,
        )
    } else {
        wordContent()
    }
}

@Composable
private fun ReadingFlowWord(
    word: AyahWordEntity,
    atlasPlacements: List<AtlasGlyphPlacement>?,
    style: TextStyle,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    QuranWordText(
        modifier = modifier
            .background(
                if (active) colorScheme.primary.alpha(0.3f) else Color.Transparent,
                shape = shapes.small,
            )
            .clickable(onClick = onClick)
            .padding(
                horizontal = 3.dp,
                vertical = 1.5.dp,
            ),
        word = word,
        atlasPlacements = atlasPlacements,
        style = style,
    )
}
