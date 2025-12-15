package com.quranapp.android.compose.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.peacedesign.android.utils.ColorUtils
import com.peacedesign.android.widget.dialog.base.PeaceDialog
import com.quranapp.android.R
import com.quranapp.android.api.models.tafsir.TafsirInfoModel
import com.quranapp.android.components.tafsir.TafsirGroupModel
import com.quranapp.android.compose.components.ErrorMessageCard
import com.quranapp.android.utils.univ.MessageUtils
import com.quranapp.android.viewModels.TafsirDownloadState
import com.quranapp.android.viewModels.TafsirEvent
import com.quranapp.android.viewModels.TafsirUiState
import com.quranapp.android.viewModels.TafsirViewModel

@Composable
fun TafsirSelectionScreen(
    uiState: TafsirUiState,
    modifier: Modifier = Modifier
) {
    val viewModel = viewModel<TafsirViewModel>()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            uiState.isLoading -> LoadingState()
            uiState.error != null -> ErrorMessageCard(
                error = uiState.error,
                onRetry = { viewModel.onEvent(TafsirEvent.Refresh) }
            )

            else -> TafsirContent(
                groups = uiState.tafsirGroups,
                selectedKey = uiState.selectedTafsirKey,
                downloadStates = uiState.downloadStates,
                downloadedTafsirKeys = uiState.downloadedTafsirKeys
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(36.dp),
            strokeWidth = 3.dp
        )
    }
}

@Composable
private fun TafsirContent(
    groups: List<TafsirGroupModel>,
    selectedKey: String?,
    downloadStates: Map<String, TafsirDownloadState>,
    downloadedTafsirKeys: Set<String>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(groups, key = { it.langCode }) { group ->
            LanguageGroupCard(
                group = group,
                selectedKey = selectedKey,
                downloadStates = downloadStates,
                downloadedTafsirKeys = downloadedTafsirKeys
            )
        }

        item { Spacer(modifier = Modifier.height(60.dp)) }
    }
}

@Composable
private fun LanguageGroupCard(
    group: TafsirGroupModel,
    selectedKey: String?,
    downloadStates: Map<String, TafsirDownloadState>,
    downloadedTafsirKeys: Set<String>
) {
    val viewModel = viewModel<TafsirViewModel>()
    val isAnyDownloading = downloadStates.values.any {
        it is TafsirDownloadState.Started || it is TafsirDownloadState.Downloading
    }

    val hasSelection = remember(group.tafsirs, selectedKey) {
        group.tafsirs.any { it.key == selectedKey }
    }

    val chevronRotation by animateFloatAsState(
        targetValue = if (group.isExpanded) 90f else 0f,
        animationSpec = tween(250),
        label = "chevron"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { viewModel.onEvent(TafsirEvent.ToggleGroup(group.langCode)) })
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = group.langName,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (hasSelection) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text =
                                stringResource(
                                    if (group.tafsirs.size > 1)
                                        R.string.nItems else
                                        R.string.nItem, group.tafsirs.size
                                ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    painter = painterResource(R.drawable.dr_icon_chevron_right),
                    contentDescription = if (group.isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(chevronRotation)
                )
            }

            AnimatedVisibility(
                visible = group.isExpanded,
                enter = expandVertically(tween(250)) + fadeIn(tween(200)),
                exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
            ) {
                Column {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        thickness = 0.5.dp
                    )

                    group.tafsirs.forEachIndexed { index, tafsir ->
                        val downloadState = downloadStates[tafsir.key] ?: TafsirDownloadState.Idle
                        val isDownloaded = downloadedTafsirKeys.contains(tafsir.key)

                        TafsirRow(
                            tafsir = tafsir,
                            isSelected = tafsir.key == selectedKey,
                            isDownloaded = isDownloaded,
                            isAnyDownloading = isAnyDownloading,
                            downloadState = downloadState,
                            onSelect = { viewModel.onEvent(TafsirEvent.SelectTafsir(tafsir.key)) },
                            onDownload = {
                                if (!isAnyDownloading) {
                                    viewModel.onEvent(TafsirEvent.DownloadTafsir(tafsir.key))
                                }
                            },
                            onCancelDownload = { viewModel.onEvent(TafsirEvent.CancelDownload(tafsir.key)) },
                            onDelete = { viewModel.onEvent(TafsirEvent.DeleteTafsir(tafsir.key)) }
                        )

                        if (index < group.tafsirs.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 52.dp, end = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                thickness = 0.5.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TafsirRow(
    tafsir: TafsirInfoModel,
    isSelected: Boolean,
    isDownloaded: Boolean,
    isAnyDownloading: Boolean,
    downloadState: TafsirDownloadState,
    onSelect: () -> Unit,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = MaterialTheme.colorScheme.primary),
                    onClick = onSelect
                )
                .padding(start = 4.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tafsir.name,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Downloaded indicator
                    if (isDownloaded && downloadState == TafsirDownloadState.Idle) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            painter = painterResource(R.drawable.dr_icon_check),
                            contentDescription = stringResource(R.string.strLabelDownloaded),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                when (downloadState) {
                    is TafsirDownloadState.Downloading -> {
                        Text(
                            text = stringResource(R.string.textDownloading) + " ${downloadState.progress}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    is TafsirDownloadState.Started -> {
                        Text(
                            text = stringResource(R.string.textDownloading),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    else -> {
                        Text(
                            text = tafsir.author,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        VerticalDivider(
            modifier = Modifier
                .height(38.dp)
                .width(1.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            thickness = 1.dp
        )

        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            when (downloadState) {
                is TafsirDownloadState.Started -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                is TafsirDownloadState.Downloading -> {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { downloadState.progress / 100f },
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        IconButton(
                            onClick = {
                                PeaceDialog.newBuilder(context)
                                    .setTitle(R.string.titleCancelDownload)
                                    .setMessage("")
                                    .setPositiveButton(
                                        R.string.yesCancelDownload,
                                        ColorUtils.DANGER
                                    ) { _, _ ->
                                        onCancelDownload()
                                    }
                                    .setNegativeButton(R.string.noContinueDownload, null)
                                    .show()
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.dr_icon_close),
                                contentDescription = stringResource(R.string.strLabelCancel),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                is TafsirDownloadState.Failed -> {
                    // Retry button
                    IconButton(
                        onClick = onDownload,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.dr_icon_refresh),
                            contentDescription = stringResource(R.string.strLabelRetry),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                else -> {
                    if (isDownloaded) {
                        IconButton(
                            onClick = {
                                MessageUtils.showConfirmationDialog(
                                    context = context,
                                    title = context.getString(R.string.strTitleDeleteTafsir),
                                    msg = context.getString(
                                        R.string.strMsgDeleteTafsir,
                                        tafsir.name
                                    ),
                                    btn = context.getString(R.string.strLabelDelete),
                                    btnColor = ColorUtils.DANGER,
                                    action = {
                                        onDelete()
                                    }
                                )
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.dr_icon_delete),
                                contentDescription = stringResource(R.string.strLabelDelete),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = onDownload,
                            modifier = Modifier.size(40.dp),
                            enabled = !isAnyDownloading
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.dr_icon_download),
                                contentDescription = stringResource(R.string.labelDownload),
                                tint = if (isAnyDownloading) colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                else colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
