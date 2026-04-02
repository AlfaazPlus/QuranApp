package com.quranapp.android.compose.screens.recitation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.quranapp.android.components.quran.Quran2
import com.quranapp.android.compose.components.reader.VerseView
import com.quranapp.android.compose.components.reader.dialogs.FootnotePresenter
import com.quranapp.android.compose.components.reader.dialogs.FootnotePresenterData
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.utils.reader.LocalVerseActions
import com.quranapp.android.utils.reader.VerseActions

@Composable
fun ReaderTextDemoSection(modifier: Modifier = Modifier) {
    val quran = Quran2.rememberQuran()
    val slugs = ReaderPreferences.observeTranslations()
    val verse = quran?.getVerse(1, 2)

    var footnotePresenterData by remember { mutableStateOf<FootnotePresenterData?>(null) }

    if (verse == null) return

    CompositionLocalProvider(
        LocalVerseActions provides VerseActions(
            onReferenceClick = { slugs, chapterNo, verses ->

            },
            onFootnoteClick = { verse, footnote ->
                footnotePresenterData = FootnotePresenterData(
                    verse,
                    footnote
                )
            }
        )
    ) {
        VerseView(verse, slugs)
    }


    FootnotePresenter(
        data = footnotePresenterData,
    ) {
        footnotePresenterData = null
    }
}
