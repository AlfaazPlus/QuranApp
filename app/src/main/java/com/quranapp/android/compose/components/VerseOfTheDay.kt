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
import androidx.compose.material3.MaterialTheme.shapes
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alfaazplus.sunnah.ui.utils.shared_preference.DataStoreManager
import com.quranapp.android.R
import com.quranapp.android.api.models.translation.TranslationBookInfoModel
import com.quranapp.android.components.quran.subcomponents.Translation
import com.quranapp.android.compose.components.common.Loader
import com.quranapp.android.compose.components.settings.DailyReminderSheet
import com.quranapp.android.compose.components.reader.dialogs.BookmarkViewerData
import com.quranapp.android.compose.components.reader.dialogs.BookmarkViewerSheet
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.compose.utils.preferences.VersePreferences
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
    val translationFactory = QuranTranslationFactory.remember(context)
    val fontResolver = FontResolver.remember()

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

    var bookmarkViewerData by remember { mutableStateOf<BookmarkViewerData?>(null) }
    var showDailyReminderSheet by remember { mutableStateOf(false) }

    BookmarkViewerSheet(bookmarkViewerData) {
        bookmarkViewerData = null
    }

    DailyReminderSheet(
        isOpen = showDailyReminderSheet,
        onClose = {
            showDailyReminderSheet = false
        },
    )

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

    val votdState = state
    if (fontResolver == null || votdState == null) {
        VerseOfTheDayLoading(modifier)
        return
    }

    val userRepository = remember(context) { DatabaseProvider.getUserRepository(context) }
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
                    .background(Color.Black.alpha(0.5f))
            )

            Column() {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color.White.copy(alpha = 0.12f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.dr_icon_heart_filled),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = stringResource(R.string.strTitleVOTD),
                                style = typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                            )
                        }
                    }

                    FilledTonalButton(
                        modifier = Modifier.height(28.dp),
                        onClick = {
                            ReaderFactory.startVerse(
                                context,
                                verse.chapterNo,
                                verse.verseNo
                            )
                        },
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
                        style = quranTextStyle.copy(
                            color = Color.White
                        ),
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
                                color = Color.White,
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
                                    color = Color.White.alpha(0.6f),
                                    textAlign = TextAlign.Center,
                                )
                            }
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
                        style = typography.labelMedium.copy(
                            color = Color.White.alpha(0.75f),
                            fontWeight = FontWeight.Normal
                        ),
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
                            modifier = Modifier.size(22.dp),
                            tint = null,
                        )
                    }

                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                if (!isBookmarked) {
                                    userRepository.addToBookmark(
                                        chapterNo = verse.chapterNo,
                                        verseRange = verse.verseNo..verse.verseNo,
                                        note = null,
                                    )
                                }

                                bookmarkViewerData = BookmarkViewerData(
                                    chapterNo = verse.chapterNo,
                                    fromVerse = verse.verseNo,
                                    toVerse = verse.verseNo,
                                    startInEditMode = !isBookmarked,
                                )
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
                            modifier = Modifier.size(23.dp),
                            tint = if (isBookmarked) colorScheme.primary else {
                                Color.White.alpha(0.7f)
                            },
                        )
                    }

                    IconButton(
                        onClick = {
                            showDailyReminderSheet = true
                        }
                    ) {
                        Icon(
                            painter = painterResource(
                                if (votdEnabled) R.drawable.ic_bell_ring else R.drawable.ic_bell
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (votdEnabled) colorScheme.primary else {
                                Color.White.alpha(0.7f)
                            },
                        )
                    }
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
