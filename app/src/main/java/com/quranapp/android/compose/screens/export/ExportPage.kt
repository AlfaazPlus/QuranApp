package com.quranapp.android.compose.screens.export

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.backup.BackupImportService
import com.quranapp.android.utils.Log
import com.quranapp.android.backup.showBackupFailure
import com.quranapp.android.backup.writeExport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ExportPage() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exportBytes = remember { mutableStateOf(ByteArray(0)) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            writeExport(context, uri, exportBytes.value)
        }
    }

    var selectedItems by rememberSaveable(stateSaver = exportSelectionSaver) {
        mutableStateOf(setOf<String>())
    }
    val selectedCount = selectedItems.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            CategorySelection(
                forExport = true,
                selectedItems = selectedItems,
                onSelectedItemsChange = { selectedItems = it },
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        SelectedActionButton(
            text = stringResource(R.string.labelExportSelected, selectedCount),
            icon = R.drawable.ic_upload,
            enabled = selectedCount > 0,
            onClick = {
                scope.launch(Dispatchers.IO) {
                    try {
                        exportBytes.value = BackupImportService(context).exportBytes(
                            selectedScopes(selectedItems),
                        )
                        withContext(Dispatchers.Main) {
                            exportLauncher.launch(
                                Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                    type = "application/gzip"
                                    putExtra(Intent.EXTRA_TITLE, "quranapp-backup.json.gz")
                                },
                            )
                        }
                    } catch (error: Exception) {
                        Log.saveError(error, "exportData")
                        withContext(Dispatchers.Main) {
                            showBackupFailure(context, error)
                        }
                    }
                }
            },
        )
    }
}
