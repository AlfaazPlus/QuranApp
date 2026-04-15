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
import androidx.compose.runtime.LaunchedEffect
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
import com.quranapp.android.components.transls.TranslModel
import com.quranapp.android.components.transls.TranslationGroupModel
import com.quranapp.android.compose.components.common.ErrorMessageCard
import com.quranapp.android.compose.components.dialogs.AlertDialog
import com.quranapp.android.compose.components.dialogs.AlertDialogAction
import com.quranapp.android.compose.components.dialogs.AlertDialogActionStyle
import com.quranapp.android.utils.reader.factory.QuranTranslationFactory
import com.quranapp.android.viewModels.TranslationEvent
import com.quranapp.android.viewModels.TranslationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun StorageCleanupTranslationScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
) {
    val viewModel = viewModel<TranslationViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var downloadedSlugs by remember { mutableStateOf<Set<String>>(emptySet()) }
    var downloadedSlugsReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.onEvent(TranslationEvent.Refresh)
    }

    LaunchedEffect(uiState.translationGroups, uiState.isLoading) {
        if (uiState.isLoading) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            QuranTranslationFactory(context).use { factory ->
                downloadedSlugs = factory.getDownloadedTranslationBooksInfo().keys.toSet()
            }
        }
        downloadedSlugsReady = true
    }

    val filteredGroups = remember(uiState.translationGroups, downloadedSlugs) {
        uiState.translationGroups.mapNotNull { group ->
            val kept = group.translations.filter { downloadedSlugs.contains(it.bookInfo.slug) }
            if (kept.isEmpty()) null else group.copy(translations = ArrayList(kept))
        }
    }

    var pendingDelete by remember { mutableStateOf<TranslModel?>(null) }

    val innerModifier = modifier.padding(contentPadding)

    when {
        uiState.isLoading || !downloadedSlugsReady -> {
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
                onRetry = { viewModel.onEvent(TranslationEvent.Refresh) },
                modifier = innerModifier
                    .fillMaxSize(),
            )
        }

        filteredGroups.isEmpty() -> {
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
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filteredGroups, key = { it.langCode }) { group ->
                    TranslationCleanupGroup(
                        group = group,
                        onDelete = { pendingDelete = it },
                    )
                }
            }
        }
    }

    val toDelete = pendingDelete
    AlertDialog(
        isOpen = toDelete != null,
        onClose = { pendingDelete = null },
        title = stringResource(R.string.strTitleTranslDelete),
        actions = listOf(
            AlertDialogAction(text = stringResource(R.string.strLabelCancel)),
            AlertDialogAction(
                text = stringResource(R.string.strLabelDelete),
                style = AlertDialogActionStyle.Danger,
                onClick = {
                    toDelete?.let {
                        viewModel.onEvent(TranslationEvent.DeleteTranslation(it.bookInfo.slug))
                    }
                    pendingDelete = null
                },
            ),
        ),
    ) {
        if (toDelete != null) {
            Text(
                text = stringResource(
                    R.string.msgDeleteTranslation,
                    toDelete.bookInfo.bookName,
                    toDelete.bookInfo.authorName,
                ),
            )
        }
    }
}

@Composable
private fun TranslationCleanupGroup(
    group: TranslationGroupModel,
    onDelete: (TranslModel) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = group.langName,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        group.translations.forEach { transl ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transl.bookInfo.bookName,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = transl.bookInfo.authorName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
                IconButton(onClick = { onDelete(transl) }) {
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
