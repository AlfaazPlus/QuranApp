package com.quranapp.android.compose.components.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.db.relations.VerseWithDetails
import com.quranapp.android.utils.reader.TranslUtils
import com.quranapp.android.utils.reader.factory.QuranTranslationFactory

@Composable
fun SpotlightVersePanel(
    versePair: ChapterVersePair,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val slugs = ReaderPreferences.observeTranslations()
    val scriptCode = ReaderPreferences.observeQuranScript()
    val chapterNo = versePair.chapterNo
    val verseNo = versePair.verseNo

    val repository = remember(context) { DatabaseProvider.getQuranRepository(context) }
    val factory = QuranTranslationFactory.remember(context)

    val verse by produceState<VerseWithDetails?>(
        initialValue = null,
        repository,
        factory,
        chapterNo,
        verseNo,
        scriptCode,
        slugs
    ) {
        if (!versePair.isValid) {
            value = null
            return@produceState
        }

        val ayah = repository.getAyah(chapterNo, verseNo)
        val surah = repository.getSurahWithLocalizations(chapterNo)

        if (ayah == null || surah == null) {
            value = null
            return@produceState
        }

        val words = repository.getWordsForAyah(chapterNo, verseNo, scriptCode)

        val aSlug = slugs.firstOrNull() ?: TranslUtils.TRANSL_SLUG_DEFAULT

        value = VerseWithDetails(
            words = words,
            pageNo = 0,
            verse = ayah,
            chapter = surah
        ).apply {
            this.translations = factory.getTranslationsSingleVerse(
                setOf(aSlug),
                chapterNo,
                verseNo
            )
        }
    }

    val scroll = rememberScrollState()

    Box(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (val v = verse) {
            null -> CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                color = colorScheme.primary,
                strokeWidth = 3.dp,
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scroll),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                SpotlightQuranText(v)
                SpotlightTranslationText(v)
            }
        }
    }
}


@Composable
private fun SpotlightQuranText(
    vwd: VerseWithDetails,
) {
    /*val style = rememberQuranTextStyle(verse.pageNo)

    Text(
        text = buildAnnotatedString {
            verse.segments.forEachIndexed { index, word ->
                append(word)

                if (index != verse.segments.lastIndex) {
                    append(" ")
                }
            }

            if (!verse.endText.isNullOrEmpty()) {
                append("  " + verse.endText)
            }
        },
        style = style,
        modifier = Modifier.padding(8.dp),
        textAlign = TextAlign.Center
    )*/
}


@Composable
private fun SpotlightTranslationText(
    vwd: VerseWithDetails,
) {
    /*val viewModel = viewModel<VerseViewModel>()
    val translations = remember(slugs, verse.chapterNo, verse.verseNo) {
        viewModel.translationFactory.getTranslationsSingleVerse(
            slugs,
            verse.chapterNo,
            verse.verseNo,
        )
    }

    SelectionContainer {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            translations.forEach { translation ->
                Text(
                    buildAnnotatedString {
                        append(
                            buildTranslationAnnotatedString(
                                parseTranslationText(
                                    translation.text,
                                    translation.bookSlug
                                ).filter { it is RichTextPart.Plain },
                                colorScheme,
                                actions = null
                            )
                        )
                    },
                    style = rememberTranslationTextStyle(translation.bookSlug),
                    modifier = Modifier.fillMaxWidth(),
                    color = PlayerContentColor.alpha(0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }*/
}
