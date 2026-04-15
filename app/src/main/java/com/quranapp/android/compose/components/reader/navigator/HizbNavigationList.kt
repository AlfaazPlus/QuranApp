package com.quranapp.android.compose.components.reader.navigator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.compose.components.common.Loader
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.db.relations.NavigationUnit
import com.quranapp.android.viewModels.ReaderViewModel
import com.quranapp.android.viewModels.ReaderViewType
import verticalFadingEdge

@Composable
fun HizbNavigationList(
    readerVm: ReaderViewModel,
    onHizbSelected: (Int) -> Unit,
    onVerseSelected: (Int, Int) -> Unit,
) {
    val hizbs by readerVm.hizbs.collectAsState()
    val hizbViewState = readerVm.uiState.collectAsState().value.viewType as? ReaderViewType.Hizb

    if (hizbs.isEmpty()) return Loader(true)

    val ayahs = remember<List<ChapterVersePair>>(hizbs, hizbViewState) {
        val currentHizbRange = hizbViewState?.hizbNo?.let { hizbs.getOrNull(it - 1) }?.ranges
            ?: return@remember emptyList()

        currentHizbRange.flatMap { range ->
            (range.startAyah..range.endAyah).map { ayahNo ->
                ChapterVersePair(
                    chapterNo = range.surah.surah.surahNo,
                    verseNo = ayahNo
                )
            }
        }
    }

    Row {
        HizbGrid(
            readerVm,
            hizbs,
            hizbViewState,
            onHizbSelected,
        )
        NavigationVerseList(
            ayahs = ayahs,
            onVerseSelected = onVerseSelected
        )
    }
}

@Composable
private fun RowScope.HizbGrid(
    readerVm: ReaderViewModel,
    hizbs: List<NavigationUnit>,
    hizbViewState: ReaderViewType.Hizb?,
    onHizbSelected: (Int) -> Unit
) {
    val gridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = hizbViewState?.hizbNo?.let { it - 1 } ?: 0,
        initialFirstVisibleItemScrollOffset = -100
    )

    var filterText by rememberSaveable { mutableStateOf("") }
    var filteredHizbs by remember { mutableStateOf(hizbs) }

    LaunchedEffect(filterText, hizbs) {
        val query = filterText.lowercase().trim()

        if (query.isEmpty()) {
            filteredHizbs = hizbs
        } else {
            val surahNos = readerVm.repository.searchSurahNos(query)

            filteredHizbs = hizbs.filter { hizb ->
                hizb.unitNo.toString().contains(query)
                    || hizb.ranges.any { it.surah.surah.surahNo in surahNos }
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
                value = filterText,
                onValueChange = { filterText = it },
                hint = stringResource(R.string.strTitleReaderHizb),
                keyboardType = KeyboardType.Text,
            )
        }

        BoxWithConstraints(
            Modifier.verticalFadingEdge(gridState)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(
                    when {
                        maxWidth >= 600.dp -> 2
                        maxWidth >= 300.dp && hizbViewState?.hizbNo == null -> 2
                        else ->1
                    }
                ),
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
                items(filteredHizbs, key = { it.unitNo }) { hizb ->
                    HizbCard(
                        hizb,
                        isCurrent = hizbViewState?.hizbNo == hizb.unitNo,
                        onClick = {
                            onHizbSelected(hizb.unitNo)
                        }
                    )
                }
            }
        }
    }
}