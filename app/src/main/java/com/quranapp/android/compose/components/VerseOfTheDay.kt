package com.quranapp.android.compose.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranapp.android.R
import com.quranapp.android.api.models.translation.TranslationBookInfoModel
import com.quranapp.android.components.quran.subcomponents.Translation
import com.quranapp.android.compose.components.common.Loader
import com.quranapp.android.compose.screens.reader.BookmarkViewerData
import com.quranapp.android.compose.screens.reader.BookmarkViewerSheet
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.db.relations.VerseWithDetails
import com.quranapp.android.utils.reader.FontResolver
import com.quranapp.android.utils.reader.QuranTextStyleParams
import com.quranapp.android.utils.reader.TranslUtils
import com.quranapp.android.utils.reader.TranslationTextStyleParams
import com.quranapp.android.utils.reader.factory.QuranTranslationFactory
import com.quranapp.android.utils.reader.factory.ReaderFactory
import com.quranapp.android.utils.reader.getQuranTextStyle
import com.quranapp.android.utils.reader.getTranslationTextStyle
import com.quranapp.android.utils.univ.StringUtils
import com.quranapp.android.utils.verse.VerseUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class VerseOfTheDayState(
    val verse: VerseWithDetails,
    val translation: Translation?,
    val translationBook: TranslationBookInfoModel?,
)

@Composable
fun VerseOfTheDay(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val colors = colorScheme
    val type = typography

    val repository = remember(context) { DatabaseProvider.getQuranRepository(context) }
    val userRepository = remember(context) { DatabaseProvider.getUserRepository(context) }
    val translationFactory = QuranTranslationFactory.remember(context)
    val fontResolver = FontResolver.remember()

    val arabicEnabled = ReaderPreferences.observeArabicTextEnabled()
    val arabicTextMultiplier = ReaderPreferences.observeArabicTextSizeMultiplier()
    val translationTextMultiplier = ReaderPreferences.observeTranlationTextSizeMultiplier()
    val scriptCode = ReaderPreferences.observeQuranScript()
    val translationSlugs = ReaderPreferences.observeTranslations()

    var bookmarkViewerData by remember { mutableStateOf<BookmarkViewerData?>(null) }

    val state by produceState<VerseOfTheDayState?>(
        initialValue = null,
        repository,
        translationFactory,
        scriptCode,
        translationSlugs,
    ) {
        value = withContext(Dispatchers.IO) {
            val verse = VerseUtils.getVOTD(context, repository)
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

    BookmarkViewerSheet(bookmarkViewerData) {
        bookmarkViewerData = null
    }

    val votdState = state
    if (fontResolver == null || votdState == null) {
        VerseOfTheDayLoading(modifier)
        return
    }

    val verse = votdState.verse
    val isBookmarked by userRepository.isBookmarkedFlow(
        verse.chapterNo,
        verse.verseNo..verse.verseNo
    ).collectAsStateWithLifecycle(false)

    val quranTextStyle = remember(
        context,
        colors,
        type,
        fontResolver,
        scriptCode,
        verse.pageNo,
        arabicTextMultiplier
    ) {
        getQuranTextStyle(
            QuranTextStyleParams(
                context = context,
                fontResolver = fontResolver,
                colors = colors,
                type = type,
                pageNo = verse.pageNo,
                script = scriptCode,
                sizeMultiplier = arabicTextMultiplier,
                useSmallSize = true,
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

    val translationText = remember(votdState.translation?.text) {
        votdState.translation?.text?.let { StringUtils.removeHTML(it, false) }.orEmpty()
    }

    val translationAuthor = remember(votdState.translationBook) {
        votdState.translationBook?.getDisplayName(true)?.takeIf { it.isNotBlank() }
    }

    val gradient = remember(colors) {
        Brush.linearGradient(
            colors = listOf(
                colors.primary.copy(alpha = 0.24f),
                colors.secondaryContainer.copy(alpha = 0.72f),
                colors.surfaceContainerHigh.copy(alpha = 0.98f),
            )
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface)
            .padding(12.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface,
        ),
    ) {
        Column(
            modifier = Modifier
                .background(gradient)
                .animateContentSize(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = colorScheme.primary.copy(alpha = 0.12f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.dr_icon_heart_filled),
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = stringResource(R.string.strTitleVOTD),
                            style = typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurface,
                        )
                    }
                }

                FilledTonalButton(
                    modifier = Modifier.height(28.dp),
                    onClick = { ReaderFactory.startVerse(context, verse.chapterNo, verse.verseNo) },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                ) {
                    Text(
                        stringResource(R.string.strLabelRead),
                        style = type.labelMedium
                    )
                }
            }

            if (arabicEnabled && arabicText.isNotBlank()) {
                Text(
                    text = arabicText,
                    style = quranTextStyle,
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
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = translationText,
                            style = translationTextStyle,
                            color = colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )

                        if (!translationAuthor.isNullOrBlank()) {
                            Text(
                                text = "${StringUtils.HYPHEN} $translationAuthor",
                                style = translationTextStyle.copy(
                                    fontStyle = FontStyle.Italic,
                                    fontSize = type.labelMedium.fontSize
                                ),
                                color = colorScheme.onSurface.alpha(0.6f),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = 8.dp),
                color = colorScheme.outlineVariant.alpha(0.7f)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(
                        R.string.strLabelVerseSerialWithChapter,
                        votdState.verse.chapter.getCurrentName(),
                        verse.chapterNo,
                        verse.verseNo
                    ),
                    modifier = Modifier.weight(1f),
                    style = typography.labelMedium,
                    color = colorScheme.onSurface,
                )

                IconButton(
                    onClick = {
                        ReaderFactory.startTafsir(
                            context,
                            verse.chapterNo,
                            verse.verseNo
                        )
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.dr_icon_tafsir),
                        contentDescription = stringResource(R.string.strTitleTafsir),
                        tint = null,
                    )
                }

                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            if (isBookmarked) {
                                bookmarkViewerData = BookmarkViewerData(
                                    chapterNo = verse.chapterNo,
                                    fromVerse = verse.verseNo,
                                    toVerse = verse.verseNo,
                                    showOpenInReaderButton = false,
                                )
                            } else {
                                userRepository.addToBookmark(
                                    chapterNo = verse.chapterNo,
                                    verseRange = verse.verseNo..verse.verseNo,
                                    note = null,
                                )

                                bookmarkViewerData = BookmarkViewerData(
                                    chapterNo = verse.chapterNo,
                                    fromVerse = verse.verseNo,
                                    toVerse = verse.verseNo,
                                    showOpenInReaderButton = false,
                                    startInEditMode = true,
                                )
                            }
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(
                            if (isBookmarked) {
                                R.drawable.dr_icon_bookmark_added
                            } else {
                                R.drawable.dr_icon_bookmark_outlined
                            }
                        ),
                        contentDescription = null,
                        tint = if (isBookmarked) {
                            colorScheme.primary
                        } else {
                            colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun VerseOfTheDayLoading(
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceContainer,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Loader()
        }
    }
}
