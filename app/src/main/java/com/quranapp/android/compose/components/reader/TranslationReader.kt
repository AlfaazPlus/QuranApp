package com.quranapp.android.compose.components.reader

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.compose.theme.tightTextStyle
import com.quranapp.android.R
import com.quranapp.android.compose.components.ChapterIcon
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.compose.utils.LocalAppLocale
import com.quranapp.android.compose.utils.formattedStringResource
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.db.relations.SurahWithLocalizations
import com.quranapp.android.utils.quran.QuranMeta
import com.quranapp.android.utils.reader.LocalVerseActions
import com.quranapp.android.utils.reader.TranslationPageBuilderParams
import com.quranapp.android.utils.univ.StringUtils
import com.quranapp.android.viewModels.ReaderViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart

data class TranslationPageVerse(
    val chapterNo: Int,
    val verseNo: Int,
    /** Index within annotatedText (inclusive). */
    val rangeStart: Int,
    /** Index within annotatedText (Exclusive). */
    val rangeEnd: Int,
)

sealed class TranslationPageSection {
    object Divider : TranslationPageSection()
    data class Title(val swl: SurahWithLocalizations) : TranslationPageSection()
    object Bismillah : TranslationPageSection()

    data class Text(
        val annotatedText: AnnotatedString,
        val verses: List<TranslationPageVerse>,
    ) : TranslationPageSection()
}

data class TranslationPageItem(
    val pageNo: Int,
    val juzNo: Int,
    val hizbNos: List<Int>,
    val chapterNames: String,
    val translationSlug: String,
    val sections: List<TranslationPageSection>,
)

private class TranslationVersePositions(private val section: TranslationPageSection.Text) {
    private val ayahIds = IntArray(section.verses.size) { index ->
        val verse = section.verses[index]
        QuranMeta.getAyahId(verse.chapterNo, verse.verseNo)
    }
    private val tops = IntArray(section.verses.size)
    private val bottoms = IntArray(section.verses.size)
    private var hasLayout = false

    fun updateLayout(layout: TextLayoutResult, textLength: Int) {
        val lastOffset = (textLength - 1).coerceAtLeast(0)
        for ((index, verse) in section.verses.withIndex()) {
            val firstLine = layout.getLineForOffset(verse.rangeStart.coerceIn(0, lastOffset))
            val lastLine = layout.getLineForOffset((verse.rangeEnd - 1).coerceIn(0, lastOffset))
            tops[index] = layout.getLineTop(firstLine).toInt()
            bottoms[index] = layout.getLineBottom(lastLine).toInt()
        }
        hasLayout = true
    }

    fun updatePosition(tracker: VerseVisibilityTracker, rootTop: Int) {
        if (!hasLayout) return
        for (index in ayahIds.indices) {
            tracker.recordBounds(ayahIds[index], rootTop + tops[index], rootTop + bottoms[index])
        }
    }
}

@Composable
fun ReaderLayoutTranslationPageMode(
    readerVm: ReaderViewModel,
    nestedScrollConnection: NestedScrollConnection?,
    onSyncStateChanged: (Boolean) -> Unit = {},
) {
    val mushafSession by readerVm.mushafSession.collectAsState()
    val translationPageItems by readerVm.translationPageItems.collectAsState()

    val sessionLayout = mushafSession.layout
    val pageCount = mushafSession.pageCount

    val context = LocalContext.current
    val colors = colorScheme
    val typography = MaterialTheme.typography
    val verseActions = LocalVerseActions.current
    val translSizeMult = ReaderPreferences.observeTranlationTextSizeMultiplier()
    val buildParams = remember(context, colors, typography, verseActions, translSizeMult) {
        TranslationPageBuilderParams(
            context = context,
            colors = colors,
            type = typography,
            verseActions = verseActions,
            translationSizeMultiplier = translSizeMult,
        )
    }

    val initialPageIndex =
        mushafSession.currentPageNo?.minus(1)?.coerceAtLeast(0) ?: 0
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialPageIndex)
    val activePageIndex by remember(listState) {
        derivedStateOf { listState.firstVisibleItemIndex }
    }
    val visibilityTrackers = remember { VerseVisibilityRegistry() }
    val verseAnchor = remember { VerseScrollAnchor() }

    var previousMushafKey by remember { mutableStateOf(sessionLayout) }

    LaunchedEffect(sessionLayout, pageCount, mushafSession.currentPageNo) {
        val layoutJustChanged = previousMushafKey != sessionLayout

        if (!layoutJustChanged) return@LaunchedEffect

        if (pageCount <= 0) return@LaunchedEffect

        val p = mushafSession.currentPageNo ?: return@LaunchedEffect

        val idx = p.coerceIn(1, pageCount) - 1

        if (listState.firstVisibleItemIndex != idx) {
            listState.scrollToItem(idx)
        }

        previousMushafKey = sessionLayout
    }

    LaunchedEffect(listState, buildParams, mushafSession.version) {
        snapshotFlow {
            val visible = listState.layoutInfo.visibleItemsInfo

            if (visible.isEmpty()) {
                listOf(listState.firstVisibleItemIndex + 1)
            } else {
                visible.map { it.index + 1 }
            }
        }
            .onStart {
                emit(
                    listOf(mushafSession.currentPageNo ?: 1)
                )
            }
            .distinctUntilChanged()
            .collect { anchorPages ->
                readerVm.fetchTranslationPages(
                    context, anchorPages, buildParams
                )
            }
    }

    val navigateToPage by readerVm.navigateToPage.collectAsStateWithLifecycle()
    val navigateToVerse by readerVm.navigateToVerse.collectAsStateWithLifecycle()
    val latestNavigateToVerse by rememberUpdatedState(navigateToVerse)

    LaunchedEffect(context, listState, pageCount, navigateToPage) {
        listState.updatePageWhenIdle(pageCount) { currentPageNo ->
            if (navigateToPage == null && latestNavigateToVerse == null) {
                readerVm.updateCurrentPageNo(currentPageNo)
            }
        }
    }

    LaunchedEffect(listState, pageCount) {
        listState.updateVisibleVerseWhenIdle(
            pageCount = pageCount,
            trackers = visibilityTrackers,
            viewport = verseAnchor,
            onVerseVisible = readerVm::updateLastKnownVerse,
        )
    }

    LaunchedEffect(navigateToPage, pageCount) {
        val targetPage = navigateToPage ?: return@LaunchedEffect

        if (pageCount <= 0) return@LaunchedEffect

        val clamped = targetPage.coerceIn(1, pageCount)

        try {
            listState.scrollToItem(clamped - 1)
            readerVm.updateCurrentPageNo(clamped)
        } finally {
            readerVm.consumePageNavigation(targetPage)
        }
    }

    var verseTarget by remember { mutableStateOf<Pair<ChapterVersePair, Int>?>(null) }

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
        val targetIndex = clampedPage - 1
        val targetPageIsVisible =
            listState.layoutInfo.visibleItemsInfo.any { it.index == targetIndex }
        if (!targetPageIsVisible) listState.scrollToItem(targetIndex)

        readerVm.updateCurrentPageNo(clampedPage)

        val pageItem = snapshotFlow { translationPageItems[clampedPage] }
            .first { it != null } ?: return@LaunchedEffect
        val hasTargetVerse = pageItem.sections.any { section ->
            section is TranslationPageSection.Text &&
                    section.verses.any { it.chapterNo == targetVerse.chapterNo && it.verseNo == targetVerse.verseNo }
        }

        if (!hasTargetVerse) {
            readerVm.consumeVerseNavigation(targetVerse)
            return@LaunchedEffect
        }

        verseTarget = targetVerse to clampedPage

        listState.scrollToVersePosition(verseAnchor)
        readerVm.completeVerseNavigation(targetVerse)

        verseTarget = null
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
            highlightAfterNavigation = false,
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

        snapshotFlow { listState.firstVisibleItemIndex + 1 }
            .distinctUntilChanged()
            .collect { current ->
                onSyncStateChanged(current == expectedPage)
            }
    }


    SelectionContainer {
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
                val item = translationPageItems[pageNo]

                key(sessionLayout, pageIndex) {
                    if (pageIndex > 0) {
                        Spacer(Modifier.height(12.dp))
                    }

                    TranslationModePage(
                        item = item,
                        visibilityTrackers = visibilityTrackers,
                        trackVisibleVerses = pageIndex == activePageIndex,
                        targetVerse = verseTarget?.takeIf { it.second == pageNo }?.first,
                        verseAnchor = verseAnchor,
                    )
                }
            }
        }
    }
}

@Composable
private fun TranslationModePage(
    item: TranslationPageItem?,
    visibilityTrackers: VerseVisibilityRegistry,
    trackVisibleVerses: Boolean,
    targetVerse: ChapterVersePair?,
    verseAnchor: VerseScrollAnchor,
) {
    if (item == null) {
        TranslationPageLoadingSkeleton()
        return
    }

    val playerState = LocalRecitation.current
    val isPlaying = playerState.isAnyPlaying
    val playingVerse = playerState.playingVerse
    val isRtl = StringUtils.isRtlLanguage(item.translationSlug)
    val playingVerseToHighlight = playingVerse.takeIf { isPlaying && it.isValid }
    val visibilityTracker = remember(item) {
        VerseVisibilityTracker(
            item.sections.asSequence()
                .filterIsInstance<TranslationPageSection.Text>()
                .flatMap { it.verses.asSequence() }
                .map { QuranMeta.getAyahId(it.chapterNo, it.verseNo) }
                .distinct().toList().toIntArray())
    }
    androidx.compose.runtime.DisposableEffect(item.pageNo, visibilityTracker, visibilityTrackers) {
        visibilityTrackers.register(item.pageNo, visibilityTracker)
        onDispose { visibilityTrackers.unregister(item.pageNo, visibilityTracker) }
    }
    val textDirection = if (isRtl) TextDirection.Rtl else TextDirection.Ltr

    CompositionLocalProvider(
        LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else {
            LayoutDirection.Ltr
        }
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val pageInset = (maxWidth * 0.025f).coerceIn(8.dp, 20.dp)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = pageInset)
            ) {
                TranslationBookPageHeader(
                    pageNo = item.pageNo,
                    chapterNames = item.chapterNames,
                    juzNo = item.juzNo,
                    hizbNos = item.hizbNos,
                )

                item.sections.forEach {
                    when (it) {
                        TranslationPageSection.Divider -> HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = colorScheme.outlineVariant,
                        )

                        is TranslationPageSection.Title -> TranslationReaderChapterTitle(it.swl)
                        is TranslationPageSection.Bismillah -> Bismillah()
                        is TranslationPageSection.Text -> TranslationPageText(
                            section = it,
                            targetVerse = targetVerse,
                            playingVerse = playingVerseToHighlight,
                            verseAnchor = verseAnchor,
                            textDirection = textDirection,
                            visibilityTracker = visibilityTracker,
                            trackVisibleVerses = trackVisibleVerses,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TranslationPageText(
    section: TranslationPageSection.Text,
    targetVerse: ChapterVersePair?,
    playingVerse: ChapterVersePair?,
    verseAnchor: VerseScrollAnchor,
    textDirection: TextDirection,
    visibilityTracker: VerseVisibilityTracker,
    trackVisibleVerses: Boolean,
) {
    val text = section.annotatedText
    val targetRange = remember(section.verses, targetVerse) {
        targetVerse?.let { target ->
            section.verses.firstOrNull {
                it.chapterNo == target.chapterNo && it.verseNo == target.verseNo
            }
        }
    }
    var textTopPx by remember(targetRange) { mutableIntStateOf(Int.MIN_VALUE) }
    var verseLineTopPx by remember(targetRange) { mutableIntStateOf(Int.MIN_VALUE) }
    var verseLineBottomPx by remember(targetRange) { mutableIntStateOf(Int.MIN_VALUE) }
    var textLayout by remember(section) { mutableStateOf<TextLayoutResult?>(null) }
    val versePositions = remember(section) { TranslationVersePositions(section) }

    val highlightState = LocalTransientVerseHighlight.current
    val highlightedVerse = highlightState.verse
    val highlightAlpha = highlightState.alpha
    val highlightColor = colorScheme.primary

    val navigationRange = remember(section.verses, highlightedVerse) {
        highlightedVerse?.let { target ->
            section.verses.firstOrNull {
                it.chapterNo == target.chapterNo && it.verseNo == target.verseNo
            }
        }
    }

    val playingRange = remember(section.verses, playingVerse, highlightedVerse) {
        playingVerse?.takeIf { it != highlightedVerse }?.let { target ->
            section.verses.firstOrNull {
                it.chapterNo == target.chapterNo && it.verseNo == target.verseNo
            }
        }
    }

    fun updateAnchor() {
        if (
            textTopPx != Int.MIN_VALUE &&
            verseLineTopPx != Int.MIN_VALUE &&
            verseLineBottomPx != Int.MIN_VALUE
        ) {
            verseAnchor.targetTopPx = textTopPx + verseLineTopPx
            verseAnchor.targetBottomPx = textTopPx + verseLineBottomPx
        }
    }

    val anchorModifier = if (targetRange == null && !trackVisibleVerses) {
        Modifier
    } else {
        Modifier.onGloballyPositioned { coordinates ->
            val rootTop = coordinates.positionInRoot().y.toInt()
            textTopPx = rootTop
            if (targetRange != null) {
                updateAnchor()
            }
            if (trackVisibleVerses) {
                versePositions.updatePosition(visibilityTracker, rootTop)
            }
        }
    }

    val highlightModifier = if (navigationRange == null && playingRange == null) {
        Modifier
    } else Modifier.drawBehind {
        val layout = textLayout ?: return@drawBehind
        if (text.isEmpty()) return@drawBehind

        fun drawRange(range: TranslationPageVerse, alpha: Float) {
            if (range.rangeEnd <= range.rangeStart || alpha <= 0f) return
            val startOffset = range.rangeStart.coerceIn(0, text.length - 1)
            val endOffset = range.rangeEnd.coerceIn(startOffset + 1, text.length)
            val firstLine = layout.getLineForOffset(startOffset)
            val lastLine = layout.getLineForOffset(endOffset - 1)

            for (line in firstLine..lastLine) {
                val lineStart = maxOf(startOffset, layout.getLineStart(line))
                val lineEnd = minOf(endOffset, layout.getLineEnd(line, visibleEnd = false)) - 1
                if (lineEnd < lineStart) continue

                val firstBounds = layout.getBoundingBox(lineStart)
                val lastBounds = layout.getBoundingBox(lineEnd)

                drawRoundRect(
                    color = highlightColor.copy(alpha = VERSE_HIGHLIGHT_ALPHA * alpha),
                    topLeft = Offset(
                        minOf(firstBounds.left, lastBounds.left),
                        layout.getLineTop(line)
                    ),
                    size = Size(
                        maxOf(firstBounds.right, lastBounds.right) - minOf(
                            firstBounds.left,
                            lastBounds.left
                        ),
                        layout.getLineBottom(line) - layout.getLineTop(line),
                    ),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                )
            }
        }

        playingRange?.let { drawRange(it, 1f) }
        navigationRange?.let { drawRange(it, highlightAlpha.value) }
    }

    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .then(anchorModifier)
            .then(highlightModifier),
        style = TextStyle(textDirection = textDirection),
        onTextLayout = { layout ->
            textLayout = layout
            versePositions.updateLayout(layout, text.length)
            if (trackVisibleVerses && textTopPx != Int.MIN_VALUE) {
                versePositions.updatePosition(visibilityTracker, textTopPx)
            }

            if (targetRange != null) {
                val lastOffset = (text.length - 1).coerceAtLeast(0)
                val start = layout.getLineForOffset(targetRange.rangeStart.coerceIn(0, lastOffset))
                val end =
                    layout.getLineForOffset((targetRange.rangeEnd - 1).coerceIn(0, lastOffset))
                verseLineTopPx = layout.getLineTop(start).toInt()
                verseLineBottomPx = layout.getLineBottom(end).toInt()
                updateAnchor()
            }
        },
    )
}

@Composable
internal fun TranslationBookPageHeader(
    pageNo: Int,
    chapterNames: String,
    juzNo: Int,
    hizbNos: List<Int>,
) {
    val appLocale = LocalAppLocale.current
    val typography = MaterialTheme.typography
    val scheme = colorScheme
    val juzLabel =
        if (juzNo > 0) formattedStringResource(R.string.strLabelJuzNo, juzNo) else ""
    val hizbLabel =
        hizbNos
            .filter { it > 0 }
            .distinct()
            .sorted()
            .let { hizbs ->
                when (hizbs.size) {
                    0 -> ""
                    else -> "${stringResource(R.string.strTitleReaderHizb)} ${
                        hizbs.map { String.format(appLocale.platformLocale, "%d", it) }
                            .joinToString(" / ")
                    }"
                }
            }
    val rightLabel = listOf(juzLabel, hizbLabel).filter { it.isNotBlank() }.joinToString(", ")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        color = colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = chapterNames,
                    style = typography.labelSmall.merge(tightTextStyle),
                    color = scheme.onSurface.alpha(0.75f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .basicMarquee(
                            initialDelayMillis = 900,
                            repeatDelayMillis = 1_200,
                        ),
                )
            }

            Text(
                text = formattedStringResource(R.string.strLabelPageNo, pageNo),
                style = typography.labelMedium.merge(tightTextStyle),
                color = scheme.onBackground.alpha(0.75f),
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .background(colorScheme.background, shapes.extraLarge)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                maxLines = 1,
            )

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text(
                    text = rightLabel,
                    style = typography.labelSmall.merge(tightTextStyle),
                    color = scheme.onSurface.alpha(0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}


@Composable
internal fun TranslationPageLoadingSkeleton() {
    val scheme = colorScheme
    val transition = rememberInfiniteTransition(label = "translation_page_sk")
    val pulse by transition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    val barColor = scheme.onSurface.copy(alpha = pulse)
    val lineWidths = listOf(1f, 0.97f, 0.92f, 1f, 0.85f, 0.94f, 0.78f, 1f, 0.88f, 0.72f, 0.58f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 380.dp)
            .padding(horizontal = 12.dp),
        shape = RoundedCornerShape(4.dp),
        color = scheme.surfaceContainer,
        border = BorderStroke(
            1.dp,
            scheme.outlineVariant.copy(alpha = 0.45f),
        ),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(24.dp)
                        .background(barColor, RoundedCornerShape(4.dp)),
                )

                Box(
                    Modifier
                        .padding(horizontal = 24.dp)
                        .width(88.dp)
                        .height(24.dp)
                        .background(barColor, shapes.extraLarge),
                )

                Box(
                    Modifier
                        .weight(1f)
                        .height(24.dp)
                        .background(barColor, RoundedCornerShape(4.dp)),
                )
            }

            HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.4f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
            ) {
                for (w in lineWidths) {
                    Box(
                        Modifier
                            .fillMaxWidth(w)
                            .height(32.dp)
                            .padding(vertical = 5.dp)
                            .background(barColor, shapes.large),
                    )
                }
            }
        }
    }
}

@Composable
fun TranslationReaderChapterTitle(
    swl: SurahWithLocalizations,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                stringResource(R.string.strLabelSurah, swl.getCurrentName()),
                style = typography.labelLarge
            )
            Text(
                swl.getCurrentMeaning(),
                style = typography.bodyMedium,
                color = colorScheme.onSurface.alpha(0.75f)
            )
        }

        VerticalDivider(
            modifier = Modifier.height(32.dp),
            color = colorScheme.onSurface
        )

        ChapterIcon(
            swl.surah.surahNo,
            fontSize = 36.sp,
            modifier = Modifier.padding(top = 8.dp),
            color = colorScheme.primary
        )

    }
}
