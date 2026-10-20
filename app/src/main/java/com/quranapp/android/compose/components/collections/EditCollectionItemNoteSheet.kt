package com.quranapp.android.compose.components.collections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quranapp.android.R
import com.quranapp.android.compose.components.common.TextInput
import com.quranapp.android.compose.components.dialogs.BottomSheet
import com.quranapp.android.db.entities.user.CollectionItemEntity
import com.quranapp.android.viewModels.CollectionsViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCollectionItemNoteSheet(
    itemToUpdate: CollectionItemEntity? = null,
    onClose: () -> Unit,
) {
    BottomSheet(
        isOpen = itemToUpdate != null,
        onDismiss = onClose,
        title = stringResource(
            if (!itemToUpdate?.note.isNullOrBlank()) R.string.update_note
            else R.string.strTitleAddNote
        ),
        contentWindowInsets = { BottomSheetDefaults.windowInsets },
        sheetGesturesEnabled = false,
    ) {
        if (itemToUpdate != null) {
            Content(
                itemToUpdate = itemToUpdate,
                onClose = onClose,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.Content(
    itemToUpdate: CollectionItemEntity,
    onClose: () -> Unit,
) {
    val viewModel = viewModel<CollectionsViewModel>()
    var note by remember(itemToUpdate) { mutableStateOf(itemToUpdate.note ?: "") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, false)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(
                R.string.strLabelVerseSerial,
                itemToUpdate.chapterNo,
                itemToUpdate.verseNo
            )
        )
        TextInput(
            value = note,
            onValueChange = { note = it },
            maxLines = 5,
            minLines = 5,
            label = stringResource(R.string.strTitleNote),
            placeholder = stringResource(R.string.collection_note_hint),
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorScheme.surfaceContainer)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            modifier = Modifier.weight(1f),
            onClick = onClose,
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.surfaceVariant,
                contentColor = colorScheme.onSurfaceVariant,
            ),
        ) {
            Text(stringResource(R.string.strLabelCancel))
        }
        Button(
            modifier = Modifier.weight(1f),
            onClick = {
                viewModel.updateItemNote(
                    itemId = itemToUpdate.id,
                    note = note
                )
                onClose()
            },
        ) {
            Text(
                stringResource(
                    R.string.strLabelUpdate
                )
            )
        }
    }
}
