package com.quranapp.android.compose.components.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alfaazplus.sunnah.ui.theme.fontUrdu
import com.quranapp.android.api.models.translation.TranslationBookInfoModel
import com.quranapp.android.components.quran.subcomponents.Translation
import com.quranapp.android.components.quran.subcomponents.Verse
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.utils.reader.LocalVerseActions
import com.quranapp.android.utils.reader.VerseActions
import com.quranapp.android.utils.reader.buildTranslationAnnotatedString
import com.quranapp.android.utils.reader.translationTextStyle
import com.quranapp.android.viewModels.VerseViewModel

@Composable
fun TranslationText(
    verse: Verse,
    slugs: Set<String>,
) {
    val viewModel = viewModel<VerseViewModel>()
    val verseActions = LocalVerseActions.current
    val translations = remember(slugs, verse.chapterNo, verse.verseNo) {
        viewModel.translationFactory.getTranslationsSingleVerse(
            slugs,
            verse.chapterNo,
            verse.verseNo,
        )
    }

    val booksInfo = remember(slugs) {
        viewModel.translationFactory.getTranslationBooksInfoValidated(slugs)
    }


    SelectionContainer {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            translations.forEach { translation ->
                SingleTranslation(verse, translation, booksInfo, verseActions)
            }
        }
    }
}

@Composable
private fun SingleTranslation(
    verse: Verse,
    translation: Translation,
    booksInfo: Map<String, TranslationBookInfoModel>,
    verseActions: VerseActions
) {
    val colors = colorScheme
    val type = typography

    val annotatedText = remember(
        verse, translation, booksInfo, colors, type
    ) {
        buildAnnotatedString {
            append(
                buildTranslationAnnotatedString(
                    translation,
                    colors,
                    actions = VerseActions(
                        verseActions.onReferenceClick,
                        onFootnoteClickRaw = { slug, footnoteNo ->
                            verseActions.onFootnoteClick?.invoke(
                                verse,
                                translation.footnotes[footnoteNo]
                            )
                        }
                    )
                )
            )

            append("\n")

            booksInfo.get(translation.bookSlug)?.let { bookInfo ->
                withStyle(
                    style = SpanStyle(
                        color = colors.onBackground.alpha(0.6f),
                        fontSize = type.labelMedium.fontSize,
                        fontFamily = if (bookInfo.isUrdu) fontUrdu else null,
                    )
                ) {
                    append(bookInfo.getDisplayName(false))
                }
            }
        }
    }

    Text(
        annotatedText,
        style = translationTextStyle(translation.bookSlug),
        modifier = Modifier.fillMaxWidth()
    )
}