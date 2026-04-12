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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quranapp.android.R
import com.quranapp.android.api.models.mediaplayer.RecitationAudioKind
import com.quranapp.android.compose.components.common.ErrorMessageCard
import com.quranapp.android.compose.components.dialogs.AlertDialog
import com.quranapp.android.compose.components.dialogs.AlertDialogAction
import com.quranapp.android.compose.components.dialogs.AlertDialogActionStyle
import com.quranapp.android.utils.mediaplayer.RecitationModelManager
import com.quranapp.android.viewModels.RecitationDownloadEvent
import com.quranapp.android.viewModels.RecitationDownloadViewModel

private data class ReciterCleanupRow(
    val kind: RecitationAudioKind,
    val id: String,
    val name: String,
    val downloadedCount: Int,
)

@Composable
fun StorageCleanupRecitationScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
) {
    val viewModel = viewModel<RecitationDownloadViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val modelManager = remember(context) { RecitationModelManager.get(context) }

    val rows = remember(uiState.quranReciters, uiState.translationReciters, uiState.downloadStates) {
        buildList {
            uiState.quranReciters.forEach { m ->
                val st = uiState.downloadStates[RecitationDownloadViewModel.stateKey(RecitationAudioKind.QURAN, m.id)]
                if ((st?.downloadedCount ?: 0) > 0) {
                    add(
                        ReciterCleanupRow(
                            kind = RecitationAudioKind.QURAN,
                            id = m.id,
                            name = m.getReciterName(),
                            downloadedCount = st!!.downloadedCount,
                        ),
                    )
                }
            }
            uiState.translationReciters.forEach { m ->
                val st = uiState.downloadStates[RecitationDownloadViewModel.stateKey(RecitationAudioKind.TRANSLATION, m.id)]
                if ((st?.downloadedCount ?: 0) > 0) {
                    add(
                        ReciterCleanupRow(
                            kind = RecitationAudioKind.TRANSLATION,
                            id = m.id,
                            name = m.getReciterName(),
                            downloadedCount = st!!.downloadedCount,
                        ),
                    )
                }
            }
        }
    }

    var pendingDelete by remember { mutableStateOf<ReciterCleanupRow?>(null) }

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
                onRetry = { viewModel.onEvent(RecitationDownloadEvent.Refresh) },
                modifier = innerModifier
                    .fillMaxSize(),
            )
        }

        rows.isEmpty() -> {
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
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                items(rows, key = { "${it.kind.name}:${it.id}" }) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = row.name,
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = stringResource(R.string.nFiles, row.downloadedCount),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            )
                        }
                        IconButton(onClick = { pendingDelete = row }) {
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
        title = stringResource(R.string.titleRecitationCleanup),
        actions = listOf(
            AlertDialogAction(text = stringResource(R.string.strLabelCancel)),
            AlertDialogAction(
                text = stringResource(R.string.strLabelDelete),
                style = AlertDialogActionStyle.Danger,
                onClick = {
                    toDelete?.let { row ->
                        modelManager.deleteReciterAudioDirectory(row.id)
                        viewModel.onEvent(RecitationDownloadEvent.Refresh)
                    }
                    pendingDelete = null
                },
            ),
        ),
    ) {
        if (toDelete != null) {
            Text(
                text = stringResource(
                    R.string.msgRecitationCleanup,
                    toDelete.name,
                ),
            )
        }
    }
}
