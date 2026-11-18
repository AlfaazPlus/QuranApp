package com.quranapp.android.screens.settings

import android.text.format.Formatter
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quranapp.android.api.models.mediaplayer.RecitationAudioKind
import com.quranapp.android.api.models.recitation2.RecitationModelBase
import com.quranapp.android.api.models.recitation2.RecitationQuranModel
import com.quranapp.android.api.models.recitation2.RecitationTranslationModel
import com.quranapp.android.components.common.AlertCard
import com.quranapp.android.components.common.AppBar
import com.quranapp.android.components.common.ErrorMessageCard
import com.quranapp.android.components.common.IconButton
import com.quranapp.android.components.common.Loader
import com.quranapp.android.components.common.dialogs.AlertDialog
import com.quranapp.android.components.common.dialogs.AlertDialogAction
import com.quranapp.android.components.common.dialogs.AlertDialogActionStyle
import com.quranapp.android.components.common.dialogs.BottomSheet
import com.quranapp.android.components.common.dialogs.MessageDialog
import com.quranapp.android.components.reader.navigator.FilterField
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.extensions.appViewModel
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.byteProgress
import com.quranapp.android.generated.resources.downloadRecitations
import com.quranapp.android.generated.resources.dr_icon_check_circle
import com.quranapp.android.generated.resources.dr_icon_chevron_down
import com.quranapp.android.generated.resources.dr_icon_close
import com.quranapp.android.generated.resources.dr_icon_delete
import com.quranapp.android.generated.resources.dr_icon_download
import com.quranapp.android.generated.resources.dr_icon_mic
import com.quranapp.android.generated.resources.dr_icon_refresh
import com.quranapp.android.generated.resources.labelDownload
import com.quranapp.android.generated.resources.labelTranslation
import com.quranapp.android.generated.resources.noContinueDownload
import com.quranapp.android.generated.resources.noResults
import com.quranapp.android.generated.resources.recitationDownloadAllComplete
import com.quranapp.android.generated.resources.recitationDownloadChaptersProgress
import com.quranapp.android.generated.resources.recitationDownloadConfirmMessage
import com.quranapp.android.generated.resources.recitationDownloadConfirmTitle
import com.quranapp.android.generated.resources.recitationDownloadUpdateConfirmMessage
import com.quranapp.android.generated.resources.recitationDownloadUpdateConfirmTitle
import com.quranapp.android.generated.resources.strHintSearchChapter
import com.quranapp.android.generated.resources.strLabelCancel
import com.quranapp.android.generated.resources.strLabelClose
import com.quranapp.android.generated.resources.strLabelDelete
import com.quranapp.android.generated.resources.strLabelUpdate
import com.quranapp.android.generated.resources.strMsgReciterDownloadHint
import com.quranapp.android.generated.resources.strTitleQuran
import com.quranapp.android.generated.resources.strTitleReaderChapters
import com.quranapp.android.generated.resources.titleCancelDownload
import com.quranapp.android.generated.resources.titleRecitationCleanup
import com.quranapp.android.generated.resources.updateAvailable
import com.quranapp.android.generated.resources.yesCancelDownload
import com.quranapp.android.theme.alpha
import com.quranapp.android.utils.mediaplayer.RecitationDownloadProgressBus
import com.quranapp.android.utils.quran.QuranMeta
import com.quranapp.android.viewmodel.RecitationBatchDownloadState
import com.quranapp.android.viewmodel.RecitationDownloadEvent
import com.quranapp.android.viewmodel.RecitationDownloadUiEvent
import com.quranapp.android.viewmodel.RecitationDownloadViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

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

    data class ConfirmUpdateDownload(
        val kind: RecitationAudioKind,
        val reciterId: String,
        val name: String,
    ) : RecitationPendingDialog()

    data class ConfirmDeleteChapter(
        val kind: RecitationAudioKind,
        val reciterId: String,
        val chapterNo: Int,
        val label: String,
    ) : RecitationPendingDialog()
}

@Composable
fun RecitationDownloadScreen() {
    val viewModel = appViewModel<RecitationDownloadViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    var pendingDialog by remember { mutableStateOf<RecitationPendingDialog?>(null) }
    var message by remember { mutableStateOf<RecitationDownloadUiEvent.ShowMessage?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is RecitationDownloadUiEvent.ShowMessage -> message = event
            }
        }
    }

    Scaffold(
        topBar = {
            AppBar(
                stringResource(Res.string.downloadRecitations),
                shadowElevation = 0.dp,
                actions = {
                    IconButton(
                        painterResource(Res.drawable.dr_icon_refresh),
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
                    onUpdateRequested = { kind, id, name ->
                        pendingDialog =
                            RecitationPendingDialog.ConfirmUpdateDownload(kind, id, name)
                    },
                    onOpenChapterSheet = { kind, id, name ->
                        viewModel.onEvent(
                            RecitationDownloadEvent.OpenChapterSheet(kind, id, name),
                        )
                    },
                )
            }
        }
    }

    ChapterDownloadsSheet(
        viewModel = viewModel,
        onRequestDeleteChapter = { chapterNo, label ->
            val selected = uiState.chapterSheet?.reciter ?: return@ChapterDownloadsSheet

            pendingDialog = RecitationPendingDialog.ConfirmDeleteChapter(
                kind = selected.kind,
                reciterId = selected.id,
                chapterNo = chapterNo,
                label = label,
            )
        },
    )

    val pendingMessage = message
    if (pendingMessage != null) {
        MessageDialog(
            title = pendingMessage.title,
            message = pendingMessage.message,
            confirmText = stringResource(Res.string.strLabelClose),
            onClose = { message = null },
        )
    }

    when (val dialog = pendingDialog) {
        is RecitationPendingDialog.ConfirmDownload -> {
            AlertDialog(
                isOpen = true,
                onClose = { pendingDialog = null },
                title = stringResource(Res.string.recitationDownloadConfirmTitle),
                actions = listOf(
                    AlertDialogAction(
                        text = stringResource(Res.string.strLabelCancel),
                        style = AlertDialogActionStyle.Default,
                        onClick = {},
                    ),
                    AlertDialogAction(
                        text = stringResource(Res.string.labelDownload),
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
                        text = stringResource(Res.string.recitationDownloadConfirmMessage) +
                                "\n\n${dialog.name}",
                        style = typography.bodyMedium,
                    )
                },
            )
        }

        is RecitationPendingDialog.ConfirmCancelDownload -> {
            AlertDialog(
                isOpen = true,
                onClose = { pendingDialog = null },
                title = stringResource(Res.string.titleCancelDownload),
                actions = listOf(
                    AlertDialogAction(
                        text = stringResource(Res.string.noContinueDownload),
                        style = AlertDialogActionStyle.Default,
                        onClick = {},
                    ),
                    AlertDialogAction(
                        text = stringResource(Res.string.yesCancelDownload),
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
                        style = typography.bodyMedium,
                    )
                },
            )
        }

        is RecitationPendingDialog.ConfirmUpdateDownload -> {
            AlertDialog(
                isOpen = true,
                onClose = { pendingDialog = null },
                title = stringResource(Res.string.recitationDownloadUpdateConfirmTitle),
                actions = listOf(
                    AlertDialogAction(
                        text = stringResource(Res.string.strLabelCancel),
                        style = AlertDialogActionStyle.Default,
                        onClick = {},
                    ),
                    AlertDialogAction(
                        text = stringResource(Res.string.strLabelUpdate),
                        style = AlertDialogActionStyle.Primary,
                        onClick = {
                            viewModel.onEvent(
                                RecitationDownloadEvent.UpdateReciter(
                                    dialog.kind,
                                    dialog.reciterId,
                                ),
                            )
                        },
                    ),
                ),
                content = {
                    Text(
                        text = stringResource(Res.string.recitationDownloadUpdateConfirmMessage) +
                                "\n\n${dialog.name}",
                        style = typography.bodyMedium,
                    )
                },
            )
        }

        is RecitationPendingDialog.ConfirmDeleteChapter -> {
            AlertDialog(
                isOpen = true,
                onClose = { pendingDialog = null },
                title = stringResource(Res.string.titleRecitationCleanup),
                actions = listOf(
                    AlertDialogAction(
                        text = stringResource(Res.string.strLabelCancel),
                        style = AlertDialogActionStyle.Default,
                        onClick = {},
                    ),
                    AlertDialogAction(
                        text = stringResource(Res.string.strLabelDelete),
                        style = AlertDialogActionStyle.Danger,
                        onClick = {
                            viewModel.onEvent(
                                RecitationDownloadEvent.DeleteChapter(
                                    dialog.kind,
                                    dialog.reciterId,
                                    dialog.chapterNo,
                                ),
                            )
                        },
                    ),
                ),
                content = {
                    Text(
                        text = dialog.label,
                        style = typography.bodyMedium,
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
    onUpdateRequested: (RecitationAudioKind, String, String) -> Unit,
    onOpenChapterSheet: (RecitationAudioKind, String, String) -> Unit,
) {
    val tabs = listOf(
        Res.string.strTitleQuran,
        Res.string.labelTranslation,
    )
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { tabs.size },
    )
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        SecondaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = colorScheme.surfaceContainer
        ) {
            tabs.forEachIndexed { index, titleRes ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = { Text(stringResource(titleRes), style = typography.labelLarge) },
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
                0 -> ReciterList(
                    reciters = quranReciters,
                    audioKind = RecitationAudioKind.QURAN,
                    getSubtitle = { it.getStyleName() },
                    downloadStates = downloadStates,
                    busyDownloadKey = busyDownloadKey,
                    onDownload = { id, name ->
                        onDownloadRequested(RecitationAudioKind.QURAN, id, name)
                    },
                    onCancel = { id, name ->
                        onCancelRequested(RecitationAudioKind.QURAN, id, name)
                    },
                    onUpdate = { id, name ->
                        onUpdateRequested(RecitationAudioKind.QURAN, id, name)
                    },
                    onOpenChapterSheet = { id, name ->
                        onOpenChapterSheet(RecitationAudioKind.QURAN, id, name)
                    },
                )

                else -> ReciterList(
                    reciters = translationReciters,
                    audioKind = RecitationAudioKind.TRANSLATION,
                    getSubtitle = { it.langName + if (it.book.isNullOrEmpty()) "" else " - ${it.book}" },
                    downloadStates = downloadStates,
                    busyDownloadKey = busyDownloadKey,
                    onDownload = { id, name ->
                        onDownloadRequested(RecitationAudioKind.TRANSLATION, id, name)
                    },
                    onCancel = { id, name ->
                        onCancelRequested(RecitationAudioKind.TRANSLATION, id, name)
                    },
                    onUpdate = { id, name ->
                        onUpdateRequested(RecitationAudioKind.TRANSLATION, id, name)
                    },
                    onOpenChapterSheet = { id, name ->
                        onOpenChapterSheet(RecitationAudioKind.TRANSLATION, id, name)
                    },
                )
            }
        }
    }
}

@Composable
private fun <T : RecitationModelBase> ReciterList(
    reciters: List<T>,
    audioKind: RecitationAudioKind,
    getSubtitle: (T) -> String?,
    downloadStates: Map<String, RecitationBatchDownloadState>,
    busyDownloadKey: String?,
    onDownload: (String, String) -> Unit,
    onCancel: (String, String) -> Unit,
    onUpdate: (String, String) -> Unit,
    onOpenChapterSheet: (String, String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "message") {
            AlertCard {
                Text(
                    text = stringResource(Res.string.strMsgReciterDownloadHint),
                    style = typography.bodyMedium
                )
            }
        }

        items(reciters, key = { it.id }) { reciter ->
            val key = RecitationDownloadViewModel.stateKey(audioKind, reciter.id)
            val state = downloadStates[key] ?: RecitationBatchDownloadState(
                downloadedCount = 0,
                inProgressCount = 0,
            )
            val downloadBlockedByOther =
                busyDownloadKey != null && busyDownloadKey != key

            ReciterDownloadCard(
                title = reciter.getReciterName(),
                subtitle = getSubtitle(reciter),
                state = state,
                downloadBlockedByOther = downloadBlockedByOther,
                onOpenChapters = {
                    onOpenChapterSheet(reciter.id, reciter.getReciterName())
                },
                onDownload = {
                    onDownload(reciter.id, reciter.getReciterName())
                },
                onCancel = {
                    onCancel(reciter.id, reciter.getReciterName())
                },
                onUpdate = {
                    onUpdate(reciter.id, reciter.getReciterName())
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
    onOpenChapters: () -> Unit,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onUpdate: () -> Unit,
) {
    val total = state.totalChapters
    val rawProgress = if (total > 0) state.downloadedCount.toFloat() / total else 0f
    val progress by animateFloatAsState(targetValue = rawProgress, label = "progress")
    val hasActive = state.hasActiveWork
    val isComplete = state.isComplete
    val canRequestNewDownload = !downloadBlockedByOther || hasActive

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.medium)
            .background(colorScheme.surfaceContainerLow)
            .clickable(onClick = onOpenChapters)
            .border(
                width = 1.dp,
                color = colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = shapes.medium,
            ),
    ) {
        Column(
            Modifier
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .animateContentSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            colorScheme.primary.copy(alpha = 0.2f),
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.dr_icon_mic),
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = typography.labelLarge,
                        color = colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = typography.bodyMedium,
                            color = colorScheme.onSurface.alpha(0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    when {
                        state.needsAudioUpdate -> {
                            Text(
                                text = stringResource(Res.string.updateAvailable),
                                style = typography.labelMedium,
                                color = colorScheme.tertiary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        isComplete -> {
                            Text(
                                text = stringResource(Res.string.recitationDownloadAllComplete),
                                style = typography.labelMedium,
                                color = colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        hasActive -> {
                            Text(
                                text = stringResource(
                                    Res.string.recitationDownloadChaptersProgress,
                                    state.downloadedCount,
                                    total,
                                ),
                                style = typography.labelMedium,
                                color = colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        else -> {
                            Text(
                                text = stringResource(
                                    Res.string.recitationDownloadChaptersProgress,
                                    state.downloadedCount,
                                    total,
                                ),
                                style = typography.labelMedium,
                                color = colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Icon(
                    painter = painterResource(Res.drawable.dr_icon_chevron_down),
                    contentDescription = null,
                    tint = colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(18.dp)
                )

                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        state.needsAudioUpdate -> {
                            IconButton(
                                painter = painterResource(Res.drawable.dr_icon_refresh),
                                contentDescription = stringResource(Res.string.strLabelUpdate),
                                onClick = onUpdate,
                            )
                        }

                        isComplete -> {
                            Icon(
                                painter = painterResource(Res.drawable.dr_icon_check_circle),
                                contentDescription = stringResource(Res.string.recitationDownloadAllComplete),
                                tint = colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                            )
                        }

                        hasActive -> {
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    strokeWidth = 2.dp,
                                    color = colorScheme.primary,
                                    trackColor = colorScheme.surfaceVariant,
                                )

                                IconButton(
                                    painter = painterResource(Res.drawable.dr_icon_close),
                                    contentDescription = stringResource(Res.string.strLabelCancel),
                                    onClick = onCancel,
                                )
                            }
                        }

                        else -> {
                            IconButton(
                                painter = painterResource(Res.drawable.dr_icon_download),
                                contentDescription = stringResource(Res.string.labelDownload),
                                onClick = onDownload,
                                enabled = canRequestNewDownload,
                            )
                        }
                    }
                }
            }

            if (hasActive || (!isComplete && state.downloadedCount > 0)) {
                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        colorScheme.primary,
                                        colorScheme.tertiary
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun ChapterDownloadsSheet(
    viewModel: RecitationDownloadViewModel,
    onRequestDeleteChapter: (Int, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheet = uiState.chapterSheet

    if (sheet == null) return

    val context = LocalContext.current

    val selected = sheet.reciter
    val bulkDownloadActive = sheet.bulkDownloadActive
    val downloadedChapters = sheet.downloadedChapters
    val activeChapters = sheet.activeChapters

    val downloadStates = uiState.downloadStates
    val busyDownloadKey = uiState.downloadStates.entries
        .firstOrNull { it.value.hasActiveWork }
        ?.key

    val progressMap by RecitationDownloadProgressBus.state.collectAsState()
    val sheetKey = RecitationDownloadViewModel.stateKey(selected.kind, selected.id)
    val reciterState = downloadStates[sheetKey]
    val reciterWorkActive = reciterState?.hasActiveWork == true
    val downloadBlockedByOther = busyDownloadKey != null && busyDownloadKey != sheetKey

    val chapterNames by produceState<Map<Int, String>?>(null, selected.id) {
        value = withContext(Dispatchers.IO) {
            DatabaseProvider.getQuranRepository().getChapterNames(
                QuranMeta.chapterRange.toList(),
            )
        }
    }

    val repository = remember { DatabaseProvider.getQuranRepository() }
    var searchQuery by remember { mutableStateOf("") }
    var filteredChapters by remember { mutableStateOf(QuranMeta.chapterRange.toList()) }

    LaunchedEffect(searchQuery, chapterNames) {
        val query = searchQuery.trim().lowercase()

        if (query.isBlank()) {
            filteredChapters = QuranMeta.chapterRange.toList()
            return@LaunchedEffect
        }

        val surahNos = withContext(Dispatchers.IO) {
            repository.searchSurahNos(query)
        }

        val queryAsInt = query.toIntOrNull()

        val finalSurahNos = if (queryAsInt != null && queryAsInt in QuranMeta.chapterRange) {
            (surahNos + queryAsInt).distinct().sorted()
        } else {
            surahNos
        }

        filteredChapters = finalSurahNos
    }

    BottomSheet(
        isOpen = true,
        onDismiss = { viewModel.onEvent(RecitationDownloadEvent.CloseChapterSheet) },
        title = stringResource(Res.string.strTitleReaderChapters),
    ) {
        FilterField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            hint = stringResource(Res.string.strHintSearchChapter),
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (filteredChapters.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(Res.string.noResults),
                    style = typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            items(
                items = filteredChapters,
                key = { it },
            ) { chapterNo ->
                val busKey = RecitationDownloadProgressBus.key(selected.id, chapterNo)
                val progress = progressMap[busKey]
                val isDownloaded = chapterNo in downloadedChapters
                val name = chapterNames?.get(chapterNo).orEmpty()
                val titleText = "$chapterNo. $name".trimEnd()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(
                                colorScheme.primary.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chapterNo.toString(),
                            style = typography.labelMedium,
                            color = colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                            .animateContentSize(),
                    ) {
                        Text(
                            text = name,
                            style = typography.bodyLarge,
                            color = colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        if (progress != null) {
                            Spacer(modifier = Modifier.height(2.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                val byteLabel = if (progress.totalBytes > 0L) {
                                    stringResource(
                                        Res.string.byteProgress,
                                        Formatter.formatFileSize(context, progress.bytesDownloaded),
                                        Formatter.formatFileSize(context, progress.totalBytes),
                                    )
                                } else {
                                    Formatter.formatFileSize(context, progress.bytesDownloaded)
                                }

                                val chapterProgress = if (progress.totalBytes > 0) {
                                    progress.bytesDownloaded.toFloat() / progress.totalBytes
                                } else {
                                    0f
                                }

                                val animatedProgress by animateFloatAsState(
                                    targetValue = chapterProgress,
                                    label = "chapterProgress"
                                )

                                if (progress.totalBytes > 0L) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(4.dp)
                                            .clip(CircleShape)
                                            .background(colorScheme.surfaceVariant)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(animatedProgress)
                                                .clip(CircleShape)
                                                .background(
                                                    brush = Brush.horizontalGradient(
                                                        colors = listOf(
                                                            colorScheme.primary,
                                                            colorScheme.tertiary
                                                        )
                                                    )
                                                )
                                        )
                                    }
                                }

                                Text(
                                    text = byteLabel,
                                    style = typography.labelSmall,
                                    color = colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }

                    when {
                        isDownloaded -> {
                            IconButton(
                                painter = painterResource(Res.drawable.dr_icon_delete),
                                contentDescription = stringResource(Res.string.strLabelDelete),
                                onClick = {
                                    onRequestDeleteChapter(chapterNo, titleText)
                                },
                                enabled = !reciterWorkActive,
                            )
                        }

                        progress != null -> {
                            if (!bulkDownloadActive) {
                                IconButton(
                                    painter = painterResource(Res.drawable.dr_icon_close),
                                    contentDescription = stringResource(Res.string.strLabelCancel),
                                    onClick = {
                                        viewModel.onEvent(
                                            RecitationDownloadEvent.CancelChapter(
                                                selected.id,
                                                chapterNo
                                            ),
                                        )
                                    },
                                )
                            } else {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = colorScheme.primary,
                                    trackColor = colorScheme.surfaceVariant,
                                )
                            }
                        }

                        chapterNo in activeChapters -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = colorScheme.primary,
                                trackColor = colorScheme.surfaceVariant,
                            )
                        }

                        else -> {
                            IconButton(
                                painter = painterResource(Res.drawable.dr_icon_download),
                                contentDescription = stringResource(Res.string.labelDownload),
                                onClick = {
                                    viewModel.onEvent(
                                        RecitationDownloadEvent.DownloadChapter(
                                            selected.kind,
                                            selected.id,
                                            chapterNo,
                                        ),
                                    )
                                },
                                enabled = !downloadBlockedByOther,
                            )
                        }
                    }
                }
            }
        }
    }
}

