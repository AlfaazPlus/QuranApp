package com.quranapp.android.compose.components.reader.navigator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quranapp.android.R
import com.quranapp.android.compose.components.common.Loader
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.compose.utils.formattedStringResource
import com.quranapp.android.db.entities.quran.SurahEntity
import com.quranapp.android.db.relations.SurahWithLocalizations
import com.quranapp.android.repository.QuranRepository
import com.quranapp.android.viewModels.ChapterNavigatorViewModel
import verticalFadingEdge

private val VerseSelectionSaver: Saver<Set<Int>, List<Int>> = Saver(
    save = { it.sorted() },
    restore = { it.toSet() },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterVerseNavigator(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    selectedChapterNo: Int? = null,
    selectedVerseNos: Set<Int> = emptySet(),
    /**
     * If provided, verse selector will not be visible
     */
    onChapterSelected: ((Int) -> Unit)? = null,
    onVerseSelected: ((chapterNo: Int, verseNo: Int) -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val viewModel = viewModel<ChapterNavigatorViewModel>()

    if (!isOpen) {
        return
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom) },
    ) {

        Content(
            viewModel,
            onDismiss,
            selectedChapterNo,
            selectedVerseNos,
            onChapterSelected,
            onVerseSelected
        )
    }
}

@Composable
private fun Content(
    vm: ChapterNavigatorViewModel,
    onDismiss: () -> Unit,
    initialChapterNo: Int?,
    initialVerseNos: Set<Int>,
    onChapterSelected: ((Int) -> Unit)?,
    onVerseSelected: ((chapterNo: Int, verseNo: Int) -> Unit)?,
) {
    val surahs by vm.surahs.collectAsState()

    var selectedChapterNo by rememberSaveable {
        mutableStateOf<Int?>(initialChapterNo)
    }

    var selectedVerseNos by rememberSaveable(stateSaver = VerseSelectionSaver) {
        mutableStateOf(initialVerseNos)
    }

    LaunchedEffect(initialChapterNo, initialVerseNos) {
        selectedChapterNo = initialChapterNo
        selectedVerseNos = initialVerseNos
    }

    if (surahs.isEmpty()) {
        Loader(true)
        return
    }

    val showVerseSelector = onChapterSelected == null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f)
            .navigationBarsPadding()
    ) {
        Row {
            ChapterOnlyList(
                repository = vm.repository,
                surahs = surahs,
                selectedChapterNo = selectedChapterNo,
                onChapterSelected = { chapterNo ->
                    if (onChapterSelected != null) {
                        onChapterSelected(chapterNo)
                        onDismiss()
                    } else {
                        selectedChapterNo = chapterNo
                        selectedVerseNos = if (chapterNo == initialChapterNo) initialVerseNos
                        else emptySet()
                    }
                },
            )

            if (showVerseSelector) {
                val chapterNo = selectedChapterNo
                if (chapterNo != null) {
                    VerseSelectList(
                        currentChapter = chapterNo.let { no -> surahs.getOrNull(no - 1)?.surah },
                        selectedVerseNos = selectedVerseNos,
                        onToggleVerse = { verseNo ->
                            onVerseSelected?.invoke(chapterNo, verseNo)
                            onDismiss()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.ChapterOnlyList(
    repository: QuranRepository,
    surahs: List<SurahWithLocalizations>,
    selectedChapterNo: Int?,
    onChapterSelected: (Int) -> Unit,
) {
    val gridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = selectedChapterNo?.let { it - 1 } ?: 0,
        initialFirstVisibleItemScrollOffset = -100,
    )

    var filteredSurahs by remember { mutableStateOf(surahs) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(searchQuery, surahs) {
        val query = searchQuery.lowercase().trim()

        if (query.isEmpty()) {
            filteredSurahs = surahs
        } else {
            val surahNos = repository.searchSurahNos(query)

            filteredSurahs = surahs.filter { surah ->
                surah.surah.surahNo in surahNos
            }

            gridState.scrollToItem(0)
        }
    }

    Column(Modifier.weight(1f)) {
        Box(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        ) {
            FilterField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                hint = stringResource(R.string.strHintSearchChapter),
                keyboardType = KeyboardType.Text,
            )
        }

        BoxWithConstraints(Modifier.verticalFadingEdge(gridState)) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (maxWidth < 800.dp) 1 else 2),
                modifier = Modifier.fillMaxWidth(),
                state = gridState,
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 64.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filteredSurahs, key = { it.surah.surahNo }) { surah ->
                    ChapterCard(
                        surah,
                        isCurrent = selectedChapterNo == surah.surah.surahNo,
                        iconWithPrefix = false,
                        onClick = { onChapterSelected(surah.surah.surahNo) },
                    )
                }
            }
        }
    }
}

@Composable
private fun VerseSelectList(
    currentChapter: SurahEntity?,
    selectedVerseNos: Set<Int>,
    onToggleVerse: (Int) -> Unit,
) {
    if (currentChapter == null) return

    val state = rememberLazyListState(
        initialFirstVisibleItemIndex = selectedVerseNos.firstOrNull() ?: 0,
        initialFirstVisibleItemScrollOffset = -200,
    )

    val ayahs = (1..currentChapter.ayahCount).toList()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var filteredAyahs by remember { mutableStateOf(ayahs) }

    LaunchedEffect(currentChapter) {
        state.scrollToItem(selectedVerseNos.firstOrNull() ?: 0, -200)
    }

    LaunchedEffect(ayahs, searchQuery) {
        val query = searchQuery.lowercase().trim()

        if (query.isEmpty()) {
            filteredAyahs = ayahs
        } else {
            filteredAyahs = ayahs.filter { ayahNo ->
                ayahNo.toString().contains(query)
            }
            state.scrollToItem(0)
        }
    }

    Column(modifier = Modifier.width(110.dp)) {
        Box(
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
        ) {
            FilterField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                hint = stringResource(R.string.strHintSearch),
                keyboardType = KeyboardType.Number,
                showLeading = false,
            )
        }

        Box(Modifier.verticalFadingEdge(state)) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(
                    start = 8.dp,
                    end = 8.dp,
                    top = 8.dp,
                    bottom = 64.dp,
                ),
                state = state,
            ) {
                items(filteredAyahs) { verseNo ->
                    val selected = verseNo in selectedVerseNos

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = colorScheme.surface,
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (selected) colorScheme.primary
                            else colorScheme.outlineVariant.alpha(0.5f),
                        ),
                    ) {
                        Text(
                            formattedStringResource(R.string.strLabelVerseNo, verseNo),
                            modifier = Modifier
                                .clickable { onToggleVerse(verseNo) }
                                .padding(10.dp),
                            color = if (selected) colorScheme.primary else colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}
