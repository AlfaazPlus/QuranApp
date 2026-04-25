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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alfaazplus.sunnah.ui.theme.tightTextStyle
import com.quranapp.android.R
import com.quranapp.android.compose.components.ChapterIcon
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.db.relations.SurahWithLocalizations
import com.quranapp.android.utils.reader.LocalVerseActions
import com.quranapp.android.utils.reader.TranslationPageBuilderParams
import com.quranapp.android.utils.univ.StringUtils
import com.quranapp.android.viewModels.ReaderViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
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
        var annotatedTextNormalized: AnnotatedString? = null,
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

@Composable
fun ReaderLayoutTranslationPageMode(
    readerVm: ReaderViewModel,
    nestedScrollConnection: NestedScrollConnection,
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

    LaunchedEffect(context, listState, pageCount, navigateToPage) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { currentIndex ->
                if (navigateToPage != null) return@collect
                if (pageCount <= 0) return@collect

                val currentPageNo = (currentIndex + 1).coerceIn(1, pageCount)

                readerVm.updateCurrentPageNo(currentPageNo)
                readerVm.updateLastKnownVerseFromTranslationPage(currentPageNo)
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

    val navigateToVerse by readerVm.navigateToVerse.collectAsStateWithLifecycle()

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

        snapshotFlow { listState.firstVisibleItemIndex }
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
                .nestedScroll(nestedScrollConnection),
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
                    )
                }
            }
        }
    }
}

@Composable
private fun TranslationModePage(
    item: TranslationPageItem?
) {
    if (item == null) {
        TranslationPageLoadingSkeleton()
        return
    }

    val playerState = LocalRecitation.current
    val isPlaying = playerState.isAnyPlaying
    val playingVerse = playerState.playingVerse
    val isRtl = StringUtils.isRtlLanguage(item.translationSlug)
    val colors = colorScheme

    val sections = remember(item.sections, isPlaying, playingVerse, colors) {
        item.sections.map { section ->
            if (section is TranslationPageSection.Text) {
                section.annotatedTextNormalized = buildAnnotatedString {
                    append(section.annotatedText)

                    if (!isPlaying || !playingVerse.isValid) return@buildAnnotatedString

                    val v = section.verses.find {
                        it.chapterNo == playingVerse.chapterNo && it.verseNo == playingVerse.verseNo
                    } ?: return@buildAnnotatedString

                    addStyle(
                        SpanStyle(background = colors.primary.alpha(0.2f)),
                        v.rangeStart,
                        v.rangeEnd,
                    )
                }
            }

            section
        }
    }

    val textDirection = if (isRtl) TextDirection.Rtl else TextDirection.Ltr

    CompositionLocalProvider(
        LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else {
            LayoutDirection.Ltr
        }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            shape = shapes.small,
            color = colorScheme.surface,
            border = BorderStroke(
                1.dp,
                colorScheme.outlineVariant,
            ),
            shadowElevation = 1.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Column(Modifier.fillMaxWidth()) {
                    TranslationBookPageHeader(item)

                    HorizontalDivider(
                        color = colorScheme.outlineVariant,
                    )

                    sections.forEach {
                        when (it) {
                            TranslationPageSection.Divider -> HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = colorScheme.outlineVariant,
                            )

                            is TranslationPageSection.Title -> TranslationReaderChapterTitle(it.swl)
                            is TranslationPageSection.Bismillah -> Bismillah()
                            is TranslationPageSection.Text -> {
                                if (it.annotatedTextNormalized != null) {
                                    Text(
                                        text = it.annotatedTextNormalized!!,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        style = TextStyle(textDirection = textDirection),
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
private fun TranslationBookPageHeader(item: TranslationPageItem) {
    val typography = MaterialTheme.typography
    val scheme = colorScheme
    val juzLabel =
        if (item.juzNo > 0) stringResource(R.string.strLabelJuzNo, item.juzNo) else ""
    val hizbLabel =
        item.hizbNos
            .filter { it > 0 }
            .distinct()
            .sorted()
            .let { hizbs ->
                when (hizbs.size) {
                    0 -> ""
                    else -> "${stringResource(R.string.strTitleReaderHizb)} ${
                        hizbs.map { String.format("%d", it) }.joinToString(" / ")
                    }"
                }
            }
    val rightLabel = listOf(juzLabel, hizbLabel).filter { it.isNotBlank() }.joinToString(", ")

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
                text = item.chapterNames,
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
            text = stringResource(R.string.strLabelPageNo, item.pageNo),
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


@Composable
private fun TranslationPageLoadingSkeleton() {
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