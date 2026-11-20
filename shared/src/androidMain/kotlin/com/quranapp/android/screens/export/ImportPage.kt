package com.quranapp.android.screens.export

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quranapp.android.backup.ExportKeys
import com.quranapp.android.backup.importBackup
import com.quranapp.android.backup.readImportFileName
import com.quranapp.android.components.common.AlertCard
import com.quranapp.android.components.common.AlertType
import com.quranapp.android.components.common.dialogs.AlertDialog
import com.quranapp.android.components.common.dialogs.AlertDialogAction
import com.quranapp.android.components.common.dialogs.AlertDialogActionStyle
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.col_folder_open
import com.quranapp.android.generated.resources.dr_icon_download
import com.quranapp.android.generated.resources.labelImport
import com.quranapp.android.generated.resources.labelImportSelected
import com.quranapp.android.generated.resources.labelSelectFile
import com.quranapp.android.generated.resources.strLabelCancel
import com.quranapp.android.generated.resources.titleConfirmImport
import com.quranapp.android.generated.resources.warnImportSettings
import com.quranapp.android.platform.files.rememberDocumentPicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ImportPage() {
    val scope = rememberCoroutineScope()
    var importLocation by rememberSaveable { mutableStateOf<String?>(null) }
    var fileName by rememberSaveable { mutableStateOf<String?>(null) }
    val pickDocument = rememberDocumentPicker(
        mimeTypes = listOf("application/json", "application/gzip", "application/x-gzip"),
    ) { location ->
        importLocation = location
        fileName = location?.let(::readImportFileName)
    }
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
            onClick = pickDocument,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = MaterialTheme.shapes.medium,
        ) {
            Icon(
                painter = painterResource(Res.drawable.col_folder_open),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = fileName ?: stringResource(Res.string.labelSelectFile),
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
                            text = stringResource(Res.string.warnImportSettings),
                            style = typography.bodyMedium,
                        )
                    }
                }
            }

            SelectedActionButton(
                text = stringResource(Res.string.labelImportSelected, selectedCount),
                icon = Res.drawable.dr_icon_download,
                enabled = selectedCount > 0,
                onClick = { confirmImport = true },
            )
        }
    }

    if (confirmImport) {
        AlertDialog(
            isOpen = true,
            onClose = { confirmImport = false },
            title = stringResource(Res.string.titleConfirmImport),
            actions = listOf(
                AlertDialogAction(text = stringResource(Res.string.strLabelCancel)),
                AlertDialogAction(
                    text = stringResource(Res.string.labelImport),
                    style = AlertDialogActionStyle.Primary,
                    onClick = {
                        val location = importLocation
                        if (location != null) {
                            confirmImport = false
                            scope.launch(Dispatchers.IO) {
                                importBackup(location, selectedScopes(selectedItems))
                            }
                        }
                    },
                ),
            ),
        ) {
            if (ExportKeys.SETTINGS in selectedItems) {
                Text(
                    text = stringResource(Res.string.warnImportSettings),
                    style = typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
