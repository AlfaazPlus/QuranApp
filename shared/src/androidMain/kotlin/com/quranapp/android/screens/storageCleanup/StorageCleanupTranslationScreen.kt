package com.quranapp.android.screens.storageCleanup

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
import androidx.compose.ui.unit.dp
import com.quranapp.android.components.common.ErrorMessageCard
import com.quranapp.android.components.common.dialogs.AlertDialog
import com.quranapp.android.components.common.dialogs.AlertDialogAction
import com.quranapp.android.components.common.dialogs.AlertDialogActionStyle
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.extensions.appViewModel
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.dr_icon_delete
import com.quranapp.android.generated.resources.msgDeleteTranslation
import com.quranapp.android.generated.resources.nothingToCleanup
import com.quranapp.android.generated.resources.strLabelCancel
import com.quranapp.android.generated.resources.strLabelDelete
import com.quranapp.android.generated.resources.strTitleTranslDelete
import com.quranapp.android.models.transls.TranslModel
import com.quranapp.android.models.transls.TranslationGroupModel
import com.quranapp.android.viewmodel.TranslationEvent
import com.quranapp.android.viewmodel.TranslationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun StorageCleanupTranslationScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
) {
    val viewModel = appViewModel<TranslationViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    var downloadedSlugs by remember { mutableStateOf<Set<String>>(emptySet()) }
    var downloadedSlugsReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.onEvent(TranslationEvent.Refresh)
    }

    LaunchedEffect(uiState.translationGroups, uiState.isLoading) {
        if (uiState.isLoading) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            DatabaseProvider.getTranslationRepository().let { repo ->
                downloadedSlugs = repo.getDownloadedBooks().keys.toSet()
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
                text = stringResource(Res.string.nothingToCleanup),
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
        title = stringResource(Res.string.strTitleTranslDelete),
        actions = listOf(
            AlertDialogAction(text = stringResource(Res.string.strLabelCancel)),
            AlertDialogAction(
                text = stringResource(Res.string.strLabelDelete),
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
                    Res.string.msgDeleteTranslation,
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
                        painter = painterResource(Res.drawable.dr_icon_delete),
                        contentDescription = stringResource(Res.string.strLabelDelete),
                    )
                }
            }
            HorizontalDivider()
        }
    }
}
