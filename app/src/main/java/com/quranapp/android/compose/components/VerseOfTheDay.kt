package com.quranapp.android.compose.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quranapp.android.R
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.compose.components.common.IconButton
import com.quranapp.android.compose.components.common.Loader
import com.quranapp.android.compose.components.reader.LocalReaderViewModel
import com.quranapp.android.compose.components.reader.LocalRecitation
import com.quranapp.android.compose.components.reader.QuranTextWbw
import com.quranapp.android.compose.components.reader.ReaderProvider
import com.quranapp.android.compose.components.reader.TextStyleProvider
import com.quranapp.android.compose.components.settings.DailyReminderSheet
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.compose.theme.tightTextStyle
import com.quranapp.android.utils.mediaplayer.WbwAudioPlayer
import com.quranapp.android.utils.reader.ComposeUiConfig
import com.quranapp.android.utils.reader.LocalVerseActions
import com.quranapp.android.utils.reader.QuranTextStyleParams
import com.quranapp.android.utils.reader.atlas.LocalQuranAtlasBundle
import com.quranapp.android.utils.reader.factory.ReaderFactory
import com.quranapp.android.utils.reader.getQuranTextStyle
import com.quranapp.android.viewModels.VotdViewModel

@Composable
fun VerseOfTheDay(
    vm: VotdViewModel = viewModel(),
) {
    ReaderProvider {
        HomePremiumBannerContainer {
            VotdContent(
                header = {
                    HomePremiumHeaderPill(
                        icon = R.drawable.dr_icon_heart_filled,
                        title = stringResource(R.string.strTitleVOTD)
                    )
                },
                vm = vm,
            )
        }
    }
}

@Composable
internal fun VotdContent(
    header: @Composable () -> Unit,
    vm: VotdViewModel = viewModel(),
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    val readerVm = LocalReaderViewModel.current
    val atlasBundle = LocalQuranAtlasBundle.current
    val verseActions = LocalVerseActions.current
    val uiState by vm.uiState.collectAsStateWithLifecycle()

    val verseUi = uiState.verseUi
    val verse = verseUi?.verse
    val arabicEnabled = uiState.arabicEnabled
    val arabicTextMultiplier = uiState.arabicTextMultiplier
    val scriptCode = uiState.scriptCode
    val votdEnabled = uiState.votdReminderEnabled

    LaunchedEffect(colors, type, density, verseActions, atlasBundle) {
        vm.observeVerseUi(
            uiConfig = ComposeUiConfig(
                context = context,
                textMeasurer = textMeasurer,
                density = density,
                colors = colors,
                type = type,
            ),
            verseActions = verseActions,
            atlasBundle = atlasBundle,
        )
    }

    var showDailyReminderSheet by rememberSaveable { mutableStateOf(false) }

    DailyReminderSheet(
        isOpen = showDailyReminderSheet,
        onClose = {
            showDailyReminderSheet = false
        },
    )

    if (verse == null) {
        VerseOfTheDayLoading()
        return
    }

    val isBookmarked by readerVm.userRepository.isBookmarkedFlow(
        verse.chapterNo,
        verse.verseNo..verse.verseNo
    ).collectAsStateWithLifecycle(false)

    val iconTint = Color.White.alpha(0.7f)

    val recState = LocalRecitation.current
    val isVersePlaying = recState.isAnyPlaying && recState.playingVerse.doesEqual(verse)

    LaunchedEffect(verse) {
        WbwAudioPlayer.warmUp(context)
    }

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            header()
        }

        if (arabicEnabled) {
            val quranTextStyle = remember(
                context,
                colors,
                type,
                scriptCode,
                verse.pageNo,
                arabicTextMultiplier,
            ) {
                getQuranTextStyle(
                    QuranTextStyleParams(
                        context = context,
                        fontResolver = readerVm.fontResolver,
                        colors = colors,
                        type = type,
                        pageNo = verse.pageNo,
                        script = scriptCode,
                        sizeMultiplier = arabicTextMultiplier,
                        useSmallSize = true,
                        isDark = true
                    )
                ).copy(
                    color = Color.White
                )
            }

            TextStyleProvider(
                mapOf(
                    verse.pageNo to quranTextStyle
                )
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    QuranTextWbw(
                        modifier = Modifier
                            .padding(horizontal = 16.dp),
                        verseUi = verseUi,
                        horizontalArrangement = Arrangement.Center,
                    )
                }
            }
        }

        val translationText = verseUi.parsedTranslationTexts.firstOrNull()?.second

        if (!translationText.isNullOrBlank()) {
            SelectionContainer {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    text = translationText,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp),
            color = Color.White.alpha(0.2f)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(
                painter = if (isVersePlaying) painterResource(R.drawable.ic_pause)
                else painterResource(R.drawable.ic_play),
                contentDescription = if (isVersePlaying) stringResource(R.string.strLabelPause)
                else stringResource(R.string.strLabelPlay),
                tint = if (isVersePlaying) colorScheme.primary else iconTint,
                small = true,
                onClick = { recState.controller.playControl(ChapterVersePair(verse)) }
            )

            IconButton(
                painter = painterResource(if (isBookmarked) R.drawable.ic_bookmark_added else R.drawable.ic_bookmark),
                contentDescription = stringResource(R.string.strLabelBookmark),
                tint = if (isBookmarked) colorScheme.primary else iconTint,
                small = true,
                onClick = {
                    verseActions.onBookmarkRequest?.invoke(
                        verseUi.verse.chapterNo,
                        verseUi.verse.verseNo..verseUi.verse.verseNo
                    )
                }
            )

            IconButton(
                painter = painterResource(if (votdEnabled) R.drawable.ic_bell_ring else R.drawable.ic_bell),
                contentDescription = stringResource(R.string.dailyReminderMsg),
                tint = if (votdEnabled) colorScheme.primary else iconTint,
                small = true,
                onClick = { showDailyReminderSheet = true }
            )

            Spacer(Modifier.weight(1f))

            FilledTonalButton(
                modifier = Modifier.height(28.dp),
                onClick = { ReaderFactory.startVerse(context, verse.chapterNo, verse.verseNo) },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
            ) {
                Text(
                    stringResource(R.string.strLabelRead),
                    style = typography.labelMedium
                )
            }
        }
    }
}

@Composable
internal fun HomePremiumBannerContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = colorScheme
    val gradient = remember(colors) {
        Brush.linearGradient(
            colors = listOf(
                Color.Black,
                colors.primary,
                Color.Black,
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(shapes.medium)
                .animateContentSize()
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(gradient)
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.alpha(0.8f))
            )

            content()
        }
    }
}

@Composable
internal fun HomePremiumHeaderPill(
    modifier: Modifier = Modifier,
    icon: Int? = null,
    title: String,
    isSelected: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val alpha = if (isSelected) 0.12f else 0f
    val contentAlpha = if (isSelected) 1f else 0.6f

    Surface(
        modifier = modifier.clip(RoundedCornerShape(999.dp)),
        shape = RoundedCornerShape(999.dp),
        color = Color.White.copy(alpha = alpha),
        onClick = onClick ?: {},
        enabled = onClick != null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = contentAlpha),
                    modifier = Modifier.size(16.dp),
                )
            }

            Text(
                text = title,
                style = typography.titleSmall.merge(tightTextStyle),
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = contentAlpha),
            )
        }
    }
}


@Composable
private fun VerseOfTheDayLoading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Loader()
    }
}
