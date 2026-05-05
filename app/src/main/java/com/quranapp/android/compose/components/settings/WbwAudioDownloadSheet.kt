package com.quranapp.android.compose.components.settings

import android.text.format.Formatter
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranapp.android.R
import com.quranapp.android.compose.components.common.IconButton
import com.quranapp.android.compose.components.dialogs.AlertDialog
import com.quranapp.android.compose.components.dialogs.AlertDialogAction
import com.quranapp.android.compose.components.dialogs.AlertDialogActionStyle
import com.quranapp.android.compose.components.dialogs.BottomSheet
import com.quranapp.android.compose.components.reader.navigator.FilterField
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.utils.mediaplayer.WbwAudioDownloadProgressBus
import com.quranapp.android.utils.mediaplayer.WbwAudioRepository
import com.quranapp.android.utils.quran.QuranMeta
import com.quranapp.android.viewModels.WbwAudioDownloadViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun WbwAudioDownloadSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    viewModel: WbwAudioDownloadViewModel,
) {
    if (!isOpen) return

    val context = LocalContext.current

    var confirmBulkOpen by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Pair<Int, String>?>(null) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val progressMap by WbwAudioDownloadProgressBus.state.collectAsState()

    val chapterNames by produceState<Map<Int, String>?>(null) {
        value = withContext(Dispatchers.IO) {
            DatabaseProvider.getQuranRepository(context).getChapterNames(
                QuranMeta.chapterRange.toList(),
            )
        }
    }

    val repository = remember { DatabaseProvider.getQuranRepository(context) }
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

        filteredChapters = if (queryAsInt != null && queryAsInt in QuranMeta.chapterRange) {
            (surahNos + queryAsInt).distinct().sorted()
        } else {
            surahNos
        }
    }

    val reciterWorkActive = uiState.bulkDownloadActive || uiState.hasActiveSingleChapterWork
    val totalChapters = QuranMeta.chapterRange.last
    val canStartBulk = !uiState.bulkDownloadActive &&
            !uiState.hasActiveSingleChapterWork &&
            uiState.downloadedChapters.size < totalChapters

    BottomSheet(
        isOpen = true,
        onDismiss = onDismiss,
        title = stringResource(R.string.wbwAudio),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            if (uiState.bulkDownloadActive) {
                OutlinedButton(
                    onClick = { viewModel.cancelBulkDownload(WbwAudioRepository.AUDIO_ID) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.strLabelCancel))
                }
            } else {
                Button(
                    onClick = { confirmBulkOpen = true },
                    enabled = canStartBulk,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.wbwAudioDownloadAll))
                }
            }
        }

        FilterField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            hint = stringResource(R.string.strHintSearchChapter),
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        if (filteredChapters.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.noResults),
                    style = typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
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
                val busKey = WbwAudioDownloadProgressBus.key(WbwAudioRepository.AUDIO_ID, chapterNo)
                val progress = progressMap[busKey]
                val isDownloaded = chapterNo in uiState.downloadedChapters
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
                            .background(colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = chapterNo.toString(),
                            style = typography.labelMedium,
                            color = colorScheme.primary,
                            fontWeight = FontWeight.Bold,
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
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                val byteLabel = if (progress.totalBytes > 0L) {
                                    stringResource(
                                        R.string.byteProgress,
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
                                    label = "wbwChapterProgress",
                                )

                                if (progress.totalBytes > 0L) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(4.dp)
                                            .clip(CircleShape)
                                            .background(colorScheme.surfaceVariant),
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
                                                            colorScheme.tertiary,
                                                        ),
                                                    ),
                                                ),
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
                                painter = painterResource(R.drawable.dr_icon_delete),
                                contentDescription = stringResource(R.string.strLabelDelete),
                                onClick = { pendingDelete = chapterNo to titleText },
                                enabled = !reciterWorkActive,
                            )
                        }

                        progress != null -> {
                            if (!uiState.bulkDownloadActive) {
                                IconButton(
                                    painter = painterResource(R.drawable.dr_icon_close),
                                    contentDescription = stringResource(R.string.strLabelCancel),
                                    onClick = { viewModel.cancelChapter(chapterNo) },
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

                        chapterNo in uiState.activeChapters -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = colorScheme.primary,
                                trackColor = colorScheme.surfaceVariant,
                            )
                        }

                        else -> {
                            IconButton(
                                painter = painterResource(R.drawable.dr_icon_download),
                                contentDescription = stringResource(R.string.labelDownload),
                                onClick = {
                                    viewModel.downloadChapter(
                                        WbwAudioRepository.AUDIO_ID,
                                        chapterNo
                                    )
                                },
                                enabled = !uiState.bulkDownloadActive,
                            )
                        }
                    }
                }
            }
        }
    }

    if (confirmBulkOpen) {
        AlertDialog(
            isOpen = true,
            onClose = { confirmBulkOpen = false },
            title = stringResource(R.string.wbwAudioDownloadAll),
            actions = listOf(
                AlertDialogAction(text = stringResource(R.string.strLabelCancel)),
                AlertDialogAction(
                    text = stringResource(R.string.labelDownload),
                    style = AlertDialogActionStyle.Primary,
                    dismissOnClick = false,
                    onClick = {
                        viewModel.startBulkDownload(WbwAudioRepository.AUDIO_ID)
                        confirmBulkOpen = false
                    },
                ),
            ),
        ) {
            Text(stringResource(R.string.wbwAudioDownloadAllConfirm))
        }
    }

    val deletePair = pendingDelete
    if (deletePair != null) {
        val (chapterNo, label) = deletePair
        AlertDialog(
            isOpen = true,
            onClose = { pendingDelete = null },
            title = stringResource(R.string.deleteData),
            actions = listOf(
                AlertDialogAction(text = stringResource(R.string.strLabelCancel)),
                AlertDialogAction(
                    text = stringResource(R.string.strLabelDelete),
                    style = AlertDialogActionStyle.Danger,
                    dismissOnClick = false,
                    onClick = {
                        viewModel.deleteChapter(chapterNo)
                        pendingDelete = null
                    },
                ),
            ),
        ) {
            Text(label)
        }
    }
}
