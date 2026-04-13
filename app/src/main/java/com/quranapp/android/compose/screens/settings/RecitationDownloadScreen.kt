package com.quranapp.android.compose.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quranapp.android.R
import com.quranapp.android.api.models.mediaplayer.RecitationAudioKind
import com.quranapp.android.api.models.recitation2.RecitationQuranModel
import com.quranapp.android.api.models.recitation2.RecitationTranslationModel
import com.quranapp.android.compose.components.common.AppBar
import com.quranapp.android.compose.components.common.ErrorMessageCard
import com.quranapp.android.compose.components.common.IconButton
import com.quranapp.android.compose.components.common.Loader
import com.quranapp.android.compose.components.dialogs.AlertDialog
import com.quranapp.android.compose.components.dialogs.AlertDialogAction
import com.quranapp.android.compose.components.dialogs.AlertDialogActionStyle
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.utils.univ.MessageUtils
import com.quranapp.android.viewModels.RecitationBatchDownloadState
import com.quranapp.android.viewModels.RecitationDownloadEvent
import com.quranapp.android.viewModels.RecitationDownloadUiEvent
import com.quranapp.android.viewModels.RecitationDownloadViewModel
import kotlinx.coroutines.launch

private sealed class RecitationPendingDialog {
    data class ConfirmDownload(
        val kind: RecitationAudioKind,
        val reciterId: String,
        val name: String,
    ) : RecitationPendingDialog()

    data class ConfirmCancelDownload(
        val kind: RecitationAudioKind,
        val reciterId: String,
        val name: String,
    ) : RecitationPendingDialog()
}

@Composable
fun RecitationDownloadScreen() {
    val context = LocalContext.current
    val resources = LocalResources.current
    val viewModel = viewModel<RecitationDownloadViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    var pendingDialog by remember { mutableStateOf<RecitationPendingDialog?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is RecitationDownloadUiEvent.ShowMessage -> {
                    MessageUtils.popMessage(
                        context,
                        title = event.title,
                        msg = event.message,
                        resources.getString(R.string.strLabelClose),
                        null,
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            AppBar(
                stringResource(R.string.downloadRecitations),
                shadowElevation = 0.dp,
                actions = {
                    IconButton(
                        painterResource(R.drawable.dr_icon_refresh),
                    ) {
                        viewModel.onEvent(RecitationDownloadEvent.Refresh)
                    }
                },
            )
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                uiState.isLoading -> Loader(true)
                uiState.error != null -> ErrorMessageCard(
                    error = uiState.error!!,
                    onRetry = { viewModel.onEvent(RecitationDownloadEvent.Refresh) },
                )

                else -> RecitationDownloadContent(
                    quranReciters = uiState.quranReciters,
                    translationReciters = uiState.translationReciters,
                    downloadStates = uiState.downloadStates,
                    busyDownloadKey = uiState.downloadStates.entries
                        .firstOrNull { it.value.hasActiveWork }
                        ?.key,
                    onDownloadRequested = { kind, id, name ->
                        pendingDialog = RecitationPendingDialog.ConfirmDownload(kind, id, name)
                    },
                    onCancelRequested = { kind, id, name ->
                        pendingDialog =
                            RecitationPendingDialog.ConfirmCancelDownload(kind, id, name)
                    },
                )
            }
        }
    }

    when (val dialog = pendingDialog) {
        is RecitationPendingDialog.ConfirmDownload -> {
            AlertDialog(
                isOpen = true,
                onClose = { pendingDialog = null },
                title = stringResource(R.string.recitationDownloadConfirmTitle),
                actions = listOf(
                    AlertDialogAction(
                        text = stringResource(R.string.strLabelCancel),
                        style = AlertDialogActionStyle.Default,
                        onClick = {},
                    ),
                    AlertDialogAction(
                        text = stringResource(R.string.labelDownload),
                        style = AlertDialogActionStyle.Primary,
                        onClick = {
                            viewModel.onEvent(
                                RecitationDownloadEvent.StartDownload(
                                    dialog.kind,
                                    dialog.reciterId
                                ),
                            )
                        },
                    ),
                ),
                content = {
                    Text(
                        text = stringResource(R.string.recitationDownloadConfirmMessage) +
                                "\n\n${dialog.name}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
            )
        }

        is RecitationPendingDialog.ConfirmCancelDownload -> {
            AlertDialog(
                isOpen = true,
                onClose = { pendingDialog = null },
                title = stringResource(R.string.titleCancelDownload),
                actions = listOf(
                    AlertDialogAction(
                        text = stringResource(R.string.noContinueDownload),
                        style = AlertDialogActionStyle.Default,
                        onClick = {},
                    ),
                    AlertDialogAction(
                        text = stringResource(R.string.yesCancelDownload),
                        style = AlertDialogActionStyle.Danger,
                        onClick = {
                            viewModel.onEvent(
                                RecitationDownloadEvent.CancelDownload(
                                    dialog.kind,
                                    dialog.reciterId
                                ),
                            )
                        },
                    ),
                ),
                content = {
                    Text(
                        text = dialog.name,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
            )
        }

        null -> Unit
    }
}

@Composable
private fun RecitationDownloadContent(
    quranReciters: List<RecitationQuranModel>,
    translationReciters: List<RecitationTranslationModel>,
    downloadStates: Map<String, RecitationBatchDownloadState>,
    busyDownloadKey: String?,
    onDownloadRequested: (RecitationAudioKind, String, String) -> Unit,
    onCancelRequested: (RecitationAudioKind, String, String) -> Unit,
) {
    val tabs = listOf(
        R.string.strTitleQuran,
        R.string.labelTranslation,
    )
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { tabs.size },
    )
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
            tabs.forEachIndexed { index, titleRes ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = { Text(stringResource(titleRes)) },
                    unselectedContentColor = colorScheme.onSurface.alpha(0.8f),
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) { page ->
            when (page) {
                0 -> QuranReciterList(
                    reciters = quranReciters,
                    downloadStates = downloadStates,
                    busyDownloadKey = busyDownloadKey,
                    onDownload = { id, name ->
                        onDownloadRequested(RecitationAudioKind.QURAN, id, name)
                    },
                    onCancel = { id, name ->
                        onCancelRequested(RecitationAudioKind.QURAN, id, name)
                    },
                )

                else -> TranslationReciterList(
                    reciters = translationReciters,
                    downloadStates = downloadStates,
                    busyDownloadKey = busyDownloadKey,
                    onDownload = { id, name ->
                        onDownloadRequested(RecitationAudioKind.TRANSLATION, id, name)
                    },
                    onCancel = { id, name ->
                        onCancelRequested(RecitationAudioKind.TRANSLATION, id, name)
                    },
                )
            }
        }
    }
}

@Composable
private fun QuranReciterList(
    reciters: List<RecitationQuranModel>,
    downloadStates: Map<String, RecitationBatchDownloadState>,
    busyDownloadKey: String?,
    onDownload: (String, String) -> Unit,
    onCancel: (String, String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(reciters, key = { it.id }) { reciter ->
            val key = RecitationDownloadViewModel.stateKey(RecitationAudioKind.QURAN, reciter.id)
            val state = downloadStates[key] ?: RecitationBatchDownloadState(0, 0, 0)
            val downloadBlockedByOther =
                busyDownloadKey != null && busyDownloadKey != key

            ReciterDownloadCard(
                title = reciter.getReciterName(),
                subtitle = reciter.getStyleName(),
                state = state,
                downloadBlockedByOther = downloadBlockedByOther,
                onDownload = {
                    onDownload(reciter.id, reciter.getReciterName())
                },
                onCancel = {
                    onCancel(reciter.id, reciter.getReciterName())
                },
            )
        }
    }
}

@Composable
private fun TranslationReciterList(
    reciters: List<RecitationTranslationModel>,
    downloadStates: Map<String, RecitationBatchDownloadState>,
    busyDownloadKey: String?,
    onDownload: (String, String) -> Unit,
    onCancel: (String, String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(reciters, key = { it.id }) { reciter ->
            val key =
                RecitationDownloadViewModel.stateKey(RecitationAudioKind.TRANSLATION, reciter.id)
            val state = downloadStates[key] ?: RecitationBatchDownloadState(0, 0, 0)
            val downloadBlockedByOther =
                busyDownloadKey != null && busyDownloadKey != key

            ReciterDownloadCard(
                title = reciter.getReciterName(),
                subtitle = reciter.langName + if (reciter.book.isNullOrEmpty()) "" else " - ${reciter.book}",
                state = state,
                downloadBlockedByOther = downloadBlockedByOther,
                onDownload = {
                    onDownload(reciter.id, reciter.getReciterName())
                },
                onCancel = {
                    onCancel(reciter.id, reciter.getReciterName())
                },
            )
        }
    }
}

@Composable
private fun ReciterDownloadCard(
    title: String,
    subtitle: String?,
    state: RecitationBatchDownloadState,
    downloadBlockedByOther: Boolean,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
) {
    val total = state.totalChapters
    val progress = if (total > 0) state.downloadedCount.toFloat() / total else 0f
    val hasActive = state.hasActiveWork
    val isComplete = state.isComplete
    val hasFailed = state.failedCount > 0 && !hasActive && !isComplete
    val canRequestNewDownload = !downloadBlockedByOther || hasActive

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp),
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    when {
                        isComplete -> {
                            Text(
                                text = stringResource(R.string.recitationDownloadAllComplete),
                                style = MaterialTheme.typography.labelMedium,
                                color = colorScheme.primary,
                            )
                        }

                        hasActive -> {
                            Text(
                                text = stringResource(
                                    R.string.recitationDownloadChaptersProgress,
                                    state.downloadedCount,
                                    total,
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = colorScheme.primary,
                            )
                        }

                        hasFailed -> {
                            Text(
                                text = stringResource(R.string.recitationDownloadSomeFailed),
                                style = MaterialTheme.typography.labelMedium,
                                color = colorScheme.error,
                            )
                        }

                        else -> {
                            Text(
                                text = stringResource(
                                    R.string.recitationDownloadChaptersProgress,
                                    state.downloadedCount,
                                    total,
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (hasActive || (!isComplete && state.downloadedCount > 0)) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progress)
                                    .background(colorScheme.primary)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        isComplete -> {
                            Icon(
                                painter = painterResource(R.drawable.dr_icon_check_circle),
                                contentDescription = stringResource(R.string.recitationDownloadAllComplete),
                                tint = colorScheme.primary,
                                modifier = Modifier.size(28.dp),
                            )
                        }

                        hasActive -> {
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.5.dp,
                                    color = colorScheme.primary,
                                    trackColor = colorScheme.surfaceVariant,
                                )
                                IconButton(
                                    painter = painterResource(R.drawable.dr_icon_close),
                                    contentDescription = stringResource(R.string.strLabelCancel),
                                    onClick = onCancel,
                                    small = true
                                )
                            }
                        }

                        hasFailed -> {
                            IconButton(
                                painter = painterResource(R.drawable.dr_icon_refresh),
                                contentDescription = stringResource(R.string.strLabelRetry),
                                onClick = onDownload,
                                enabled = canRequestNewDownload,
                            )
                        }

                        else -> {
                            IconButton(
                                painter = painterResource(R.drawable.dr_icon_download),
                                contentDescription = stringResource(R.string.labelDownload),
                                onClick = onDownload,
                                enabled = canRequestNewDownload,
                            )
                        }
                    }
                }
            }
        }
    }
}

