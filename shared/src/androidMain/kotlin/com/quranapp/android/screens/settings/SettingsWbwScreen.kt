package com.quranapp.android.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranapp.android.components.common.AlertCard
import com.quranapp.android.components.common.AppBar
import com.quranapp.android.components.common.ErrorMessageCard
import com.quranapp.android.components.common.Loader
import com.quranapp.android.components.common.SwitchItem
import com.quranapp.android.components.common.dialogs.AlertDialog
import com.quranapp.android.components.common.dialogs.AlertDialogAction
import com.quranapp.android.components.common.dialogs.AlertDialogActionStyle
import com.quranapp.android.components.settings.ListItemCategoryLabel
import com.quranapp.android.components.settings.WbwAudioDownloadSheet
import com.quranapp.android.extensions.appViewModel
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.belowWord
import com.quranapp.android.generated.resources.deleteData
import com.quranapp.android.generated.resources.dr_icon_close
import com.quranapp.android.generated.resources.dr_icon_delete
import com.quranapp.android.generated.resources.dr_icon_download
import com.quranapp.android.generated.resources.dr_icon_refresh
import com.quranapp.android.generated.resources.inTooltip
import com.quranapp.android.generated.resources.labelDownload
import com.quranapp.android.generated.resources.noWbwAvailable
import com.quranapp.android.generated.resources.selectWbwLanguage
import com.quranapp.android.generated.resources.strLabelCancel
import com.quranapp.android.generated.resources.strLabelDelete
import com.quranapp.android.generated.resources.strLabelDownloaded
import com.quranapp.android.generated.resources.textDownloading
import com.quranapp.android.generated.resources.updateAvailable
import com.quranapp.android.generated.resources.wbwRecitation
import com.quranapp.android.generated.resources.wbwRecitationMsg
import com.quranapp.android.generated.resources.wbwShowTranslation
import com.quranapp.android.generated.resources.wbwShowTransliteration
import com.quranapp.android.generated.resources.wbwShowTransliterationMgs
import com.quranapp.android.generated.resources.wbwTextSize
import com.quranapp.android.generated.resources.wordByWord
import com.quranapp.android.locale.LocalAppLocale
import com.quranapp.android.platform.Platform
import com.quranapp.android.preferences.DataStoreManager
import com.quranapp.android.preferences.ReaderPreferences
import com.quranapp.android.task.TaskState
import com.quranapp.android.utils.reader.ReaderTextSizeUtils
import com.quranapp.android.viewmodel.WbwAudioDownloadUiEvent
import com.quranapp.android.viewmodel.WbwAudioDownloadViewModel
import com.quranapp.android.viewmodel.WbwSettingsUiState
import com.quranapp.android.viewmodel.WbwSettingsViewModel
import com.quranapp.android.viewmodel.WbwUiModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.quranapp.android.components.common.IconButton as AppIconButton

@Composable
fun SettingsWbwScreen() {
    val viewModel = appViewModel<WbwSettingsViewModel>()
    val wbwAudioDownloadViewModel = appViewModel<WbwAudioDownloadViewModel>()
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    var wbwAudioSheetOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        wbwAudioDownloadViewModel.events.collect { event ->
            when (event) {
                is WbwAudioDownloadUiEvent.ShowMessage -> {
                    Platform.messages.show(event.message, long = true)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            AppBar(
                title = stringResource(Res.string.wordByWord),
                actions = {
                    AppIconButton(
                        painterResource(Res.drawable.dr_icon_refresh)
                    ) {
                        viewModel.load(true)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            WbwRows(
                viewModel,
                uiState,
                onOpenWbwAudioDownloadSheet = { wbwAudioSheetOpen = true },
            )
        }

        WbwAudioDownloadSheet(
            isOpen = wbwAudioSheetOpen,
            onDismiss = { wbwAudioSheetOpen = false },
            viewModel = wbwAudioDownloadViewModel,
        )
    }
}

@Composable
private fun WbwRows(
    viewModel: WbwSettingsViewModel,
    uiState: WbwSettingsUiState,
    onOpenWbwAudioDownloadSheet: () -> Unit,
) {
    var deleteDialogData by remember { mutableStateOf<WbwUiModel?>(null) }
    val downloadStates = uiState.downloadStates
    val rows = uiState.rows

    val isAnyDownloading = downloadStates.values.any {
        it.isEnqueued || it.isRunning
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 64.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        item {
            Configurations(onOpenWbwAudioDownloadSheet = onOpenWbwAudioDownloadSheet)
        }

        when {
            uiState.isLoading -> {
                item(key = "loading") {
                    Loader(fill = true)
                }
            }

            uiState.error != null -> {
                val error = uiState.error
                item(key = "error") {
                    ErrorMessageCard(
                        error = error!!,
                        onRetry = {
                            viewModel.load(true)
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                }
            }

            else -> {
                items(rows, key = { it.info.id }) { row ->
                    val status = downloadStates[row.info.id]

                    WbwRow(
                        row,
                        status,
                        viewModel,
                        uiState,
                        isAnyDownloading,
                        onDeleteRequest = {
                            deleteDialogData = row
                        }
                    )
                }
            }
        }

    }

    AlertDialog(
        isOpen = deleteDialogData != null,
        onClose = { deleteDialogData = null },
        title = stringResource(Res.string.deleteData),
        actions = listOf(
            AlertDialogAction(
                text = stringResource(Res.string.strLabelCancel)
            ),
            AlertDialogAction(
                text = stringResource(Res.string.strLabelDelete),
                style = AlertDialogActionStyle.Danger,
                dismissOnClick = false,
                onClick = {
                    if (deleteDialogData != null) {
                        viewModel.deleteWbwData(deleteDialogData!!.info.id)
                    }

                    deleteDialogData = null
                }
            )
        )
    ) {
        Text(deleteDialogData?.info?.langName ?: "")
    }
}

@Composable
private fun WbwRow(
    row: WbwUiModel,
    downloadStatus: TaskState?,
    viewModel: WbwSettingsViewModel,
    uiState: WbwSettingsUiState,
    isAnyDownloading: Boolean,
    onDeleteRequest: () -> Unit
) {
    val appLocale = LocalAppLocale.current
    val isDownloaded = row.isDownloaded
    val isSelected = row.info.id == uiState.selectedWbwId

    val canSelect = isDownloaded
    val isDownloading = downloadStatus?.isEnqueued == true || downloadStatus?.isRunning == true

    val onSelect = {
        viewModel.selectLanguage(row.info.id)
    }

    val onDownloadOrUpdate = {
        viewModel.startDownload(row.info.id)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (canSelect) onSelect()
            }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = {
                if (canSelect) onSelect()
            },
            enabled = canSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary
            )
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = row.info.langName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            val subtitle = when {
                downloadStatus is TaskState.Running -> appLocale.format(
                    $$"%1$s %2$d%%",
                    stringResource(Res.string.textDownloading),
                    downloadStatus.progress?.percentage ?: 0
                )

                downloadStatus is TaskState.Enqueued -> stringResource(Res.string.textDownloading)
                row.isUpdateAvailable -> stringResource(Res.string.updateAvailable)
                isDownloaded -> stringResource(Res.string.strLabelDownloaded)
                else -> null
            }

            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (downloadStatus is TaskState.Failed)
                        colorScheme.error
                    else if (row.isUpdateAvailable)
                        colorScheme.tertiary
                    else
                        colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        when (downloadStatus) {
            is TaskState.Running -> {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { (downloadStatus.progress?.percentage ?: 0) / 100f },
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                    IconButton(
                        onClick = {
                            viewModel.cancelDownload(row.info.id)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.dr_icon_close),
                            contentDescription = stringResource(Res.string.strLabelCancel),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            TaskState.Enqueued -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            }

            is TaskState.Failed -> {
                AppIconButton(
                    painterResource(Res.drawable.dr_icon_refresh),
                    onClick = onDownloadOrUpdate
                )
            }

            else -> {
                if (row.isUpdateAvailable || !isDownloaded) {
                    val icon = if (row.isUpdateAvailable) Res.drawable.dr_icon_refresh
                    else Res.drawable.dr_icon_download

                    AppIconButton(
                        painter = painterResource(icon),
                        enabled = !isAnyDownloading || isDownloading,
                        onClick = onDownloadOrUpdate,
                        tint = if (row.isUpdateAvailable) colorScheme.tertiary else colorScheme.onBackground
                    )
                } else {
                    AppIconButton(
                        painter = painterResource(Res.drawable.dr_icon_delete),
                        enabled = !isAnyDownloading,
                        tint = colorScheme.error,
                        onClick = onDeleteRequest
                    )
                }
            }
        }
    }
}

@Composable
private fun Configurations(onOpenWbwAudioDownloadSheet: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val appLocale = LocalAppLocale.current

    val prefs by DataStoreManager.flowMultiple(
        ReaderPreferences.KEY_WBW_SHOW_TRANSLATION,
        ReaderPreferences.KEY_WBW_SHOW_TRANSLITERATION,
        ReaderPreferences.KEY_WBW_TOOLTIP_SHOW_TRANSLATION,
        ReaderPreferences.KEY_WBW_TOOLTIP_SHOW_TRANSLITERATION,
        ReaderPreferences.KEY_WBW_RECITATION,
        ReaderPreferences.KEY_TEXT_SIZE_MULT_WBW,
    ).collectAsStateWithLifecycle(null)

    val preferences = prefs ?: return

    val showTranslation = preferences.get(ReaderPreferences.KEY_WBW_SHOW_TRANSLATION)
    val showTransliteration = preferences.get(ReaderPreferences.KEY_WBW_SHOW_TRANSLITERATION)
    val showTooltipTr = preferences.get(ReaderPreferences.KEY_WBW_TOOLTIP_SHOW_TRANSLATION)
    val showTooltipTrlt = preferences.get(ReaderPreferences.KEY_WBW_TOOLTIP_SHOW_TRANSLITERATION)
    val recitation = preferences.get(ReaderPreferences.KEY_WBW_RECITATION)
    val wbwTextMult = preferences.get(ReaderPreferences.KEY_TEXT_SIZE_MULT_WBW)

    val min = 100
    val max = 160
    val steps = max - min
    val wbwProgress = wbwTextMult * 100

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 16.dp)
    ) {
        SwitchItem(
            title = Res.string.wbwRecitation,
            subtitle = Res.string.wbwRecitationMsg,
            checked = recitation,
            onCheckedChange = { checked ->
                coroutineScope.launch {
                    ReaderPreferences.setWbwRecitationEnabled(checked)
                }
            },
        )

        TextButton(
            onClick = onOpenWbwAudioDownloadSheet,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .height(36.dp),
            colors = ButtonDefaults.textButtonColors(
                containerColor = colorScheme.surfaceContainer,
                contentColor = colorScheme.onSurfaceVariant
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
        ) {
            Text(stringResource(Res.string.labelDownload))
            Icon(
                painterResource(Res.drawable.dr_icon_download),
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(18.dp)
            )
        }

        HorizontalDivider(Modifier.padding(top = 24.dp, bottom = 12.dp))

        ListItemCategoryLabel(stringResource(Res.string.inTooltip))

        SwitchItem(
            title = Res.string.wbwShowTranslation,
            checked = showTooltipTr,
            onCheckedChange = { checked ->
                coroutineScope.launch {
                    ReaderPreferences.setWbwTooltipShowTranslation(checked)
                }
            },
        )

        SwitchItem(
            title = Res.string.wbwShowTransliteration,
            subtitle = Res.string.wbwShowTransliterationMgs,
            checked = showTooltipTrlt,
            onCheckedChange = { checked ->
                coroutineScope.launch {
                    ReaderPreferences.setWbwTooltipShowTransliteration(checked)
                }
            },
        )

        HorizontalDivider(Modifier.padding(top = 24.dp, bottom = 12.dp))

        ListItemCategoryLabel(stringResource(Res.string.belowWord))

        SwitchItem(
            title = Res.string.wbwShowTranslation,
            checked = showTranslation,
            onCheckedChange = { checked ->
                coroutineScope.launch {
                    ReaderPreferences.setWbwShowTranslation(checked)
                }
            },
        )

        SwitchItem(
            title = Res.string.wbwShowTransliteration,
            subtitle = Res.string.wbwShowTransliterationMgs,
            checked = showTransliteration,
            onCheckedChange = { checked ->
                coroutineScope.launch {
                    ReaderPreferences.setWbwShowTransliteration(checked)
                }
            },
        )

        HorizontalDivider(Modifier.padding(top = 24.dp, bottom = 12.dp))

        ListItemCategoryLabel(stringResource(Res.string.wbwTextSize))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Slider(
                modifier = Modifier.weight(1f),
                value = wbwProgress,
                onValueChange = { v ->
                    coroutineScope.launch {
                        ReaderPreferences.setWbwTextSizeMultiplier(
                            ReaderTextSizeUtils.calculateMultiplier(v.toInt(), min, max),
                        )
                    }
                },
                valueRange = min.toFloat()..max.toFloat(),
                steps = steps,
            )
            Text(
                text = appLocale.format(
                    "%d%%",
                    wbwProgress.toInt(),
                ),
                modifier = Modifier.padding(start = 10.dp),
                style = MaterialTheme.typography.labelSmall,
            )
        }

        HorizontalDivider(Modifier.padding(top = 24.dp, bottom = 12.dp))

        ListItemCategoryLabel(stringResource(Res.string.selectWbwLanguage))

        Spacer(Modifier.height(8.dp))

        AlertCard(
            Modifier.padding(horizontal = 12.dp)
        ) {
            Text(
                stringResource(Res.string.noWbwAvailable),
                style = typography.bodyMedium
            )
        }
    }
}
