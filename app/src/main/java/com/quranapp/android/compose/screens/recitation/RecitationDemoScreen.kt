package com.quranapp.android.compose.screens.recitation

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.components.quran.Quran2
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.compose.components.player.RecitationPlayerSheet
import com.quranapp.android.compose.components.reader.dialogs.FootnotePresenter
import com.quranapp.android.compose.components.reader.dialogs.FootnotePresenterData
import com.quranapp.android.utils.mediaplayer.RecitationController
import com.quranapp.android.utils.mediaplayer.RecitationServiceState
import com.quranapp.android.utils.reader.factory.QuranTranslationFactory
import com.quranapp.android.utils.sharedPrefs.SPReader

private data class ChapterEntry(val number: Int, val name: String, val verseCount: Int)

private val DEMO_CHAPTERS = listOf(
    ChapterEntry(1, "Al-Fatihah", 7),
    ChapterEntry(2, "Al-Baqarah", 286),
    ChapterEntry(36, "Ya-Sin", 83),
    ChapterEntry(55, "Ar-Rahman", 78),
    ChapterEntry(67, "Al-Mulk", 30),
    ChapterEntry(78, "An-Naba", 40),
    ChapterEntry(112, "Al-Ikhlas", 4),
    ChapterEntry(113, "Al-Falaq", 5),
    ChapterEntry(114, "An-Nas", 6),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecitationDemoScreen() {
    val context = LocalContext.current
    val controller = RecitationController.getInstance(context)
    val state by controller.state.collectAsState()
    val isPlaying by controller.isPlayingState.collectAsState()

    val quran = Quran2.rememberQuran()
    val translFactory = QuranTranslationFactory.rememberFactory(context)
    val verse = quran?.getVerse(1, 2)?.apply {
        translations = translFactory.getTranslationsSingleVerse(
            slugs = SPReader.getSavedTranslations(context),
            chapterNo,
            verseNo
        )
    }


    var footnotePresenterData by remember { mutableStateOf<FootnotePresenterData?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("Recitation Player") },
                    navigationIcon = {
                        val activity = context as? Activity
                        IconButton(onClick = { activity?.finish() }) {
                            Icon(
                                painterResource(R.drawable.dr_icon_arrow_left),
                                contentDescription = "Back",
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                Button(
                    onClick = {
                        if (verse != null) {
                            footnotePresenterData = FootnotePresenterData(
                                verse = verse,
                                singleFootnote = null,
                            )
                        }
                    }
                ) {
                    Text("Open all footnotes")
                }
                Button(
                    onClick = {
                        if (verse != null) {
                            footnotePresenterData = FootnotePresenterData(
                                verse = verse,
                                singleFootnote = verse.translations.get(0).footnotes.get(1),
                            )
                        }
                    }
                ) {
                    Text("Open one footnote")
                }

                ChapterList(
                    state = state,
                    isPlaying = isPlaying,
                    onChapterSelected = { chapter ->
                        controller.start(ChapterVersePair(chapterNo = chapter.number, verseNo = 1))
                    },
                )
            }
        }
        RecitationPlayerSheet()
    }

    FootnotePresenter(
        data = footnotePresenterData,
    ) {
        footnotePresenterData = null
    }
}

@Composable
private fun ChapterList(
    state: RecitationServiceState,
    isPlaying: Boolean,
    onChapterSelected: (ChapterEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeChapter = state.currentVerse.chapterNo

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(DEMO_CHAPTERS, key = { it.number }) { chapter ->
            val isActive = chapter.number == activeChapter && isPlaying
            ChapterRow(
                chapter = chapter,
                isActive = isActive,
                currentVerse = if (isActive) state.currentVerse.verseNo else null,
                onClick = { onChapterSelected(chapter) },
            )
        }
    }
}

@Composable
private fun ChapterRow(
    chapter: ChapterEntry,
    isActive: Boolean,
    currentVerse: Int?,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = chapter.number.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isActive) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.width(36.dp),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chapter.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${chapter.verseCount} verses",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }

            if (currentVerse != null) {
                Text(
                    text = "Verse $currentVerse",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}
