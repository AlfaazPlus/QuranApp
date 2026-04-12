package com.quranapp.android.compose.screens.storageCleanup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quranapp.android.R
import com.quranapp.android.api.models.tafsir.TafsirInfoModel
import com.quranapp.android.compose.components.common.ErrorMessageCard
import com.quranapp.android.compose.components.dialogs.AlertDialog
import com.quranapp.android.compose.components.dialogs.AlertDialogAction
import com.quranapp.android.compose.components.dialogs.AlertDialogActionStyle
import com.quranapp.android.viewModels.TafsirEvent
import com.quranapp.android.viewModels.TafsirViewModel

@Composable
fun StorageCleanupTafsirScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
) {
    val viewModel = viewModel<TafsirViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    val downloadedRows = remember(uiState.tafsirGroups, uiState.downloadedTafsirKeys) {
        buildList {
            uiState.tafsirGroups.forEach { group ->
                group.tafsirs.forEach { tafsir ->
                    if (uiState.downloadedTafsirKeys.contains(tafsir.key)) {
                        add(tafsir)
                    }
                }
            }
        }
    }

    var pendingDelete by remember { mutableStateOf<TafsirInfoModel?>(null) }

    val innerModifier = modifier.padding(contentPadding)

    when {
        uiState.isLoading -> {
            Column(
                modifier = innerModifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }
        }

        uiState.error != null -> {
            ErrorMessageCard(
                error = uiState.error!!,
                onRetry = { viewModel.onEvent(TafsirEvent.Refresh) },
                modifier = innerModifier
                    .fillMaxSize(),
            )
        }

        downloadedRows.isEmpty() -> {
            Text(
                text = stringResource(R.string.nothingToCleanup),
                style = MaterialTheme.typography.bodyLarge,
                modifier = innerModifier
                    .fillMaxSize()
                    .padding(24.dp),
            )
        }

        else -> {
            LazyColumn(
                modifier = innerModifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
            ) {
                items(downloadedRows, key = { it.key }) { tafsir ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tafsir.name,
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = tafsir.author,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            )
                        }
                        IconButton(onClick = { pendingDelete = tafsir }) {
                            Icon(
                                painter = painterResource(R.drawable.dr_icon_delete),
                                contentDescription = stringResource(R.string.strLabelDelete),
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    val toDelete = pendingDelete
    AlertDialog(
        isOpen = toDelete != null,
        onClose = { pendingDelete = null },
        title = stringResource(R.string.strTitleDeleteTafsir),
        actions = listOf(
            AlertDialogAction(text = stringResource(R.string.strLabelCancel)),
            AlertDialogAction(
                text = stringResource(R.string.strLabelDelete),
                style = AlertDialogActionStyle.Danger,
                onClick = {
                    toDelete?.let { viewModel.onEvent(TafsirEvent.DeleteTafsir(it.key)) }
                    pendingDelete = null
                },
            ),
        ),
    ) {
        if (toDelete != null) {
            Text(
                text = stringResource(
                    R.string.strMsgDeleteTafsir,
                    toDelete.name,
                ),
            )
        }
    }
}
