package com.quranapp.android.compose.components.reader

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranapp.android.components.quran.subcomponents.Verse
import com.quranapp.android.compose.components.common.Loader
import com.quranapp.android.reader_managers.ReaderParams
import com.quranapp.android.viewModels.ReaderUiState
import com.quranapp.android.viewModels.ReaderViewModel

data class QuranPageSectionItem(
    val chapterNo: Int,
    val showBismillah: Boolean,
    val verses: List<Verse>,
)

data class QuranPageItem(
    val pageNo: Int,
    val juzNo: Int,
    val sections: List<QuranPageSectionItem>,
    val chapterRange: IntRange,
    val chaptersName: String,
) {
    val verseRanges: Map<Int, IntRange> by lazy {
        HashMap<Int, IntRange>().apply {
            for (section in sections) {
                if (section.verses.isNotEmpty()) {
                    this[section.chapterNo] =
                        section.verses.first().verseNo..section.verses.last().verseNo
                }
            }
        }
    }

    fun hasChapter(chapterNo: Int): Boolean {
        return chapterNo in chapterRange
    }

    fun hasVerse(chapterNo: Int, verseNo: Int): Boolean {
        return hasChapter(chapterNo) && verseRanges.values.any { verseNo in it }
    }
}

sealed class ReaderLayoutItem(var key: String? = null) {
    data object ChapterInfo : ReaderLayoutItem()
    data object Bismillah : ReaderLayoutItem()
    data class ChapterTitle(val chapterNo: Int) : ReaderLayoutItem()
    data class VerseUI(val verse: Verse) : ReaderLayoutItem()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReaderLayout(
    readerVm: ReaderViewModel,
) {
    val uiState by readerVm.uiState.collectAsStateWithLifecycle()
    val readerMode by readerVm.readerMode.collectAsState()

    if (uiState.loading || readerMode == null) {
        return Loader(fill = true)
    }

    when (uiState.transientReaderMode ?: readerMode) {
        ReaderParams.READER_STYLE_PAGE -> ReaderLayoutPageMode(readerVm)
        else -> ReaderLayoutTranslationMode(readerVm, uiState)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReaderLayoutTranslationMode(
    readerVm: ReaderViewModel,
    uiState: ReaderUiState,
) {
    val items by readerVm.translationViewItems
    val slugs by readerVm.slugs.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 240.dp)
    ) {
        items(
            items = items,
            key = { item -> item.key ?: "" },
        ) { item ->
            TranslationRow(readerVm, item, uiState.transientTranslationSlugs ?: slugs)
        }
    }
}

@Composable
private fun TranslationRow(
    readerVm: ReaderViewModel,
    item: ReaderLayoutItem,
    slugs: Set<String>?
) {
    if (slugs == null) return

    when (item) {
        ReaderLayoutItem.ChapterInfo -> {
            Text(
                text = "Chapter info",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        is ReaderLayoutItem.ChapterTitle -> {
            Text(
                text = "Surah ${item.chapterNo}",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }

        ReaderLayoutItem.Bismillah -> {
            Text(
                text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }

        is ReaderLayoutItem.VerseUI -> {
            VerseView(
                verse = item.verse,
                slugs = slugs
            )
        }
    }
}

@Composable
private fun ReaderLayoutPageMode(
    readerVm: ReaderViewModel,
) {

}

@Composable
private fun PageModePage(page: QuranPageItem) {

}
