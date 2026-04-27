package com.quranapp.android.compose.screens.reference

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alfaazplus.sunnah.ui.theme.tightTextStyle
import com.quranapp.android.R
import com.quranapp.android.components.quran.ExclusiveVerse
import com.quranapp.android.components.quran.ExclusiveVersesDataset
import com.quranapp.android.components.quran.QuranExclusiveVerses
import com.quranapp.android.compose.components.common.AppBar
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.utils.reader.ExclusiveVerseNavigator
import kotlinx.coroutines.delay

enum class ExclusiveVersesScreenKind(
    val titleRes: Int,
    val dataset: ExclusiveVersesDataset,
) {
    Dua(R.string.strTitleFeaturedDuas, dataset = ExclusiveVersesDataset.Dua),
    Etiquette(R.string.titleEtiquetteVerses, dataset = ExclusiveVersesDataset.Etiquette),
    MajorSins(R.string.strTitleMajorSins, dataset = ExclusiveVersesDataset.MajorSins),
    Solution(R.string.titleSolutionVerses, dataset = ExclusiveVersesDataset.Solution),
}

@Composable
fun ExclusiveVersesListScreen(kind: ExclusiveVersesScreenKind) {
    val verses = QuranExclusiveVerses.observe(kind.dataset)

    var searchQuery by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }

    LaunchedEffect(searchQuery) {
        delay(150)
        debouncedQuery = searchQuery
    }

    val filteredVerses = remember(verses, debouncedQuery) {
        val list = verses ?: return@remember emptyList()
        val q = debouncedQuery.trim()
        if (q.isEmpty()) list
        else {
            list.filter { verse ->
                buildString {
                    append(verse.title)
                    verse.description?.takeIf { it.isNotBlank() }?.let { append(it) }
                    append(verse.inChapters)
                }.contains(q, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            AppBar(
                title = stringResource(kind.titleRes),
                bgColor = colorResource(R.color.colorBGHomePageItem),
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                searchPlaceholder = stringResource(R.string.strHintSearch),
            )
        },
    ) { paddingValues ->
        when {
            verses == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            verses.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.strMsgSearchNoResultsFound),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            filteredVerses.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.strMsgSearchNoResultsFound),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            else -> {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                ) {
                    val columns = if (maxWidth >= 960.dp) 3 else if (maxWidth >= 600.dp) 2 else 1

                    val rows = filteredVerses.chunked(columns)

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = 64.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(
                            items = rows,
                            key = { row ->
                                row.joinToString(separator = "-") { "${it.id}_${it.title}" }
                            },
                        ) { row ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                row.forEach { verse ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                    ) {
                                        ExclusiveVerseListItem(kind = kind, verse = verse)
                                    }
                                }
                                repeat(columns - row.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExclusiveVerseListItem(
    kind: ExclusiveVersesScreenKind,
    verse: ExclusiveVerse,
) {
    when (kind) {
        ExclusiveVersesScreenKind.Dua -> DuaListItem(verse)
        ExclusiveVersesScreenKind.Etiquette -> EtiquetteListItem(verse)
        ExclusiveVersesScreenKind.MajorSins -> MajorSinsListItem(verse)
        ExclusiveVersesScreenKind.Solution -> SolutionListItem(verse)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DuaListItem(verse: ExclusiveVerse) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val excluded = verse.id in arrayOf(1, 2)
    val duaName = if (!excluded) {
        stringResource(R.string.strMsgDuaFor, verse.title)
    } else {
        verse.title
    }
    val count = verse.verses.size
    val placesLine = when {
        verse.id == 1 -> null
        count > 1 -> stringResource(R.string.places, count)
        else -> stringResource(R.string.place, count)
    }
    val inChaptersLine = if (verse.id == 1) null else verse.inChapters.takeIf { it.isNotBlank() }

    Card(
        onClick = {
            ExclusiveVerseNavigator.open(context, ExclusiveVersesDataset.Dua, verse)
        },
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        border = BorderStroke(1.dp, colorScheme.outlineVariant.alpha(0.5f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = duaName,
                style = MaterialTheme.typography.titleMedium.merge(tightTextStyle),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Start,
            )
            placesLine?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium.merge(tightTextStyle),
                    color = colorScheme.onSurface.copy(alpha = 0.85f),
                )
            }
            inChaptersLine?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall.merge(tightTextStyle),
                    color = colorResource(R.color.colorText2),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EtiquetteListItem(verse: ExclusiveVerse) {
    val context = LocalContext.current

    Card(
        onClick = {
            ExclusiveVerseNavigator.open(context, ExclusiveVersesDataset.Etiquette, verse)
        },
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        border = BorderStroke(1.dp, colorScheme.outlineVariant.alpha(0.5f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = verse.title,
                style = MaterialTheme.typography.titleMedium.merge(tightTextStyle),
                fontWeight = FontWeight.SemiBold,
            )
            if (verse.chapters.isNotEmpty() && verse.inChapters.isNotBlank()) {
                Text(
                    text = verse.inChapters,
                    style = MaterialTheme.typography.bodyMedium.merge(tightTextStyle),
                    color = colorResource(R.color.colorText2),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MajorSinsListItem(verse: ExclusiveVerse) {
    val context = LocalContext.current

    Card(
        onClick = {
            ExclusiveVerseNavigator.open(context, ExclusiveVersesDataset.MajorSins, verse)
        },
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        border = BorderStroke(1.dp, colorScheme.outlineVariant.alpha(0.5f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = verse.title,
                style = MaterialTheme.typography.titleMedium.merge(tightTextStyle),
                fontWeight = FontWeight.SemiBold,
            )
            verse.description?.takeIf { it.isNotBlank() }?.let { desc ->
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium.merge(tightTextStyle),
                    color = colorScheme.onSurface.copy(alpha = 0.8f),
                )
            }
            if (verse.inChapters.isNotBlank()) {
                Text(
                    text = verse.inChapters,
                    style = MaterialTheme.typography.bodySmall.merge(tightTextStyle),
                    color = colorResource(R.color.colorText2),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SolutionListItem(verse: ExclusiveVerse) {
    val context = LocalContext.current
    val count = verse.verses.size
    val placesLine = if (count > 1) {
        stringResource(R.string.places, count)
    } else {
        stringResource(R.string.place, count)
    }

    Card(
        onClick = {
            ExclusiveVerseNavigator.open(context, ExclusiveVersesDataset.Solution, verse)
        },
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        border = BorderStroke(1.dp, colorScheme.outlineVariant.alpha(0.5f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = verse.title,
                style = MaterialTheme.typography.titleMedium.merge(tightTextStyle),
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = placesLine,
                style = MaterialTheme.typography.bodyMedium.merge(tightTextStyle),
                color = colorScheme.onSurface.copy(alpha = 0.85f),
            )
            if (verse.inChapters.isNotBlank()) {
                Text(
                    text = verse.inChapters,
                    style = MaterialTheme.typography.bodySmall.merge(tightTextStyle),
                    color = colorResource(R.color.colorText2),
                )
            }
        }
    }
}
