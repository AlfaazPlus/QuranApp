package com.quranapp.android.compose.screens.export

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.backup.ExportKeys
import com.quranapp.android.backup.AutoBackup
import com.quranapp.android.backup.BackupPreferences
import com.quranapp.android.compose.components.common.AlertCard
import com.quranapp.android.compose.components.common.AppBar
import com.quranapp.android.compose.components.common.ListItem
import com.quranapp.android.compose.components.common.SwitchItem
import com.quranapp.android.compose.components.dialogs.BottomSheet
import com.quranapp.android.compose.components.dialogs.SimpleTooltip
import com.quranapp.android.backup.disableAutoBackup
import com.quranapp.android.backup.enableAutoBackup
import com.quranapp.android.backup.persistBackupFile
import com.quranapp.android.utils.univ.MessageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.net.toUri

internal data class ExportOption(
    val key: String,
    val title: Int,
    val description: Int,
    val icon: Int,
)

internal fun dataOptions(forExport: Boolean) = listOf(
    ExportOption(
        ExportKeys.SETTINGS,
        R.string.strTitleSettings,
        if (forExport) R.string.msgExportSettings else R.string.msgImportSettings,
        R.drawable.dr_icon_settings,
    ),
    ExportOption(
        ExportKeys.BOOKMARKS,
        R.string.strTitleBookmarks,
        if (forExport) R.string.msgExportBookmarks else R.string.msgImportBookmarks,
        R.drawable.ic_bookmark,
    ),
    ExportOption(
        ExportKeys.COLLECTIONS,
        R.string.labelCollections,
        if (forExport) R.string.msgExportCollections else R.string.msgImportCollections,
        R.drawable.ic_library,
    ),
)

internal val exportSelectionSaver = listSaver<Set<String>, String>(
    save = { value -> value.toList() },
    restore = { saved -> saved.toSet() },
)

internal fun selectedScopes(selectedItems: Set<String>): Map<String, Boolean> {
    return dataOptions(forExport = true).associate { it.key to (it.key in selectedItems) }
}

@Composable
fun ExportImportScreen() {
    val tabs = listOf(R.string.labelExport, R.string.labelImport)
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { tabs.size },
    )
    val coroutineScope = rememberCoroutineScope()
    val autoBackupEnabled = BackupPreferences.observeEnabled()
    var autoBackupOpen by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppBar(
                title = stringResource(R.string.titleExportData),
                shadowElevation = 0.dp,
                actions = {
                    val label = stringResource(R.string.autoBackup)

                    SimpleTooltip(text = label) {
                        IconButton(onClick = { autoBackupOpen = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_auto_backup),
                                contentDescription = label,
                                tint = if (autoBackupEnabled) {
                                    colorScheme.primary
                                } else {
                                    LocalContentColor.current
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            PrimaryTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        text = { Text(stringResource(title), style = typography.labelLarge) },
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
            ) { page ->
                if (page == 0) ExportPage()
                if (page == 1) ImportPage()
            }
        }
    }

    AutoBackupSheet(
        isOpen = autoBackupOpen,
        onDismiss = { autoBackupOpen = false },
    )
}

@Composable
private fun AutoBackupSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val enabled = BackupPreferences.observeEnabled()
    val documentUri = BackupPreferences.observeDocumentUri()
    val fileName = remember(documentUri) {
        documentUri.takeIf { it.isNotBlank() }?.let { AutoBackup.displayName(context, it.toUri()) }
    }

    LaunchedEffect(documentUri) {
        if (documentUri.isBlank()) return@LaunchedEffect
        withContext(Dispatchers.IO) { AutoBackup.ensureAvailable(context) }
    }

    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        if (!persistBackupFile(context, uri, result.data?.flags ?: 0)) {
            MessageUtils.showRemovableToast(
                context,
                R.string.autoBackupFailed,
                Toast.LENGTH_LONG,
            )
            return@rememberLauncherForActivityResult
        }

        scope.launch(Dispatchers.IO) {
            enableAutoBackup(context, uri.toString())
        }
    }

    val pickFile = {
        fileLauncher.launch(
            Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/gzip"
                putExtra(Intent.EXTRA_TITLE, AutoBackup.FILE_NAME)
            },
        )
    }

    val viewFile = {
        val opened = documentUri.isNotBlank() && AutoBackup.openFile(context, documentUri.toUri())
        if (!opened) {
            MessageUtils.showRemovableToast(
                context,
                R.string.autoBackupOpenFailed,
                Toast.LENGTH_LONG,
            )
        }
    }

    BottomSheet(
        isOpen = isOpen,
        onDismiss = onDismiss,
        icon = R.drawable.ic_auto_backup,
        title = stringResource(R.string.autoBackup),
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            AlertCard(
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Text(
                    text = stringResource(R.string.autoBackupDetails),
                    style = typography.bodyMedium,
                )
            }

            SwitchItem(
                modifier = Modifier.padding(8.dp),
                title = R.string.autoBackup,
                subtitle = if (enabled) R.string.strLabelOn else R.string.strLabelOff,
                checked = enabled,
                onCheckedChange = { checked ->
                    if (checked) {
                        pickFile()
                    } else {
                        scope.launch(Dispatchers.IO) {
                            disableAutoBackup(context)
                        }
                    }
                },
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = shapes.medium,
                color = colorScheme.surface,
                border = BorderStroke(1.dp, colorScheme.outlineVariant),
            ) {
                Column {
                    BackupInfoRow(
                        label = stringResource(R.string.labelBackupFile),
                        value = fileName ?: stringResource(R.string.autoBackupNoFile),
                        emphasized = fileName != null && enabled,
                    )

                    HorizontalDivider(color = colorScheme.outlineVariant)

                    BackupInfoRow(
                        label = stringResource(R.string.labelBackupSchedule),
                        value = stringResource(R.string.autoBackupSchedule),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = viewFile,
                    enabled = fileName != null,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(50),
                ) {
                    Text(stringResource(R.string.viewFile))
                }

                Button(
                    onClick = pickFile,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(50),
                ) {
                    Text(stringResource(R.string.changeFile))
                }
            }
        }
    }
}

@Composable
private fun BackupInfoRow(
    label: String,
    value: String,
    detail: String? = null,
    emphasized: Boolean = false,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = typography.labelMedium,
            color = colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = typography.bodyLarge,
            color = if (emphasized) colorScheme.primary else colorScheme.onSurface,
        )
        if (detail != null) {
            Text(
                text = detail,
                style = typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun CategorySelection(
    forExport: Boolean,
    selectedItems: Set<String>,
    onSelectedItemsChange: (Set<String>) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val options = dataOptions(forExport)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shapes.medium,
        color = colorScheme.surface,
        border = BorderStroke(1.dp, colorScheme.outlineVariant),
    ) {
        Column() {
            options.forEachIndexed { index, option ->
                DataOptionRow(
                    option = option,
                    checked = option.key in selectedItems,
                    onCheckedChange = { checked ->
                        onSelectedItemsChange(
                            if (checked) selectedItems + option.key else selectedItems - option.key
                        )
                    },
                )

                if (index < options.lastIndex) {
                    HorizontalDivider(color = colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
internal fun SelectedActionButton(
    text: String,
    icon: Int,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .height(56.dp),
        shape = RoundedCornerShape(50),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = typography.titleMedium,
        )
    }
}

@Composable
private fun DataOptionRow(
    option: ExportOption,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        title = option.title,
        subtitle = option.description,
        leading = {
            Icon(
                painter = painterResource(option.icon),
                contentDescription = null,
                modifier = Modifier.size(26.dp),
            )
        },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        }
    ) { onCheckedChange(!checked) }
}
