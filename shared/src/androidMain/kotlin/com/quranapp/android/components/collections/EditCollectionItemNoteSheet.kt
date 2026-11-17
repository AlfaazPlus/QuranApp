package com.quranapp.android.components.collections

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.quranapp.android.components.common.TextInput
import com.quranapp.android.components.common.dialogs.BottomSheet
import com.quranapp.android.db.entities.user.CollectionItemEntity
import com.quranapp.android.extensions.appViewModel
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.collection_note_hint
import com.quranapp.android.generated.resources.strLabelCancel
import com.quranapp.android.generated.resources.strLabelUpdate
import com.quranapp.android.generated.resources.strLabelVerseSerial
import com.quranapp.android.generated.resources.strTitleAddNote
import com.quranapp.android.generated.resources.strTitleNote
import com.quranapp.android.generated.resources.update_note
import com.quranapp.android.viewmodel.CollectionsViewModel
import org.jetbrains.compose.resources.stringResource

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
            if (!itemToUpdate?.note.isNullOrBlank()) Res.string.update_note
            else Res.string.strTitleAddNote
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
    val viewModel = appViewModel<CollectionsViewModel>()
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var note by remember(itemToUpdate) {
        val text = itemToUpdate.note ?: ""
        mutableStateOf(TextFieldValue(text, TextRange(text.length)))
    }

    LaunchedEffect(itemToUpdate.id) {
        focusRequester.requestFocus()
        keyboard?.show()
    }

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
                Res.string.strLabelVerseSerial,
                itemToUpdate.chapterNo,
                itemToUpdate.verseNo
            )
        )
        TextInput(
            modifier = Modifier.focusRequester(focusRequester),
            value = note,
            onValueChange = { note = it },
            maxLines = 5,
            minLines = 5,
            label = stringResource(Res.string.strTitleNote),
            placeholder = stringResource(Res.string.collection_note_hint),
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
            Text(stringResource(Res.string.strLabelCancel))
        }
        Button(
            modifier = Modifier.weight(1f),
            onClick = {
                viewModel.updateItemNote(
                    itemId = itemToUpdate.id,
                    note = note.text
                )
                onClose()
            },
        ) {
            Text(
                stringResource(
                    Res.string.strLabelUpdate
                )
            )
        }
    }
}
