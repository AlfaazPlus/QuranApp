package com.quranapp.android.compose.screens

import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.activities.ExportKeys
import com.quranapp.android.compose.components.AppBar

@Composable
fun ExportImportScreen(
    exportCallback: (scopes: Map<String, Boolean>) -> Unit,
    importCallback: (scopes: Map<String, Boolean>) -> Unit,
) {
    Scaffold(
        topBar = {
            AppBar(
                stringResource(R.string.titleExportData)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(it)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ExportImportCard(
                mapOf(
                    ExportKeys.SETTINGS to true,
                    ExportKeys.BOOKMARKS to true
                ),
                R.string.labelImportExportEverything,
                R.string.warnImportSettings,
                importCallback,
                exportCallback,
            )
            ExportImportCard(
                mapOf(
                    ExportKeys.SETTINGS to true,
                ),
                R.string.labelImportExportSettings,
                R.string.warnImportSettings,
                importCallback,
                exportCallback,
            )
            ExportImportCard(
                mapOf(
                    ExportKeys.BOOKMARKS to true
                ),
                R.string.labelImportExportBookmarks,
                R.string.msgExportImportBookmarks,
                importCallback,
                exportCallback,
            )
        }
    }
}

@Composable
private fun ExportImportCard(
    scopes: Map<String, Boolean>,
    title: Int,
    description: Int,
    importCallback: (scopes: Map<String, Boolean>) -> Unit,
    exportCallback: (scopes: Map<String, Boolean>) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                modifier = Modifier.padding(top = 16.dp),
            ) {
                Button(onClick = { importCallback(scopes) }) {
                    Text(text = stringResource(R.string.labelImport))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { exportCallback(scopes) }) {
                    Text(text = stringResource(R.string.labelExport))
                }
            }
        }
    }
}