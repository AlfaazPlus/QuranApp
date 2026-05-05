package com.quranapp.android.compose.screens.settings

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alfaazplus.sunnah.ui.utils.shared_preference.DataStoreManager
import com.quranapp.android.R
import com.quranapp.android.compose.components.common.AlertCard
import com.quranapp.android.compose.components.common.AppBar
import com.quranapp.android.compose.components.common.ErrorMessageCard
import com.quranapp.android.compose.components.common.Loader
import com.quranapp.android.compose.components.common.SwitchItem
import com.quranapp.android.compose.components.dialogs.AlertDialog
import com.quranapp.android.compose.components.dialogs.AlertDialogAction
import com.quranapp.android.compose.components.dialogs.AlertDialogActionStyle
import com.quranapp.android.compose.components.settings.ListItemCategoryLabel
import com.quranapp.android.compose.components.settings.WbwAudioDownloadSheet
import com.quranapp.android.compose.utils.LocalAppLocale
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.utils.managers.ResourceDownloadStatus
import com.quranapp.android.utils.reader.ReaderTextSizeUtils
import com.quranapp.android.utils.univ.MessageUtils
import com.quranapp.android.viewModels.WbwAudioDownloadUiEvent
import com.quranapp.android.viewModels.WbwAudioDownloadViewModel
import com.quranapp.android.viewModels.WbwSettingsUiState
import com.quranapp.android.viewModels.WbwSettingsViewModel
import com.quranapp.android.viewModels.WbwUiModel
import kotlinx.coroutines.launch
import com.quranapp.android.compose.components.common.IconButton as AppIconButton

@Composable
fun SettingsWbwScreen() {
    val viewModel = viewModel<WbwSettingsViewModel>()
    val wbwAudioDownloadViewModel = viewModel<WbwAudioDownloadViewModel>()
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val context = LocalContext.current
    var wbwAudioSheetOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        wbwAudioDownloadViewModel.events.collect { event ->
            when (event) {
                is WbwAudioDownloadUiEvent.ShowMessage -> {
                    MessageUtils.showRemovableToast(
                        context,
                        msg = event.message,
                        Toast.LENGTH_LONG
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            AppBar(
                title = stringResource(R.string.wordByWord),
                actions = {
                    AppIconButton(
                        painterResource(R.drawable.dr_icon_refresh)
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
        it is ResourceDownloadStatus.Started || it is ResourceDownloadStatus.InProgress
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
                item(key = "error") {
                    ErrorMessageCard(
                        error = uiState.error,
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
                    val status = downloadStates[row.info.id] ?: ResourceDownloadStatus.Idle

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
        title = stringResource(R.string.deleteData),
        actions = listOf(
            AlertDialogAction(
                text = stringResource(R.string.strLabelCancel)
            ),
            AlertDialogAction(
                text = stringResource(R.string.strLabelDelete),
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
    downloadStatus: ResourceDownloadStatus,
    viewModel: WbwSettingsViewModel,
    uiState: WbwSettingsUiState,
    isAnyDownloading: Boolean,
    onDeleteRequest: () -> Unit
) {
    val appLocale = LocalAppLocale.current
    val isDownloaded = row.isDownloaded
    val isSelected = row.info.id == uiState.selectedWbwId

    val canSelect = isDownloaded
    val isDownloading = downloadStatus is ResourceDownloadStatus.Started ||
            downloadStatus is ResourceDownloadStatus.InProgress

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
                downloadStatus is ResourceDownloadStatus.InProgress -> String.format(
                    appLocale.platformLocale,
                    $$"%1$s %2$d%%",
                    stringResource(R.string.textDownloading),
                    downloadStatus.progress
                )

                downloadStatus is ResourceDownloadStatus.Started -> stringResource(R.string.textDownloading)
                row.isUpdateAvailable -> stringResource(R.string.strLabelUpdate)
                isDownloaded -> stringResource(R.string.strLabelDownloaded)
                else -> null
            }

            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (downloadStatus is ResourceDownloadStatus.Failed)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        when (downloadStatus) {
            is ResourceDownloadStatus.InProgress -> {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { downloadStatus.progress / 100f },
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
                            painter = painterResource(R.drawable.dr_icon_close),
                            contentDescription = stringResource(R.string.strLabelCancel),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            is ResourceDownloadStatus.Started -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            }

            is ResourceDownloadStatus.Failed -> {
                AppIconButton(
                    painterResource(R.drawable.dr_icon_refresh),
                    onClick = onDownloadOrUpdate
                )
            }

            else -> {
                if (row.isUpdateAvailable || !isDownloaded) {
                    val icon = if (row.isUpdateAvailable) R.drawable.dr_icon_refresh
                    else R.drawable.dr_icon_download

                    AppIconButton(
                        painter = painterResource(icon),
                        enabled = !isAnyDownloading || isDownloading,
                        onClick = onDownloadOrUpdate,
                    )
                } else {
                    AppIconButton(
                        painter = painterResource(R.drawable.dr_icon_delete),
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
            title = R.string.wbwRecitation,
            subtitle = R.string.wbwRecitationMsg,
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
            Text(stringResource(R.string.labelDownload))
            Icon(
                painterResource(R.drawable.dr_icon_download),
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(18.dp)
            )
        }

        HorizontalDivider(Modifier.padding(top = 24.dp, bottom = 12.dp))

        ListItemCategoryLabel(stringResource(R.string.inTooltip))

        SwitchItem(
            title = R.string.wbwShowTranslation,
            checked = showTooltipTr,
            onCheckedChange = { checked ->
                coroutineScope.launch {
                    ReaderPreferences.setWbwTooltipShowTranslation(checked)
                }
            },
        )

        SwitchItem(
            title = R.string.wbwShowTransliteration,
            subtitle = R.string.wbwShowTransliterationMgs,
            checked = showTooltipTrlt,
            onCheckedChange = { checked ->
                coroutineScope.launch {
                    ReaderPreferences.setWbwTooltipShowTransliteration(checked)
                }
            },
        )

        HorizontalDivider(Modifier.padding(top = 24.dp, bottom = 12.dp))

        ListItemCategoryLabel(stringResource(R.string.belowWord))

        SwitchItem(
            title = R.string.wbwShowTranslation,
            checked = showTranslation,
            onCheckedChange = { checked ->
                coroutineScope.launch {
                    ReaderPreferences.setWbwShowTranslation(checked)
                }
            },
        )

        SwitchItem(
            title = R.string.wbwShowTransliteration,
            subtitle = R.string.wbwShowTransliterationMgs,
            checked = showTransliteration,
            onCheckedChange = { checked ->
                coroutineScope.launch {
                    ReaderPreferences.setWbwShowTransliteration(checked)
                }
            },
        )

        HorizontalDivider(Modifier.padding(top = 24.dp, bottom = 12.dp))

        ListItemCategoryLabel(stringResource(R.string.wbwTextSize))

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
                text = String.format(
                    appLocale.platformLocale,
                    "%d%%",
                    wbwProgress.toInt(),
                ),
                modifier = Modifier.padding(start = 10.dp),
                style = MaterialTheme.typography.labelSmall,
            )
        }

        HorizontalDivider(Modifier.padding(top = 24.dp, bottom = 12.dp))

        ListItemCategoryLabel(stringResource(R.string.selectWbwLanguage))

        Spacer(Modifier.height(8.dp))

        AlertCard(
            Modifier.padding(horizontal = 12.dp)
        ) {
            Text(
                stringResource(R.string.noWbwAvailable),
                style = typography.bodyMedium
            )
        }
    }
}
