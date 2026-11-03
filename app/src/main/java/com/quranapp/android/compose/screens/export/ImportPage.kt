package com.quranapp.android.compose.screens.export

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.activities.ExportKeys
import com.quranapp.android.compose.components.common.AlertCard
import com.quranapp.android.compose.components.common.AlertType
import com.quranapp.android.compose.components.dialogs.AlertDialog
import com.quranapp.android.compose.components.dialogs.AlertDialogAction
import com.quranapp.android.compose.components.dialogs.AlertDialogActionStyle

@Composable
fun ImportPage(
    fileName: String?,
    onPickFile: () -> Unit,
    onImport: (scopes: Map<String, Boolean>) -> Unit,
) {
    var selectedItems by rememberSaveable(stateSaver = exportSelectionSaver) {
        mutableStateOf(setOf<String>())
    }
    var confirmImport by rememberSaveable { mutableStateOf(false) }
    val selectedCount = selectedItems.size
    val fileSelected = fileName != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedButton(
            onClick = onPickFile,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = MaterialTheme.shapes.medium,
        ) {
            Icon(
                painter = painterResource(R.drawable.col_folder_open),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = fileName ?: stringResource(R.string.labelSelectFile),
                style = typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (fileSelected) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                CategorySelection(
                    forExport = false,
                    selectedItems = selectedItems,
                    onSelectedItemsChange = { selectedItems = it },
                )
                if (ExportKeys.SETTINGS in selectedItems) {
                    AlertCard(
                        type = AlertType.Warning,
                        modifier = Modifier.padding(vertical = 16.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.warnImportSettings),
                            style = typography.bodyMedium,
                        )
                    }
                }
            }

            SelectedActionButton(
                text = stringResource(R.string.labelImportSelected, selectedCount),
                icon = R.drawable.dr_icon_download,
                enabled = selectedCount > 0,
                onClick = { confirmImport = true },
            )
        }
    }

    if (confirmImport) {
        AlertDialog(
            isOpen = true,
            onClose = { confirmImport = false },
            title = stringResource(R.string.titleConfirmImport),
            actions = listOf(
                AlertDialogAction(text = stringResource(R.string.strLabelCancel)),
                AlertDialogAction(
                    text = stringResource(R.string.labelImport),
                    style = AlertDialogActionStyle.Primary,
                    onClick = {
                        confirmImport = false
                        onImport(selectedScopes(selectedItems))
                    },
                ),
            ),
        ) {
            if (ExportKeys.SETTINGS in selectedItems) {
                Text(
                    text = stringResource(R.string.warnImportSettings),
                    style = typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
