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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranapp.android.R
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.utils.reader.LocalVerseActions
import com.quranapp.android.utils.reader.TranslUtils
import com.quranapp.android.utils.reader.TranslationPageBuilderParams
import com.quranapp.android.viewModels.ReaderViewModel
import kotlinx.coroutines.flow.distinctUntilChanged

data class TranslationPageItem(
    val pageNo: Int,
    val juzNo: Int,
    val hizbNo: Int,
    val chapterNames: String,
    val translationSlug: String,
    val annotatedText: AnnotatedString,
    val verses: List<TranslationPageVerse>,
)

data class TranslationPageVerse(
    val chapterNo: Int,
    val verseNo: Int,
    /** Index within annotatedText (inclusive). */
    val rangeStart: Int,
    /** Index within annotatedText (Exclusive). */
    val rangeEnd: Int,
)


@Composable
fun ReaderLayoutTranslationPageMode(
    readerVm: ReaderViewModel,
    nestedScrollConnection: NestedScrollConnection,
    onSyncStateChanged: (Boolean) -> Unit = {},
) {
    val uiState by readerVm.uiState.collectAsStateWithLifecycle()
    val mushafLayoutKey by readerVm.mushafLayoutKey

    val pageCount by produceState(0, mushafLayoutKey) {
        value = readerVm.mushafPageCount(mushafLayoutKey.toMushafId())
    }

    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
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

    LaunchedEffect(buildParams) {
        readerVm.clearTranslationPageCache()
    }

    val initialPageIndex =
        uiState.currentPageNo?.minus(1)?.coerceAtLeast(0) ?: 0
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialPageIndex)

    var previousMushafKey by remember { mutableStateOf(mushafLayoutKey) }

    LaunchedEffect(mushafLayoutKey, pageCount, uiState.currentPageNo) {
        val layoutJustChanged = previousMushafKey != mushafLayoutKey

        if (!layoutJustChanged) return@LaunchedEffect

        if (pageCount <= 0) return@LaunchedEffect

        val p = uiState.currentPageNo ?: return@LaunchedEffect

        val idx = p.coerceIn(1, pageCount) - 1

        if (listState.firstVisibleItemIndex != idx) {
            listState.scrollToItem(idx)
        }

        previousMushafKey = mushafLayoutKey
    }

    LaunchedEffect(listState, pageCount, mushafLayoutKey, buildParams) {
        snapshotFlow {
            val visible = listState.layoutInfo.visibleItemsInfo

            if (visible.isEmpty()) {
                listOf(listState.firstVisibleItemIndex + 1)
            } else {
                visible.map { it.index + 1 }
            }
        }
            .distinctUntilChanged()
            .collect { anchorPages ->
                if (pageCount > 0) {
                    readerVm.fetchTranslationPages(
                        context, anchorPages, pageCount, buildParams
                    )
                }
            }
    }

    LaunchedEffect(uiState.currentPageNo, pageCount, mushafLayoutKey, buildParams) {
        val vmPage = uiState.currentPageNo ?: return@LaunchedEffect

        readerVm.fetchTranslationPages(
            context, listOf(vmPage), pageCount, buildParams
        )
    }

    val navigateToPage by readerVm.navigateToPage.collectAsStateWithLifecycle()

    LaunchedEffect(context, listState, pageCount, navigateToPage) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { currentIndex ->
                if (navigateToPage != null) return@collect
                if (pageCount <= 0) return@collect

                val currentPageNo = (currentIndex + 1).coerceIn(1, pageCount)

                readerVm.updateState {
                    it.copy(currentPageNo = currentPageNo)
                }

                readerVm.updateLastKnownVerseFromTranslationPage(currentPageNo)
            }
    }

    LaunchedEffect(navigateToPage, pageCount) {
        val targetPage = navigateToPage ?: return@LaunchedEffect

        if (pageCount <= 0) return@LaunchedEffect

        val clamped = targetPage.coerceIn(1, pageCount)

        try {
            listState.scrollToItem(clamped - 1)

            if (clamped != targetPage) {
                readerVm.updateState { it.copy(currentPageNo = clamped) }
            }
        } finally {
            readerVm.consumePageNavigation()
        }
    }

    val navigateToVerse by readerVm.navigateToVerse.collectAsStateWithLifecycle()

    LaunchedEffect(navigateToVerse, pageCount) {
        val targetVerse = navigateToVerse ?: return@LaunchedEffect

        val targetPage = readerVm.resolvePageNo(targetVerse.chapterNo, targetVerse.verseNo)
            ?: return@LaunchedEffect

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
                key = { index -> mushafLayoutKey to index },
            ) { pageIndex ->
                key(mushafLayoutKey, pageIndex) {
                    if (pageIndex > 0) {
                        Spacer(Modifier.height(12.dp))
                    }

                    TranslationModePage(
                        readerVm = readerVm,
                        pageNo = pageIndex + 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun TranslationModePage(
    readerVm: ReaderViewModel,
    pageNo: Int,
) {
    val i by remember(pageNo) {
        derivedStateOf { readerVm.translationPageItems[pageNo] }
    }

    val item = i
    if (item == null) {
        TranslationPageLoadingSkeleton()
        return
    }

    val playerState = LocalRecitation.current
    val isPlaying = playerState.isAnyPlaying
    val playingVerse = playerState.playingVerse
    val isRtl = TranslUtils.isRtl(item.translationSlug)
    val colors = colorScheme

    val displayText = remember(item.annotatedText, item.verses, isPlaying, playingVerse, colors) {
        buildAnnotatedString {
            append(item.annotatedText)

            if (!isPlaying || !playingVerse.isValid) return@buildAnnotatedString

            val v = item.verses.find {
                it.chapterNo == playingVerse.chapterNo && it.verseNo == playingVerse.verseNo
            } ?: return@buildAnnotatedString

            addStyle(
                SpanStyle(background = colors.primary.alpha(0.2f)),
                v.rangeStart,
                v.rangeEnd,
            )
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
            shape = RoundedCornerShape(4.dp),
            color = colorScheme.surfaceContainer,
            border = BorderStroke(
                1.dp,
                colorScheme.outlineVariant.copy(alpha = 0.45f),
            ),
        ) {
            Column() {
                TranslationBookPageHeader(
                    chapterNames = item.chapterNames,
                    pageNo = item.pageNo,
                    juzNo = item.juzNo,
                )

                HorizontalDivider(
                    color = colorScheme.outlineVariant.copy(alpha = 0.55f),
                )

                Text(
                    text = displayText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    style = TextStyle(textDirection = textDirection),
                )
            }
        }
    }
}

@Composable
private fun TranslationBookPageHeader(
    chapterNames: String,
    pageNo: Int,
    juzNo: Int,
) {
    val typography = MaterialTheme.typography
    val scheme = colorScheme
    val juzLabel =
        if (juzNo > 0) stringResource(R.string.strLabelJuzNo, juzNo) else ""

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
                text = chapterNames.ifBlank { "—" },
                style = typography.bodyMedium,
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
            text = stringResource(R.string.strLabelPageNo, pageNo),
            style = typography.labelMedium,
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
                text = juzLabel.ifBlank { "—" },
                style = typography.bodyMedium,
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
    val scheme = MaterialTheme.colorScheme
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

