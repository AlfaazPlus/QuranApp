package com.quranapp.android.screens.export

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.quranapp.android.backup.AutoBackup
import com.quranapp.android.backup.ExportKeys
import com.quranapp.android.backup.disableAutoBackup
import com.quranapp.android.backup.enableAutoBackup
import com.quranapp.android.backup.persistBackupFile
import com.quranapp.android.components.common.AlertCard
import com.quranapp.android.components.common.AppBar
import com.quranapp.android.components.common.ListItem
import com.quranapp.android.components.common.SwitchItem
import com.quranapp.android.components.common.dialogs.BottomSheet
import com.quranapp.android.components.common.dialogs.SimpleTooltip
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.autoBackup
import com.quranapp.android.generated.resources.autoBackupDetails
import com.quranapp.android.generated.resources.autoBackupFailed
import com.quranapp.android.generated.resources.autoBackupNoFile
import com.quranapp.android.generated.resources.autoBackupOpenFailed
import com.quranapp.android.generated.resources.autoBackupSchedule
import com.quranapp.android.generated.resources.changeFile
import com.quranapp.android.generated.resources.dr_icon_settings
import com.quranapp.android.generated.resources.ic_auto_backup
import com.quranapp.android.generated.resources.ic_bookmark
import com.quranapp.android.generated.resources.ic_library
import com.quranapp.android.generated.resources.labelBackupFile
import com.quranapp.android.generated.resources.labelBackupSchedule
import com.quranapp.android.generated.resources.labelCollections
import com.quranapp.android.generated.resources.labelExport
import com.quranapp.android.generated.resources.labelImport
import com.quranapp.android.generated.resources.msgExportBookmarks
import com.quranapp.android.generated.resources.msgExportCollections
import com.quranapp.android.generated.resources.msgExportSettings
import com.quranapp.android.generated.resources.msgImportBookmarks
import com.quranapp.android.generated.resources.msgImportCollections
import com.quranapp.android.generated.resources.msgImportSettings
import com.quranapp.android.generated.resources.strLabelOff
import com.quranapp.android.generated.resources.strLabelOn
import com.quranapp.android.generated.resources.strTitleBookmarks
import com.quranapp.android.generated.resources.strTitleSettings
import com.quranapp.android.generated.resources.titleExportData
import com.quranapp.android.generated.resources.viewFile
import com.quranapp.android.platform.PlatformServices
import com.quranapp.android.preferences.BackupPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.Path.Companion.toPath
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

internal data class ExportOption(
    val key: String,
    val title: StringResource,
    val description: StringResource,
    val icon: DrawableResource,
)

internal fun dataOptions(forExport: Boolean) = listOf(
    ExportOption(
        ExportKeys.SETTINGS,
        Res.string.strTitleSettings,
        if (forExport) Res.string.msgExportSettings else Res.string.msgImportSettings,
        Res.drawable.dr_icon_settings,
    ),
    ExportOption(
        ExportKeys.BOOKMARKS,
        Res.string.strTitleBookmarks,
        if (forExport) Res.string.msgExportBookmarks else Res.string.msgImportBookmarks,
        Res.drawable.ic_bookmark,
    ),
    ExportOption(
        ExportKeys.COLLECTIONS,
        Res.string.labelCollections,
        if (forExport) Res.string.msgExportCollections else Res.string.msgImportCollections,
        Res.drawable.ic_library,
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
    val tabs = listOf(Res.string.labelExport, Res.string.labelImport)
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
                title = stringResource(Res.string.titleExportData),
                shadowElevation = 0.dp,
                actions = {
                    val label = stringResource(Res.string.autoBackup)

                    SimpleTooltip(text = label) {
                        IconButton(onClick = { autoBackupOpen = true }) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_auto_backup),
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
    val documentPath = BackupPreferences.observeDocumentPath()
    val fileName = remember(documentPath) {
        documentPath.takeIf { it.isNotBlank() }?.let { AutoBackup.displayName(it.toPath()) }
    }

    LaunchedEffect(documentPath) {
        if (documentPath.isBlank()) return@LaunchedEffect
        withContext(Dispatchers.IO) { AutoBackup.ensureAvailable() }
    }

    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        if (!persistBackupFile(context, uri, result.data?.flags ?: 0)) {
            scope.launch {
                PlatformServices.messages.show(
                    getString(Res.string.autoBackupFailed),
                    long = true
                )
            }
            return@rememberLauncherForActivityResult
        }

        scope.launch(Dispatchers.IO) {
            enableAutoBackup(uri.toString())
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
        val opened = documentPath.isNotBlank() && AutoBackup.openFile(documentPath.toPath())

        if (!opened) {
            scope.launch {
                PlatformServices.messages.show(
                    getString(Res.string.autoBackupOpenFailed),
                    long = true
                )
            }
        }
    }

    BottomSheet(
        isOpen = isOpen,
        onDismiss = onDismiss,
        icon = Res.drawable.ic_auto_backup,
        title = stringResource(Res.string.autoBackup),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .padding(bottom = 16.dp),
        ) {
            AlertCard(
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Text(
                    text = stringResource(Res.string.autoBackupDetails),
                    style = typography.bodyMedium,
                )
            }

            SwitchItem(
                modifier = Modifier.padding(8.dp),
                title = Res.string.autoBackup,
                subtitle = if (enabled) Res.string.strLabelOn else Res.string.strLabelOff,
                checked = enabled,
                onCheckedChange = { checked ->
                    if (checked) {
                        pickFile()
                    } else {
                        scope.launch(Dispatchers.IO) {
                            disableAutoBackup()
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
                        label = stringResource(Res.string.labelBackupFile),
                        value = fileName ?: stringResource(Res.string.autoBackupNoFile),
                        emphasized = fileName != null && enabled,
                    )

                    HorizontalDivider(color = colorScheme.outlineVariant)

                    BackupInfoRow(
                        label = stringResource(Res.string.labelBackupSchedule),
                        value = stringResource(Res.string.autoBackupSchedule),
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorScheme.surfaceContainer)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = viewFile,
                enabled = fileName != null,
            ) {
                Text(stringResource(Res.string.viewFile))
            }

            Button(
                modifier = Modifier.weight(1f),
                onClick = pickFile,
            ) {
                Text(stringResource(Res.string.changeFile))
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
    icon: DrawableResource,
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
