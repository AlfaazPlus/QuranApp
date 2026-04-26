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
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.compose.components.common.Loader
import com.quranapp.android.compose.components.common.SearchTextField
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.db.entities.quran.SurahEntity
import com.quranapp.android.db.relations.SurahWithLocalizations
import com.quranapp.android.utils.quran.QuranUtils
import com.quranapp.android.viewModels.ReaderViewModel
import com.quranapp.android.viewModels.ReaderViewType
import verticalFadingEdge

@Composable
fun ChapterNavigatorList(
    readerVm: ReaderViewModel,
    onChapterSelected: (Int) -> Unit,
    onVerseSelected: (Int, Int) -> Unit,
) {
    val surahs by readerVm.surahs.collectAsState()
    val chapterViewState = readerVm.uiState.collectAsState().value.viewType as? ReaderViewType.Chapter

    val mushafSession by readerVm.mushafSession.collectAsState()
    val currentMushafId = mushafSession.layout.toMushafId()
    val currentPageNo = mushafSession.currentPageNo

    val activeChapterNo by produceState<Int?>(chapterViewState?.chapterNo, currentPageNo, currentMushafId) {
        value = when {
            chapterViewState?.chapterNo != null -> chapterViewState.chapterNo

            currentPageNo != null && currentMushafId > 0 -> {
                val firstAyahId = readerVm.repository.getFirstAyahIdOnPage(currentMushafId, currentPageNo)
                firstAyahId?.let { QuranUtils.getVerseNoFromAyahId(it).first }
            }

            else -> null
        }
    }


    if (surahs.isEmpty()) return Loader(true)

    Row() {
        ChapterList(
            readerVm,
            surahs,
            activeChapterNo,
            onChapterSelected,
        )
        ChapterVerseList(
            currentChapter = activeChapterNo?.let { surahs.getOrNull(it - 1) }?.surah,
            onVerseSelected = onVerseSelected
        )
    }
}

@Composable
private fun RowScope.ChapterList(
    readerVm: ReaderViewModel,
    surahs: List<SurahWithLocalizations>,
    activeChapterNo: Int?,
    onChapterSelected: (Int) -> Unit
) {
    val gridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = activeChapterNo?.let { it - 1 } ?: 0,
        initialFirstVisibleItemScrollOffset = -100
    )

    var filteredSurahs by remember { mutableStateOf(surahs) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(searchQuery, surahs) {
        val query = searchQuery.lowercase().trim()

        if (query.isEmpty()) {
            filteredSurahs = surahs
        } else {
            val surahNos = readerVm.repository.searchSurahNos(query)

            filteredSurahs = surahs.filter { surah ->
                surah.surah.surahNo in surahNos
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
                hint = stringResource(R.string.strHintSearchChapter),
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
                    top = 16.dp,
                    bottom = 64.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filteredSurahs, key = { it.surah.surahNo }) { surah ->
                    ChapterCard(
                        surah,
                        isCurrent = activeChapterNo == surah.surah.surahNo,
                        iconWithPrefix = false,
                        onClick = {
                            onChapterSelected(surah.surah.surahNo)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChapterVerseList(currentChapter: SurahEntity?, onVerseSelected: (Int, Int) -> Unit) {
    if (currentChapter == null) return

    val state = rememberLazyListState()

    val ayahs = (1..currentChapter.ayahCount).toList()
    var searchQuery by remember { mutableStateOf("") }
    var filteredAyahs by remember {
        mutableStateOf(ayahs)
    }

    LaunchedEffect(ayahs, searchQuery) {
        val query = searchQuery.lowercase().trim()

        if (query.isEmpty()) {
            filteredAyahs = ayahs
        } else {
            filteredAyahs = ayahs.filter { ayahNo ->
                ayahNo.toString().contains(query)
            }

            state.requestScrollToItem(0)
        }
    }

    Column(
        modifier = Modifier
            .width(100.dp)
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
                items(filteredAyahs) { verseNo ->
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
                            stringResource(R.string.strLabelVerseNo, verseNo),
                            modifier = Modifier
                                .clickable {
                                    onVerseSelected(currentChapter.surahNo, verseNo)
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

@Composable
internal fun FilterField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    showLeading: Boolean = true
) {
    SearchTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = hint,
        leadingIcon = if (showLeading) {
            {
                Icon(
                    painter = painterResource(R.drawable.dr_icon_search),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = colorScheme.onSurface.alpha(0.5f)
                )
            }
        } else null,
        keyboardType = keyboardType,
        modifier = modifier
    )
}