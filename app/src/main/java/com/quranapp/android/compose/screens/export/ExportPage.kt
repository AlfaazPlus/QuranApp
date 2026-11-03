package com.quranapp.android.compose.screens.export

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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quranapp.android.R

@Composable
fun ExportPage(
    onExport: (scopes: Map<String, Boolean>) -> Unit,
) {
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
            onClick = { onExport(selectedScopes(selectedItems)) },
        )
    }
}
