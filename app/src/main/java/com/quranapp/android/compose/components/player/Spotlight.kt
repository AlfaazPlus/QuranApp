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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.quranapp.android.components.quran.Quran2
import com.quranapp.android.components.quran.subcomponents.Verse
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.compose.utils.preferences.ReaderPreferences

@Composable
fun SpotlightVersePanel(
    versePair: ChapterVersePair,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val slugs = ReaderPreferences.observeTranslations()
    val chapterNo = versePair.chapterNo
    val verseNo = versePair.verseNo

    val verse by produceState<Verse?>(
        initialValue = null,
        chapterNo,
        verseNo,
    ) {
        if (!versePair.isValid) {
            value = null
            return@produceState
        }

        val quran = Quran2.prepareInstance(context)

        value = quran.getVerse(chapterNo, verseNo)

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
                SpotlightTranslationText(v, slugs.firstOrNull()?.let { setOf(it) } ?: setOf())
            }
        }
    }
}


@Composable
private fun SpotlightQuranText(
    verse: Verse,
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
    verse: Verse,
    slugs: Set<String>,
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
