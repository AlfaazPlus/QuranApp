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
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alfaazplus.sunnah.ui.theme.tightTextStyle
import com.alfaazplus.sunnah.ui.utils.shared_preference.DataStoreManager
import com.quranapp.android.R
import com.quranapp.android.api.models.translation.TranslationBookInfoModel
import com.quranapp.android.components.quran.subcomponents.Translation
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.compose.components.common.IconButton
import com.quranapp.android.compose.components.common.Loader
import com.quranapp.android.compose.components.reader.LocalRecitation
import com.quranapp.android.compose.components.reader.LocalWbwState
import com.quranapp.android.compose.components.reader.ReaderProvider
import com.quranapp.android.compose.components.settings.DailyReminderSheet
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.compose.utils.preferences.VersePreferences
import com.quranapp.android.db.relations.VerseWithDetails
import com.quranapp.android.utils.reader.LocalVerseActions
import com.quranapp.android.utils.reader.QuranTextStyleParams
import com.quranapp.android.utils.reader.TranslUtils
import com.quranapp.android.utils.reader.TranslationTextStyleParams
import com.quranapp.android.utils.reader.VerseActions
import com.quranapp.android.utils.reader.buildTranslationAnnotatedString
import com.quranapp.android.utils.reader.factory.QuranTranslationFactory
import com.quranapp.android.utils.reader.factory.ReaderFactory
import com.quranapp.android.utils.reader.getQuranTextStyle
import com.quranapp.android.utils.reader.getTranslationTextStyle
import com.quranapp.android.utils.verse.VerseUtils
import com.quranapp.android.viewModels.ReaderViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class VerseOfTheDayState(
    val verse: VerseWithDetails,
    val translation: Translation?,
    val translationBook: TranslationBookInfoModel?,
)

@Composable
fun VerseOfTheDay() {
    ReaderProvider {
        HomePremiumBannerContainer {
            VotdContent(
                header = {
                    HomePremiumHeaderPill(
                        icon = R.drawable.dr_icon_heart_filled,
                        title = stringResource(R.string.strTitleVOTD)
                    )
                }
            )
        }
    }
}

@Composable
internal fun VotdContent(
    header: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colors = colorScheme
    val type = typography

    val vm = viewModel<ReaderViewModel>()
    val verseActions = LocalVerseActions.current

    val prefs by DataStoreManager.flowMultiple(
        ReaderPreferences.KEY_ARABIC_TEXT_ENABLED,
        ReaderPreferences.KEY_TEXT_SIZE_MULT_ARABIC,
        ReaderPreferences.KEY_TEXT_SIZE_MULT_TRANSL,
        ReaderPreferences.KEY_SCRIPT,
        ReaderPreferences.KEY_TRANSLATIONS,
        VersePreferences.KEY_VOTD_REMINDER_ENABLED,
    ).collectAsStateWithLifecycle(null)

    val preferences = prefs ?: return

    val arabicEnabled = preferences.get(ReaderPreferences.KEY_ARABIC_TEXT_ENABLED)
    val arabicTextMultiplier = preferences.get(ReaderPreferences.KEY_TEXT_SIZE_MULT_ARABIC)
    val translationTextMultiplier = preferences.get(ReaderPreferences.KEY_TEXT_SIZE_MULT_TRANSL)
    val scriptCode = preferences.get(ReaderPreferences.KEY_SCRIPT)
    val translationSlugs = preferences.get(ReaderPreferences.KEY_TRANSLATIONS)
    val votdEnabled = preferences.get(VersePreferences.KEY_VOTD_REMINDER_ENABLED)

    var showDailyReminderSheet by remember { mutableStateOf(false) }

    DailyReminderSheet(
        isOpen = showDailyReminderSheet,
        onClose = {
            showDailyReminderSheet = false
        },
    )

    val state by produceState<VerseOfTheDayState?>(
        initialValue = null,
        scriptCode,
        translationSlugs,
    ) {
        val translationFactory = QuranTranslationFactory(context)
        value = withContext(Dispatchers.IO) {
            val verse = VerseUtils.getVOTD(context, vm.repository)
                ?: return@withContext null

            val optimalSlug = translationSlugs.firstOrNull { !TranslUtils.isTransliteration(it) }
                ?: TranslUtils.TRANSL_SLUG_DEFAULT

            val translation = translationFactory.getTranslationsSingleVerse(
                setOf(optimalSlug),
                verse.chapterNo,
                verse.verseNo
            ).firstOrNull()

            val translationBook = translation?.bookSlug
                ?.takeIf { it.isNotBlank() }
                ?.let(translationFactory::getTranslationBookInfo)

            VerseOfTheDayState(
                verse = verse,
                translation = translation,
                translationBook = translationBook,
            )
        }
    }

    val votdState = state

    if (votdState == null) {
        VerseOfTheDayLoading()
        return
    }

    val verse = votdState.verse
    val isBookmarked by vm.userRepository.isBookmarkedFlow(
        verse.chapterNo,
        verse.verseNo..verse.verseNo
    ).collectAsStateWithLifecycle(false)

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
                fontResolver = vm.fontResolver,
                colors = colors,
                type = type,
                pageNo = verse.pageNo,
                script = scriptCode,
                sizeMultiplier = arabicTextMultiplier,
                useSmallSize = true,
                isDark = true
            )
        )
    }

    val translationTextStyle = remember(
        votdState.translation?.bookSlug,
        translationTextMultiplier
    ) {
        getTranslationTextStyle(
            TranslationTextStyleParams(
                slug = votdState.translation?.bookSlug.orEmpty(),
                sizeMultiplier = translationTextMultiplier,
            )
        )
    }

    val arabicText = remember(verse.words) {
        verse.words.joinToString(" ") { it.text }
    }

    val translationText = remember(votdState.translation, verse, verseActions, colors) {
        val translation = votdState.translation ?: return@remember buildAnnotatedString { }

        buildTranslationAnnotatedString(
            translation = translation,
            colorScheme = colors,
            actions = VerseActions(
                onReferenceClick = verseActions.onReferenceClick,
                onFootnoteClickRaw = { _, footnoteNo ->
                    verseActions.onFootnoteClick?.invoke(
                        verse,
                        translation.footnotes[footnoteNo],
                    )
                },
            ),
        )
    }


    val iconTint = Color.White.alpha(0.7f)

    val recState = LocalRecitation.current
    val isVersePlaying = recState.isAnyPlaying && recState.playingVerse.doesEqual(verse)

    val wbwState = LocalWbwState.current
    LaunchedEffect(verse) {
        wbwState.warmUpWord(verse.chapterNo, verse.verseNo, 0)
    }

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            header()
        }

        if (arabicEnabled && arabicText.isNotBlank()) {
            Text(
                text = arabicText,
                style = quranTextStyle.copy(color = Color.White),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        if (translationText.isNotBlank()) {
            SelectionContainer {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = translationText,
                        style = translationTextStyle,
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )

                    Text(
                        text = stringResource(
                            R.string.strLabelVerseSerialWithChapter,
                            votdState.verse.chapter.getCurrentName(),
                            verse.chapterNo,
                            verse.verseNo
                        ),
                        style = translationTextStyle.copy(
                            fontStyle = FontStyle.Italic,
                            fontSize = typography.labelMedium.fontSize
                        ),
                        color = Color.White.alpha(0.6f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(top = 16.dp),
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
                        votdState.verse.chapterNo,
                        votdState.verse.verseNo..votdState.verse.verseNo
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

            IconButton(
                painter = painterResource(R.drawable.dr_icon_tafsir),
                contentDescription = stringResource(R.string.strTitleTafsir),
                tint = null,
                small = true,
                onClick = { ReaderFactory.startTafsir(context, verse.chapterNo, verse.verseNo) }
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
                style = typography.labelMedium.merge(tightTextStyle),
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
