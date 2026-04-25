package com.quranapp.android.compose.components.reader.navigator

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.compose.components.common.Loader
import com.quranapp.android.db.relations.NavigationUnit
import com.quranapp.android.viewModels.ReaderViewModel
import com.quranapp.android.viewModels.ReaderViewType
import verticalFadingEdge

@Composable
fun JuzNavigationList(
    readerVm: ReaderViewModel,
    onJuzSelected: (Int) -> Unit,
    onVerseSelected: (Int, Int) -> Unit,
) {
    val juzs by readerVm.juzs.collectAsState()
    val juzViewState = readerVm.uiState.collectAsState().value.viewType as? ReaderViewType.Juz

    val mushafSession by readerVm.mushafSession.collectAsState()
    val currentPageNo = mushafSession.currentPageNo
    val currentMushafId = mushafSession.layout.toMushafId()

    val activeJuzNo by produceState<Int?>(juzViewState?.juzNo, currentPageNo, currentMushafId) {
        value = when {
            juzViewState?.juzNo != null -> juzViewState.juzNo

            currentPageNo != null && currentMushafId > 0 -> {
                readerVm.repository.getJuzForMushafPages(
                    currentMushafId,
                    listOf(currentPageNo)
                )[currentPageNo]
                    ?.takeIf { it > 0 }
            }

            else -> null
        }
    }

    if (juzs.isEmpty()) return Loader(true)

    val ayahs = remember<List<ChapterVersePair>>(juzs, activeJuzNo) {
        val currentJuzRange = activeJuzNo?.let { juzs.getOrNull(it - 1) }?.ranges
        if (currentJuzRange == null) return@remember emptyList()

        currentJuzRange.flatMap { range ->
            (range.startAyah..range.endAyah).map { ayahNo ->
                ChapterVersePair(
                    chapterNo = range.surah.surah.surahNo,
                    verseNo = ayahNo
                )
            }
        }
    }

    Row() {
        JuzList(
            readerVm,
            juzs,
            activeJuzNo,
            onJuzSelected,
        )
        NavigationVerseList(
            ayahs = ayahs,
            onVerseSelected = onVerseSelected
        )
    }
}


@Composable
private fun RowScope.JuzList(
    readerVm: ReaderViewModel,
    juzs: List<NavigationUnit>,
    activeJuzNo: Int?,
    onJuzSelected: (Int) -> Unit
) {
    val gridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = activeJuzNo?.let { it - 1 } ?: 0,
        initialFirstVisibleItemScrollOffset = -100
    )

    var filteredJuzs by remember { mutableStateOf(juzs) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(searchQuery, juzs) {
        val query = searchQuery.lowercase().trim()

        if (query.isEmpty()) {
            filteredJuzs = juzs
        } else {
            val surahNos = readerVm.repository.searchSurahNos(query)

            filteredJuzs = juzs.filter { juz ->
                juz.unitNo.toString().contains(query)
                        || juz.ranges.any { it.surah.surah.surahNo in surahNos }
            }

            gridState.requestScrollToItem(0)
        }
    }

    Column(
        Modifier.weight(1f)
    ) {
        Box(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
        ) {
            FilterField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                hint = stringResource(R.string.strHintSearchBy),
                keyboardType = KeyboardType.Text,
            )
        }

        BoxWithConstraints(
            Modifier.verticalFadingEdge(gridState)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (maxWidth < 800.dp) 1 else 2),
                modifier = Modifier.fillMaxWidth(),
                state = gridState,
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 64.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filteredJuzs, key = { it.unitNo }) { juz ->
                    JuzCard(
                        juz,
                        isCurrent = activeJuzNo == juz.unitNo,
                        onClick = {
                            onJuzSelected(juz.unitNo)
                        }
                    )
                }
            }
        }
    }
}


@Composable
internal fun NavigationVerseList(
    ayahs: List<ChapterVersePair>,
    onVerseSelected: (Int, Int) -> Unit
) {
    if (ayahs.isEmpty()) return

    val state = rememberLazyListState()

    var searchQuery by remember { mutableStateOf("") }

    var filteredAyahs by remember {
        mutableStateOf(ayahs)
    }

    LaunchedEffect(ayahs, searchQuery) {
        val query = searchQuery.lowercase().trim()

        if (query.isEmpty()) {
            filteredAyahs = ayahs
        } else {
            filteredAyahs = ayahs.filter {
                it.verseNo.toString().contains(query)
            }

            state.requestScrollToItem(0)
        }
    }

    Column(
        modifier = Modifier
            .width(120.dp)
    ) {
        Box(
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 8.dp)
        ) {
            FilterField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                },
                hint = stringResource(R.string.strHintSearch),
                keyboardType = KeyboardType.Number,
                showLeading = false
            )
        }

        Box(
            Modifier.verticalFadingEdge(state)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(
                    start = 8.dp,
                    end = 8.dp,
                    top = 8.dp,
                    bottom = 64.dp
                ),
                state = state
            ) {
                items(filteredAyahs) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    ) {
                        Text(
                            stringResource(
                                R.string.strLabelVerseWithChapNo,
                                it.chapterNo,
                                it.verseNo
                            ),
                            modifier = Modifier
                                .clickable {
                                    onVerseSelected(it.chapterNo, it.verseNo)
                                }
                                .padding(10.dp),
                            style = typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }
    }
}