package com.quranapp.android.compose.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.quranapp.android.R
import com.quranapp.android.activities.ActivityReader
import com.quranapp.android.compose.components.common.AppBar
import com.quranapp.android.compose.components.common.MessageCard
import com.quranapp.android.compose.components.dialogs.AlertDialog
import com.quranapp.android.compose.components.dialogs.AlertDialogAction
import com.quranapp.android.compose.components.dialogs.AlertDialogActionStyle
import com.quranapp.android.compose.components.dialogs.SimpleTooltip
import com.quranapp.android.compose.components.reader.ReaderMode
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.compose.utils.formattedStringResource
import com.quranapp.android.db.entities.user.ReadHistoryEntity
import com.quranapp.android.utils.reader.ReadType
import com.quranapp.android.utils.reader.factory.ReaderFactory
import com.quranapp.android.utils.reader.getQuranScriptName
import com.quranapp.android.viewModels.ReadHistoryViewModel
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private enum class SwipeRevealValue {
    Closed,
    Open,
}

@Composable
fun ReadHistoryScreen(vm: ReadHistoryViewModel = viewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val chapterNames by vm.chapterNames.collectAsStateWithLifecycle()
    val mainHistory by vm.mainHistory.collectAsStateWithLifecycle()
    val allHistories = vm.allHistories.collectAsLazyPagingItems()

    var clearAllRequested by remember { mutableStateOf(false) }

    val openHistory: (ReadHistoryEntity) -> Unit = { history ->
        ReaderFactory.prepareHistoryIntent(history)?.let {
            it.setClass(context, ActivityReader::class.java)
            context.startActivity(it)
        }
    }

    ClearAllHistoryDialog(
        isOpen = clearAllRequested,
        onDismiss = { clearAllRequested = false },
        onConfirm = {
            scope.launch { vm.deleteAllHistories() }
        },
    )

    Scaffold(
        topBar = {
            AppBar(
                title = stringResource(R.string.strTitleReadHistory),
                actions = {
                    if (allHistories.itemCount > 0) {
                        SimpleTooltip(text = stringResource(R.string.msgClearReadHistory)) {
                            IconButton(onClick = { clearAllRequested = true }) {
                                Icon(
                                    painter = painterResource(R.drawable.dr_icon_delete),
                                    contentDescription = stringResource(R.string.msgClearReadHistory),
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            mainHistory == null && allHistories.itemCount == 0 -> {
                MessageCard(
                    icon = R.drawable.dr_icon_history,
                    message = stringResource(R.string.strMsgReadHistoryNoItems),
                    modifier = Modifier.padding(paddingValues),
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 12.dp,
                        bottom = 64.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    mainHistory?.let { main ->
                        item(key = "main_${main.id}") {
                            MainSessionCard(
                                history = main,
                                chapterName = chapterNames[main.chapterNo].orEmpty(),
                                onOpen = { openHistory(main) },
                            )
                        }

                        if (allHistories.itemCount > 0) {
                            item(key = "recent_history_header") {
                                Text(
                                    text = stringResource(R.string.recent_history),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                                )
                            }
                        }
                    }

                    items(
                        allHistories.itemCount,
                        key = { index ->
                            val item = allHistories[index]
                            if (item != null) {
                                return@items item.id.toString()
                            } else {
                                index
                            }
                        },
                    ) { index ->
                        val history = allHistories[index]

                        if (history != null) {
                            SwipeToDeleteRow(
                                onDelete = {
                                    scope.launch { vm.deleteHistory(history.id) }
                                },
                            ) {
                                ReadHistoryCard(
                                    history = history,
                                    chapterName = chapterNames[history.chapterNo].orEmpty(),
                                    onOpen = { openHistory(history) },
                                    onSetAsMain = {
                                        scope.launch { vm.setMainHistory(history.id) }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SwipeToDeleteRow(
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val scope = rememberCoroutineScope()
    val actionWidth = 72.dp
    val actionWidthPx = with(density) { actionWidth.toPx() }
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val openOffset = if (isRtl) actionWidthPx else -actionWidthPx

    val state = remember {
        AnchoredDraggableState(initialValue = SwipeRevealValue.Closed)
    }

    LaunchedEffect(openOffset) {
        state.updateAnchors(
            DraggableAnchors {
                SwipeRevealValue.Closed at 0f
                SwipeRevealValue.Open at openOffset
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.medium),
    ) {
        Row(
            modifier = Modifier.matchParentSize(),
            horizontalArrangement = if (isRtl) Arrangement.Start else Arrangement.End,
        ) {
            Box(
                modifier = Modifier
                    .width(actionWidth)
                    .fillMaxHeight()
                    .background(colorScheme.error)
                    .clickable {
                        onDelete()
                        scope.launch { state.animateTo(SwipeRevealValue.Closed) }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.dr_icon_delete),
                    contentDescription = stringResource(R.string.strLabelRemove),
                    tint = colorScheme.onError,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset {
                    val x = state.offset
                    IntOffset(if (x.isNaN()) 0 else x.roundToInt(), 0)
                }
                .anchoredDraggable(
                    state = state,
                    orientation = Orientation.Horizontal,
                ),
        ) {
            content()
        }
    }
}

@Composable
private fun MainSessionCard(
    history: ReadHistoryEntity,
    chapterName: String,
    onOpen: () -> Unit,
) {
    val readerMode = ReaderMode.fromValue(history.readerMode)
    val title = history.titleLabel(chapterName)
    val subtitle = history.subtitleLabel(chapterName)
    val ribbonShape = remember {
        GenericShape { size, _ ->
            val notch = size.width * 0.32f
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height)
            lineTo(size.width / 2f, size.height - notch)
            lineTo(0f, size.height)
            close()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.large)
            .clickable(onClick = onOpen),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = shapes.large,
            color = colorScheme.surfaceContainerHigh,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = shapes.medium,
                    color = colorScheme.primaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(readerMode.iconRes()),
                            contentDescription = null,
                            tint = colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = colorScheme.onSurface,
                    )

                    subtitle?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Text(
                        text = stringResource(R.string.continue_reading_2),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = colorScheme.onSurfaceVariant.alpha(0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 18.dp)
                .size(width = 22.dp, height = 30.dp)
                .clip(ribbonShape)
                .background(colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.icon_star_filled),
                contentDescription = null,
                tint = colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .size(12.dp),
            )
        }
    }
}

@Composable
private fun ReadHistoryCard(
    history: ReadHistoryEntity,
    chapterName: String,
    onOpen: () -> Unit,
    onSetAsMain: () -> Unit,
) {
    val readerMode = ReaderMode.fromValue(history.readerMode)
    val title = history.titleLabel(chapterName)
    val subtitle = history.subtitleLabel(chapterName)
    val accentColor = readerMode.accentColor()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colorScheme.outlineVariant.alpha(0.45f), shapes.medium)
            .clickable(onClick = onOpen),
        color = colorScheme.surfaceContainer,
        shape = shapes.medium,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(10.dp),
                color = colorScheme.surfaceVariant.alpha(0.5f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(readerMode.iconRes()),
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = colorScheme.onSurface,
                )

                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            SimpleTooltip(text = stringResource(R.string.set_as_active)) {
                IconButton(onClick = onSetAsMain) {
                    Icon(
                        painter = painterResource(R.drawable.icon_star_outlined),
                        contentDescription = stringResource(R.string.set_as_active),
                        tint = colorScheme.onSurfaceVariant.alpha(0.75f),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ClearAllHistoryDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        isOpen = isOpen,
        onClose = onDismiss,
        title = stringResource(R.string.msgClearReadHistory),
        actions = listOf(
            AlertDialogAction(
                text = stringResource(R.string.strLabelCancel),
                onClick = onDismiss,
            ),
            AlertDialogAction(
                text = stringResource(R.string.strLabelRemoveAll),
                style = AlertDialogActionStyle.Danger,
                onClick = onConfirm,
            ),
        ),
    ) {
        Text(
            text = stringResource(R.string.strMsgReadHistoryDeleteAll),
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun ReadHistoryEntity.titleLabel(chapterName: String): String {
    if (readerMode == ReaderMode.Reading.value || readerMode == ReaderMode.Translation.value) {
        return pageNo?.let { formattedStringResource(R.string.strLabelPageNo, pageNo) } ?: "-"
    } else {
        return when (ReadType.fromValue(readType)) {
            ReadType.Chapter -> stringResource(
                R.string.strLabelSurah,
                chapterName
            )

            ReadType.Juz -> formattedStringResource(R.string.strLabelJuzNo, divisionNo)
            ReadType.Hizb -> formattedStringResource(R.string.labelHizbNo, divisionNo)
        }
    }
}

@Composable
fun ReaderMode.accentColor() = when (this) {
    ReaderMode.VerseByVerse -> colorScheme.primary
    ReaderMode.Reading -> colorScheme.secondary
    ReaderMode.Translation -> colorScheme.tertiary
}

fun ReaderMode.iconRes() = when (this) {
    ReaderMode.Reading -> R.drawable.ic_mode_mushaf
    ReaderMode.Translation -> R.drawable.ic_mode_translation
    ReaderMode.VerseByVerse -> R.drawable.ic_mode_verse
}

@Composable
fun ReadHistoryEntity.subtitleLabel(chapterName: String): String? {
    val verseLabel = if (fromVerseNo == toVerseNo) {
        formattedStringResource(R.string.strLabelVerseNo, fromVerseNo)
    } else {
        formattedStringResource(R.string.strLabelVerses, fromVerseNo, toVerseNo)

    }
    if (readerMode == ReaderMode.Reading.value || readerMode == ReaderMode.Translation.value) {
        val location = if (chapterNo > 0 && fromVerseNo > 0) {
            val chapterLabel = chapterName.ifBlank { chapterNo.toString() }
            "$chapterLabel · $verseLabel"
        } else null

        val scriptLabel = mushafCode?.getQuranScriptName()

        return listOfNotNull(location, scriptLabel)
            .filter { it.isNotBlank() }
            .joinToString(" · ")
            .ifBlank { "-" }
    } else {
        return when (ReadType.fromValue(readType)) {
            ReadType.Chapter -> verseLabel
            ReadType.Juz,
            ReadType.Hizb -> chapterName.takeIf { it.isNotBlank() }?.let {
                it + ": " + verseLabel
            }
        }
    }
}
