package com.quranapp.android.compose.components.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.compose.components.dialogs.AlertDialog
import com.quranapp.android.compose.components.dialogs.AlertDialogAction
import com.quranapp.android.compose.components.dialogs.AlertDialogActionStyle
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.db.search.SearchHistoryEntry
import com.quranapp.android.viewModels.QuranSearchViewModel

@Composable
fun SearchHistorySuggestionStrip(
    suggestions: List<SearchHistoryEntry>,
    onSelect: (String) -> Unit,
) {
    if (suggestions.isEmpty()) return

    Surface(
        color = colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 12.dp),
        ) {
            items(
                items = suggestions,
                key = { it.id },
            ) { entry ->
                SearchHistoryQueryChip(
                    text = entry.text,
                    onClick = { onSelect(entry.text) },
                )
            }
        }
    }
}

@Composable
private fun SearchHistoryQueryChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    Surface(
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            width = 1.dp,
            color = colorScheme.outlineVariant.alpha(0.45f),
        ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        )
    }
}

@Composable
fun SearchEmptyScrollContent(
    viewModel: QuranSearchViewModel,
    modifier: Modifier = Modifier,
) {
    val history by viewModel.searchHistory.collectAsState()
    var showClearAllDialog by remember { mutableStateOf(false) }

    ClearSearchHistoryDialog(
        isOpen = showClearAllDialog,
        onDismiss = { showClearAllDialog = false },
        onConfirm = {
            viewModel.clearSearchHistory()
        },
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp),
    ) {
        item {
            SearchTipsCard(viewModel)
        }

        if (history.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.titleRecentSearches),
                        style = MaterialTheme.typography.titleSmall,
                        color = colorScheme.onBackground.alpha(0.75f),
                    )

                    TextButton(
                        onClick = { showClearAllDialog = true },
                    ) {
                        Text(
                            stringResource(R.string.clear),
                            style = typography.labelMedium
                        )
                    }
                }
            }

            items(
                items = history,
                key = { it.id },
            ) { entry ->
                SearchHistoryRow(
                    entry = entry,
                    onClick = {
                        viewModel.recordSearchQuery(entry.text)
                        viewModel.onQueryChange(entry.text)
                    },
                    onRemove = { viewModel.removeSearchHistory(entry.id) },
                )
            }
        }
    }
}

@Composable
private fun ClearSearchHistoryDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        isOpen = isOpen,
        onClose = onDismiss,
        title = stringResource(R.string.msgClearSearchHistory),
        actions = listOf(
            AlertDialogAction(
                text = stringResource(R.string.strLabelCancel),
            ),
            AlertDialogAction(
                text = stringResource(R.string.strLabelRemoveAll),
                style = AlertDialogActionStyle.Danger,
                onClick = onConfirm,
            ),
        ),
    ) {
        Text(
            text = stringResource(R.string.strMsgSearchHistoryDeleteAll),
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurface,
        )
    }
}

@Composable
private fun SearchHistoryRow(
    entry: SearchHistoryEntry,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.dr_icon_history),
            contentDescription = null,
            modifier = Modifier
                .padding(end = 12.dp)
                .size(22.dp),
            tint = colorScheme.primary,
        )

        Text(
            text = entry.text,
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        IconButton(onClick = onRemove) {
            Icon(
                painter = painterResource(R.drawable.dr_icon_close),
                contentDescription = stringResource(R.string.strLabelClose),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
fun SearchTipsCard(
    viewModel: QuranSearchViewModel,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column {
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.searchTipsTitle),
                    style = typography.titleSmall,
                    color = colorScheme.onBackground.alpha(0.75f)
                )
            }

            HorizontalDivider(
                color = colorScheme.outlineVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Column(
                modifier = Modifier.padding(vertical = 16.dp),
            ) {
                TipRow("2:255", stringResource(R.string.searchTipDirectVerse)) { example ->
                    viewModel.recordSearchQuery(example)
                    viewModel.onQueryChange(example)
                }
                TipRow("baqarah", stringResource(R.string.searchTipChapter)) { example ->
                    viewModel.recordSearchQuery(example)
                    viewModel.onQueryChange(example)
                }
                TipRow("30", stringResource(R.string.searchTipDirectJuz)) { example ->
                    viewModel.recordSearchQuery(example)
                    viewModel.onQueryChange(example)
                }
                TipRow("mercy", stringResource(R.string.searchTipTranslation)) { example ->
                    viewModel.recordSearchQuery(example)
                    viewModel.onQueryChange(example)
                }
                TipRow("الرحيم", stringResource(R.string.searchTipArabic)) { example ->
                    viewModel.recordSearchQuery(example)
                    viewModel.onQueryChange(example, true)
                }
            }
        }
    }
}

@Composable
private fun TipRow(example: String, description: String, onClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick(example)
            }
            .padding(vertical = 7.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(82.dp)
        ) {
            Text(
                text = example,
                color = MaterialTheme.colorScheme.primary,
                style = typography.labelMedium,
                modifier = Modifier
                    .background(colorScheme.background, shapes.small)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onBackground.alpha(0.75f)
        )
    }
}
