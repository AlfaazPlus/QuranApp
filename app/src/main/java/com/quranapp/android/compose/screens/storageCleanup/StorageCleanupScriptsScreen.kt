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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.compose.components.dialogs.AlertDialog
import com.quranapp.android.compose.components.dialogs.AlertDialogAction
import com.quranapp.android.compose.components.dialogs.AlertDialogActionStyle
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.utils.reader.QuranScriptUtils
import com.quranapp.android.utils.reader.getQuranScriptName
import com.quranapp.android.utils.univ.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class ScriptCleanupRow(
    val scriptKey: String,
    val fontCount: Int,
)

@Composable
fun StorageCleanupScriptsScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fileUtils = remember(context) { FileUtils.newInstance(context) }

    val rows = remember { mutableStateListOf<ScriptCleanupRow>() }
    var rowsLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val list = mutableListOf<ScriptCleanupRow>()

            fileUtils.scriptFontDir.listFiles()?.filter { it.isDirectory }?.forEach { dir ->
                val key = dir.name
                val info = QuranScriptUtils.getKFQPCFontDownloadedCount(context, key)

                if (info.downloaded > 0) {
                    list.add(ScriptCleanupRow(scriptKey = key, fontCount = info.downloaded))
                }
            }

            withContext(Dispatchers.Main) {
                rows.clear()
                rows.addAll(list)
                rowsLoaded = true
            }
        }
    }

    var pendingDelete by remember { mutableStateOf<ScriptCleanupRow?>(null) }

    val innerModifier = modifier.padding(contentPadding)

    when {
        !rowsLoaded -> {
            Column(
                modifier = innerModifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }
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
            ) {
                items(rows, key = { it.scriptKey }) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = row.scriptKey.getQuranScriptName(),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = stringResource(
                                    R.string.nScriptsAndFonts,
                                    0,
                                    row.fontCount,
                                ),
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
        title = stringResource(R.string.titleScriptCleanup),
        actions = listOf(
            AlertDialogAction(text = stringResource(R.string.strLabelCancel)),
            AlertDialogAction(
                text = stringResource(R.string.strLabelDelete),
                style = AlertDialogActionStyle.Danger,
                onClick = {
                    toDelete?.let { row ->
                        scope.launch(Dispatchers.IO) {
                            if (ReaderPreferences.getQuranScript() == row.scriptKey) {
                                ReaderPreferences.setQuranScriptWithVariant(
                                    QuranScriptUtils.SCRIPT_DEFAULT,
                                    null,
                                )
                            }
                            fileUtils.getScriptFile(row.scriptKey).delete()
                            fileUtils.getKFQPCScriptFontDir(row.scriptKey).deleteRecursively()
                            withContext(Dispatchers.Main) {
                                rows.remove(row)
                            }
                        }
                    }
                    pendingDelete = null
                },
            ),
        ),
    ) {
        if (toDelete != null) {
            Text(
                text = stringResource(
                    R.string.msgScriptCleanup,
                    toDelete.scriptKey.getQuranScriptName(),
                ),
            )
        }
    }
}
