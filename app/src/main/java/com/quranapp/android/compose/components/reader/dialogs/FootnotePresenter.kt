package com.quranapp.android.compose.components.reader.dialogs

import ThemeUtils
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.alfaazplus.sunnah.ui.theme.fontUrdu
import com.quranapp.android.R
import com.quranapp.android.components.quran.subcomponents.Footnote
import com.quranapp.android.compose.components.common.Chip
import com.quranapp.android.compose.extensions.bottomBorder
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.db.relations.VerseWithDetails
import com.quranapp.android.utils.reader.LocalVerseActions
import com.quranapp.android.utils.reader.OnReferenceClick
import com.quranapp.android.utils.reader.TranslationTextStyleParams
import com.quranapp.android.utils.reader.VerseActions
import com.quranapp.android.utils.reader.buildTranslationAnnotatedString
import com.quranapp.android.utils.reader.factory.QuranTranslationFactory
import com.quranapp.android.utils.reader.getTranslationTextStyle
import com.quranapp.android.utils.univ.ResUtils
import com.quranapp.android.utils.univ.StringUtils
import horizontalFadingEdge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import verticalFadingEdge
import java.util.Locale


data class FootnotePresenterData(
    val verse: VerseWithDetails,
    val singleFootnote: Footnote?,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FootnotePresenter(
    data: FootnotePresenterData?,
    onClose: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(true)
    if (data == null) return

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        scrimColor = colorScheme.scrim.alpha(0.5f),
        containerColor = colorScheme.surface,
        contentColor = colorScheme.onSurface,
        contentWindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom) },
    ) {
        PresentSheetContent(data)
    }
}

@Composable
private fun PresentSheetContent(data: FootnotePresenterData) {
    val context = LocalContext.current
    val translFactory = QuranTranslationFactory.remember(context)

    val verse = data.verse
    val singleFootnote = data.singleFootnote

    var selectedSlug by rememberSaveable {
        mutableStateOf(verse.translations.firstOrNull { it.getFootnotesCount() > 0 }?.bookSlug)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth(),
    ) {
        Header(translFactory, verse, singleFootnote)

        if (singleFootnote == null) {
            AuthorChips(
                translFactory,
                verse,
                selectedSlug,
            ) {
                selectedSlug = it
            }
        }

        FootnoteContent(
            translFactory,
            selectedSlug,
            verse,
            singleFootnote
        )
    }
}

@Composable
private fun Header(
    translFactory: QuranTranslationFactory,
    verse: VerseWithDetails,
    singleFootnote: Footnote?
) {
    val context = LocalContext.current

    val singleFootnoteBooInfo = remember(singleFootnote, translFactory) {
        singleFootnote?.let {
            translFactory.getTranslationBookInfo(it.bookSlug)
        }
    }

    val title = singleFootnoteBooInfo?.let {
        ResUtils.getLocalizedString(
            context,
            R.string.strTitleFootnote,
            Locale.forLanguageTag(it.langCode)
        )
    } ?: if (singleFootnote != null) stringResource(R.string.strTitleFootnote)
    else stringResource(R.string.strTitleFootnotes)

    val repository = remember(context) { DatabaseProvider.getQuranRepository(context) }
    var chapterName by remember { mutableStateOf("") }

    LaunchedEffect(verse.chapterNo) {
        chapterName = withContext(Dispatchers.IO) {
            repository.getChapterName(verse.chapterNo)
        }
    }


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .bottomBorder()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        color = colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append(title)
                }

                if (singleFootnote != null) {
                    append(" ")
                    append(singleFootnote.number.toString())
                }
            }
        )

        Text(
            text = buildAnnotatedString {
                append(
                    stringResource(
                        R.string.strLabelVerseSerialWithChapter,
                        chapterName,
                        verse.chapterNo,
                        verse.verseNo
                    )
                )

                if (singleFootnoteBooInfo != null) {
                    append(" ${StringUtils.HYPHEN} ")
                    append(singleFootnoteBooInfo.getDisplayName(true))
                }
            },
            style = typography.labelSmall
        )
    }
}

@Composable
fun AuthorChips(
    translFactory: QuranTranslationFactory,
    verse: VerseWithDetails,
    selectedSlug: String?,
    onSelectionChange: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val translations = verse.translations
    val slugs = translations.map { it.bookSlug }.toSet()
    val booksInfo = remember {
        translFactory.getTranslationBooksInfoValidated(slugs)
    }

    Box(
        modifier = Modifier.horizontalFadingEdge(scrollState, color = colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            verse.translations.forEach { translation ->
                if (translation.getFootnotesCount() > 0) {
                    val slug = translation.bookSlug
                    val bookInfo = booksInfo[slug] ?: return@forEach

                    Chip(
                        modifier = Modifier.height(48.dp),
                        selected = slug == selectedSlug,
                        onClick = {
                            onSelectionChange(slug)
                        },
                        label = {
                            Text(
                                bookInfo.getDisplayName(true),
                                fontFamily = if (bookInfo.isUrdu) fontUrdu else null
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun FootnoteContent(
    translFactory: QuranTranslationFactory,
    selectedSlug: String?,
    verse: VerseWithDetails,
    singleFootnote: Footnote?,
) {
    val scrollState = rememberScrollState()
    val textSizeMultiplier = ReaderPreferences.observeTranlationTextSizeMultiplier()
    val isDark = ThemeUtils.observeDarkTheme()

    val footnotes = remember(selectedSlug, translFactory) {
        selectedSlug?.let {
            translFactory.getFootnotesSingleVerse(
                it, verse.chapterNo,
                verse.verseNo
            )
        } ?: emptyMap()
    }

    val verseActions = LocalVerseActions.current
    val onReferenceClick: OnReferenceClick = { slugs, chapterNo, verses ->
        verseActions.onReferenceClick(slugs, chapterNo, verses)
    }

    SelectionContainer {
        Box(
            modifier = Modifier.verticalFadingEdge(
                scrollState,
                color = colorScheme.surface,
                length = 48.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (singleFootnote != null) {
                    Text(
                        buildTranslationAnnotatedString(
                            text = singleFootnote.text,
                            slug = selectedSlug ?: "",
                            colorScheme,
                            actions = VerseActions(onReferenceClick)
                        ),
                        color = if (isDark) colorScheme.onSurface.alpha(0.8f) else colorScheme.onSurface,
                        style = getTranslationTextStyle(
                            TranslationTextStyleParams(
                                slug = singleFootnote.bookSlug,
                                sizeMultiplier = textSizeMultiplier
                            )
                        )
                    )
                } else {
                    footnotes.forEach { (number, footnote) ->
                        Text(
                            buildAnnotatedString {
                                withStyle(
                                    SpanStyle(
                                        color = colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                ) {
                                    append("\u2066$number.\u2069\u00A0")
                                }

                                append(
                                    buildTranslationAnnotatedString(
                                        text = footnote.text,
                                        slug = selectedSlug ?: "",
                                        colorScheme,
                                        actions = VerseActions(onReferenceClick)
                                    )
                                )
                            },
                            color = if (isDark) colorScheme.onSurface.alpha(0.8f) else colorScheme.onSurface,
                            style = getTranslationTextStyle(
                                TranslationTextStyleParams(
                                    slug = selectedSlug ?: "",
                                    sizeMultiplier = textSizeMultiplier
                                )
                            )
                        )
                    }
                }
            }
        }
    }
}
