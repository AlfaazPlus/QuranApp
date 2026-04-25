package com.quranapp.android.compose.components.reader.navigator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.utils.reader.rememberQuranMushafId
import com.quranapp.android.viewModels.ReaderViewModel
import verticalFadingEdge

@Composable
fun PageNavigationList(
    readerVm: ReaderViewModel,
    onPageSelected: (Int) -> Unit,
) {
    val currentPageNo = readerVm.mushafSession.collectAsState().value.currentPageNo

    val mushafId = rememberQuranMushafId()
    val pageCount by produceState(0, mushafId) {
        value = readerVm.mushafPageCount(mushafId).takeIf { it > 0 } ?: 0
    }

    val allPages = remember(pageCount) { (1..pageCount).toList() }

    val gridState = rememberLazyGridState(
        (currentPageNo ?: 1) - 1,
        initialFirstVisibleItemScrollOffset = -100
    )

    var filterText by rememberSaveable { mutableStateOf("") }
    var filteredPages by remember { mutableStateOf(allPages) }

    LaunchedEffect(filterText, allPages) {
        val query = filterText.trim()

        if (query.isEmpty()) {
            filteredPages = allPages
        } else {
            filteredPages = allPages.filter { it.toString().startsWith(query) }
            gridState.requestScrollToItem(0)
        }
    }

    Column {
        Box(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
        ) {
            FilterField(
                value = filterText,
                onValueChange = { filterText = it },
                hint = stringResource(R.string.strHintSearchPage),
                keyboardType = KeyboardType.Number,
            )
        }

        Box(
            Modifier.verticalFadingEdge(gridState)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 56.dp),
                modifier = Modifier.fillMaxWidth(),
                state = gridState,
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 64.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(filteredPages, key = { it }) { pageNo ->
                    val isCurrent = currentPageNo == pageNo

                    Surface(
                        onClick = { onPageSelected(pageNo) },
                        shape = RoundedCornerShape(8.dp),
                        color = colorScheme.surface,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isCurrent) colorScheme.primary else colorScheme.outlineVariant.copy(
                                alpha = 0.4f
                            )
                        )
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                        ) {
                            Text(
                                text = pageNo.toString(),
                                style = typography.labelMedium,
                                color = colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}
