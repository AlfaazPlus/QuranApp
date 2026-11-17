package com.quranapp.android.screens

import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.quranapp.android.components.common.AppBar
import com.quranapp.android.components.common.MessageCard
import com.quranapp.android.components.common.dialogs.AlertDialog
import com.quranapp.android.components.common.dialogs.AlertDialogAction
import com.quranapp.android.components.common.dialogs.AlertDialogActionStyle
import com.quranapp.android.components.common.dialogs.SimpleTooltip
import com.quranapp.android.components.reader.dialogs.BookmarkViewerData
import com.quranapp.android.components.reader.dialogs.BookmarkViewerSheet
import com.quranapp.android.db.entities.user.BookmarkEntity
import com.quranapp.android.extensions.appViewModel
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.dr_icon_check
import com.quranapp.android.generated.resources.dr_icon_delete
import com.quranapp.android.generated.resources.dr_icon_edit
import com.quranapp.android.generated.resources.ic_bookmark
import com.quranapp.android.generated.resources.strLabelCancel
import com.quranapp.android.generated.resources.strLabelRemove
import com.quranapp.android.generated.resources.strLabelRemoveAll
import com.quranapp.android.generated.resources.strLabelSelectedCount
import com.quranapp.android.generated.resources.strLabelVerseNo
import com.quranapp.android.generated.resources.strLabelVerses
import com.quranapp.android.generated.resources.strMsgBookmarkDate
import com.quranapp.android.generated.resources.strMsgBookmarkDateToday
import com.quranapp.android.generated.resources.strMsgBookmarkDeleteAll
import com.quranapp.android.generated.resources.strMsgBookmarkDeleteSelected
import com.quranapp.android.generated.resources.strMsgBookmarkNoItems
import com.quranapp.android.generated.resources.strTitleBookmarkDeleteAll
import com.quranapp.android.generated.resources.strTitleBookmarkDeleteCount
import com.quranapp.android.generated.resources.strTitleBookmarkDeleteThis
import com.quranapp.android.generated.resources.strTitleBookmarks
import com.quranapp.android.locale.formattedStringResource
import com.quranapp.android.platform.PlatformServices
import com.quranapp.android.theme.alpha
import com.quranapp.android.viewmodel.BookmarksViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import java.util.Calendar
import java.util.Date

private sealed interface BookmarkDeleteTarget {
    data object All : BookmarkDeleteTarget
    data class Single(val id: Long) : BookmarkDeleteTarget
    data class Selected(val ids: Set<Long>) : BookmarkDeleteTarget
}

@Composable
fun BookmarksScreen(vm: BookmarksViewModel = appViewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allBookmarks = vm.allBookmarks.collectAsLazyPagingItems()

    val chapterNames by vm.chapterNames.collectAsStateWithLifecycle()

    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var viewerData by remember { mutableStateOf<BookmarkViewerData?>(null) }
    var deleteTarget by remember { mutableStateOf<BookmarkDeleteTarget?>(null) }

    val selecting = selectedIds.isNotEmpty()

    BackHandler(enabled = selecting) {
        selectedIds = emptySet()
    }

    BookmarkDeleteDialog(
        target = deleteTarget,
        onDismiss = { deleteTarget = null },
        onConfirm = { target ->
            scope.launch {
                val message = when (target) {
                    BookmarkDeleteTarget.All -> {
                        vm.removeAllBookmarks()
                        null
                    }

                    is BookmarkDeleteTarget.Single -> vm.removeBookmark(target.id)
                    is BookmarkDeleteTarget.Selected -> {
                        val result = vm.removeBookmarks(target.ids)
                        selectedIds = emptySet()
                        result
                    }
                }
                if (message != null) {
                    PlatformServices.messages.show(getString(message.messageRes))
                }
            }
        })

    BookmarkViewerSheet(
        data = viewerData,
        onClose = { viewerData = null },
    )

    Scaffold(
        topBar = {
            AppBar(
                title = if (selecting) {
                    formattedStringResource(Res.string.strLabelSelectedCount, selectedIds.size)
                } else {
                    stringResource(Res.string.strTitleBookmarks)
                }, actions = {
                    if (allBookmarks.itemCount > 0) {
                        val tooltip = if (selecting) {
                            stringResource(Res.string.strLabelRemove)
                        } else {
                            stringResource(Res.string.strLabelRemoveAll)
                        }

                        SimpleTooltip(text = tooltip) {
                            IconButton(
                                onClick = {
                                    deleteTarget = if (selecting) {
                                        BookmarkDeleteTarget.Selected(selectedIds)
                                    } else {
                                        BookmarkDeleteTarget.All
                                    }
                                }) {
                                Icon(
                                    painter = painterResource(Res.drawable.dr_icon_delete),
                                    contentDescription = tooltip,
                                )
                            }
                        }
                    }
                })
        }) { paddingValues ->
        when {
            allBookmarks.itemCount == 0 -> {
                MessageCard(
                    icon = Res.drawable.ic_bookmark,
                    message = stringResource(Res.string.strMsgBookmarkNoItems),
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
                    items(
                        allBookmarks.itemCount,
                        key = { index ->
                            allBookmarks[index]?.id ?: index
                        },
                    ) { index ->
                        val bookmark = allBookmarks[index] ?: return@items
                        BookmarkItemCard(
                            bookmark = bookmark,
                            chapterName = chapterNames[bookmark.chapterNo].orEmpty(),
                            selected = selectedIds.contains(bookmark.id),
                            selecting = selecting,
                            onClick = {
                                if (selecting) {
                                    selectedIds = selectedIds.toggle(bookmark.id)
                                } else {
                                    viewerData = bookmark.toViewerData()
                                }
                            },
                            onLongClick = {
                                selectedIds = selectedIds.toggle(bookmark.id)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BookmarkDeleteDialog(
    target: BookmarkDeleteTarget?,
    onDismiss: () -> Unit,
    onConfirm: (BookmarkDeleteTarget) -> Unit,
) {
    AlertDialog(
        isOpen = target != null, onClose = onDismiss, title = when (target) {
            BookmarkDeleteTarget.All -> stringResource(Res.string.strTitleBookmarkDeleteAll)
            is BookmarkDeleteTarget.Selected -> stringResource(
                Res.string.strTitleBookmarkDeleteCount,
                target.ids.size,
            )

            is BookmarkDeleteTarget.Single -> stringResource(Res.string.strTitleBookmarkDeleteThis)
            null -> ""
        }, actions = listOf(
            AlertDialogAction(
                text = stringResource(Res.string.strLabelCancel),
                onClick = onDismiss,
            ), AlertDialogAction(
                text = when (target) {
                    BookmarkDeleteTarget.All -> stringResource(Res.string.strLabelRemoveAll)
                    is BookmarkDeleteTarget.Selected, is BookmarkDeleteTarget.Single -> stringResource(
                        Res.string.strLabelRemove
                    )

                    null -> ""
                },
                style = AlertDialogActionStyle.Danger,
                onClick = {
                    target?.let { onConfirm(it) }
                },
            )
        )
    ) {
        val message = when (target) {
            BookmarkDeleteTarget.All -> stringResource(Res.string.strMsgBookmarkDeleteAll)
            is BookmarkDeleteTarget.Selected -> stringResource(Res.string.strMsgBookmarkDeleteSelected)
            else -> null
        }

        if (message != null) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BookmarkItemCard(
    bookmark: BookmarkEntity,
    onClick: () -> Unit,
    chapterName: String,
    selected: Boolean,
    selecting: Boolean,
    onLongClick: () -> Unit,
) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) colorScheme.primary else colorScheme.outline.alpha(0.3f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (selecting) colorScheme.primary
                            else colorScheme.background
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selecting) {
                        Icon(
                            painter = painterResource(Res.drawable.dr_icon_check),
                            contentDescription = null,
                            tint = colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                    } else {
                        Text(
                            bookmark.chapterNo.toString(),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Light
                            ),
                        )
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        if (bookmark.fromVerseNo == bookmark.toVerseNo) chapterName + ": " + stringResource(
                            Res.string.strLabelVerseNo, bookmark.fromVerseNo
                        )
                        else chapterName + ": " + stringResource(
                            Res.string.strLabelVerses, bookmark.fromVerseNo, bookmark.toVerseNo
                        ),
                        style = MaterialTheme.typography.labelLarge,
                    )

                    Text(
                        formatBookmarkDate(java.util.Date(bookmark.dateTime)),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Light
                        ),
                    )
                }
            }

            if (!bookmark.note.isNullOrBlank()) {
                HorizontalDivider()

                Text(
                    text = buildAnnotatedString {
                        appendInlineContent("user_note", "[icon]")
                        append(" ")
                        append(bookmark.note)
                    },
                    inlineContent = mapOf(
                        "user_note" to InlineTextContent(
                            Placeholder(
                                width = 16.sp,
                                height = 16.sp,
                                placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                            )
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.dr_icon_edit),
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.fillMaxSize()
                            )
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun formatBookmarkDate(date: Date): String {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply { time = date }

    val sameYear = now.get(Calendar.YEAR) == target.get(Calendar.YEAR)
    val sameDay = sameYear && now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)

    val timeText = DateFormat.format("hh:mm a", date).toString()

    return when {
        sameDay -> stringResource(Res.string.strMsgBookmarkDateToday, timeText)
        sameYear -> stringResource(
            Res.string.strMsgBookmarkDate,
            DateFormat.format("dd MMM", date).toString(),
            timeText,
        )

        else -> stringResource(
            Res.string.strMsgBookmarkDate,
            DateFormat.format("dd MMM yyyy", date).toString(),
            timeText,
        )
    }
}

private fun BookmarkEntity.toViewerData(startInEditMode: Boolean = false): BookmarkViewerData {
    return BookmarkViewerData(
        chapterNo = chapterNo,
        fromVerse = fromVerseNo,
        toVerse = toVerseNo,
        startInEditMode = startInEditMode,
    )
}

private fun Set<Long>.toggle(id: Long): Set<Long> {
    return toMutableSet().apply {
        if (!add(id)) {
            remove(id)
        }
    }
}
