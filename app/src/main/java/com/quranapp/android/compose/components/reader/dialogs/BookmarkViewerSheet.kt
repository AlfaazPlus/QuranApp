package com.quranapp.android.compose.components.reader.dialogs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quranapp.android.R
import com.quranapp.android.compose.components.dialogs.AlertDialog
import com.quranapp.android.compose.components.dialogs.AlertDialogAction
import com.quranapp.android.compose.components.dialogs.AlertDialogActionStyle
import com.quranapp.android.compose.components.reader.ReaderLayoutItem
import com.quranapp.android.compose.components.reader.ReaderPreparedData
import com.quranapp.android.compose.components.reader.ReaderProvider
import com.quranapp.android.compose.components.reader.TextStyleProvider
import com.quranapp.android.compose.components.reader.VerseActionBarMode
import com.quranapp.android.compose.components.reader.VerseView
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.compose.utils.formattedStringResource
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.utils.extensions.orMinusOne
import com.quranapp.android.utils.reader.ComposeUiConfig
import com.quranapp.android.utils.reader.LocalVerseActions
import com.quranapp.android.utils.reader.ReaderItemsBuilder
import com.quranapp.android.utils.reader.TextBuilderParams
import com.quranapp.android.utils.reader.factory.ReaderFactory
import com.quranapp.android.viewModels.ReaderProviderViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BookmarkViewerData(
    val chapterNo: Int,
    val fromVerse: Int,
    val toVerse: Int,
    val showOpenInReaderButton: Boolean = true,
    val startInEditMode: Boolean = false,
)

/** Presents the saved note and the full verse view in one navigable sheet. */
@Composable
fun BookmarkViewerSheet(
    data: BookmarkViewerData?,
    onClose: () -> Unit,
) {
    if (data == null) return

    // VerseView uses reader composition locals for its actions, player and text rendering.
    ReaderProvider {
        BookmarkViewerSheetContent(data, onClose)
    }
}

private enum class BookmarkTab { Note, Verses }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookmarkViewerSheetContent(
    data: BookmarkViewerData,
    onClose: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val viewModel = viewModel<ReaderProviderViewModel>()

    val textMeasurer = rememberTextMeasurer()
    val colors by rememberUpdatedState(MaterialTheme.colorScheme)
    val type by rememberUpdatedState(MaterialTheme.typography)
    val density = LocalDensity.current
    val verseActions = LocalVerseActions.current

    val repo = viewModel.userRepository
    val bookmark = repo.getBookmarkFlow(data.chapterNo, data.fromVerse, data.toVerse)
        .collectAsStateWithLifecycle(null).value ?: return

    val chapterNo = bookmark.chapterNo.orMinusOne()
    val fromVerseNo = bookmark.fromVerseNo.orMinusOne()
    val toVerseNo = bookmark.toVerseNo.orMinusOne()
    var note by remember(bookmark.id) { mutableStateOf(bookmark.note.orEmpty()) }
    var editing by remember(data) { mutableStateOf(data.startInEditMode) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var chapterName by remember { mutableStateOf("") }
    var prepared by remember(bookmark.id) { mutableStateOf<ReaderPreparedData?>(null) }
    var isLoading by remember(bookmark.id) { mutableStateOf(true) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var selectedTab by remember(data) { mutableStateOf(BookmarkTab.Note) }

    LaunchedEffect(chapterNo) {
        chapterName = withContext(Dispatchers.IO) {
            DatabaseProvider.getQuranRepository(context).getChapterName(chapterNo)
        }
    }

    LaunchedEffect(chapterNo, fromVerseNo, toVerseNo, verseActions, colors, type, density) {
        isLoading = true
        prepared = withContext(Dispatchers.IO) {
            val params = TextBuilderParams(
                uiConfig = ComposeUiConfig(
                    context = context,
                    textMeasurer = textMeasurer,
                    density = density,
                    colors = colors,
                    type = type,
                ),
                fontResolver = viewModel.fontResolver,
                verseActions = verseActions,
                arabicEnabled = ReaderPreferences.getArabicTextEnabled(),
                script = ReaderPreferences.getQuranScript(),
                arabicSizeMultiplier = ReaderPreferences.getArabicTextSizeMultiplier(),
                translationSizeMultiplier = ReaderPreferences.getTranslationTextSizeMultiplier(),
                slugs = ReaderPreferences.getTranslations(),
            )
            ReaderItemsBuilder.buildQuickReferenceItems(
                context, params, chapterNo, (fromVerseNo..toVerseNo).toList(),
            )
        }

        isLoading = false
    }

    LaunchedEffect(editing) {
        if (editing) {
            focusRequester.requestFocus()
            keyboard?.show()
        } else {
            keyboard?.hide()
        }
    }

    fun saveBookmark() {
        coroutineScope.launch {
            repo.updateBookmark(
                chapterNo, fromVerseNo, toVerseNo, note.trim().takeIf { it.isNotEmpty() },
            )
        }
    }

    fun dismissAfterPersist() {
        saveBookmark()
        onClose()
    }

    val verses = prepared?.items.orEmpty().filterIsInstance<ReaderLayoutItem.VerseUI>()

    ModalBottomSheet(
        onDismissRequest = ::dismissAfterPersist,
        sheetState = sheetState,
        sheetGesturesEnabled = false,
        scrimColor = colorScheme.scrim.alpha(0.5f),
        containerColor = colorScheme.surface,
        contentColor = colorScheme.onSurface,
        dragHandle = null,
        contentWindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom) },
    ) {
        AlertDialog(
            isOpen = showDeleteConfirm,
            onClose = { showDeleteConfirm = false },
            title = stringResource(R.string.strTitleBookmarkDeleteThis),
            actions = listOf(
                AlertDialogAction(stringResource(R.string.strLabelCancel)) {
                    showDeleteConfirm = false
                },
                AlertDialogAction(
                    text = stringResource(R.string.strLabelRemove),
                    style = AlertDialogActionStyle.Danger,
                    onClick = {
                        coroutineScope.launch {
                            repo.removeFromBookmark(chapterNo, fromVerseNo, toVerseNo)
                            onClose()
                        }
                    },
                ),
            ),
        )

        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {

            BookmarkHeader(
                chapterName = chapterName,
                fromVerseNo = fromVerseNo,
                toVerseNo = toVerseNo,
                onDelete = { showDeleteConfirm = true },
            )

            SecondaryTabRow(selectedTabIndex = selectedTab.ordinal) {
                Tab(
                    selected = selectedTab == BookmarkTab.Note,
                    onClick = { selectedTab = BookmarkTab.Note },
                    text = { Text(stringResource(R.string.strTitleNote)) },
                )
                Tab(
                    selected = selectedTab == BookmarkTab.Verses,
                    onClick = { selectedTab = BookmarkTab.Verses },
                    text = { Text(stringResource(R.string.verses)) },
                )
            }

            HorizontalDivider()

            when (selectedTab) {
                BookmarkTab.Note -> Box(Modifier.weight(1f)) {
                    BookmarkNoteTab(
                        note = note,
                        editing = editing,
                        focusRequester = focusRequester,
                        onEdit = { editing = true },
                        onNoteChange = { note = it },
                        onDone = {
                            saveBookmark()
                            editing = false
                        },
                    )
                }

                BookmarkTab.Verses -> Box(Modifier.weight(1f)) {
                    BookmarkVersesTab(
                        isLoading = isLoading,
                        prepared = prepared,
                        verses = verses,
                    )
                }
            }

            if (data.showOpenInReaderButton && !editing) {
                Button(
                    onClick = {
                        ReaderFactory.startVerseRange(context, chapterNo, fromVerseNo, toVerseNo)
                        onClose()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) { Text(stringResource(R.string.strLabelOpenInReader)) }
            }
        }
    }
}

@Composable
private fun BookmarkHeader(
    chapterName: String,
    fromVerseNo: Int,
    toVerseNo: Int,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.strLabelSurah, chapterName),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface,
            )

            Text(
                text = if (fromVerseNo == toVerseNo) {
                    stringResource(R.string.strLabelVerseNoWithColon, fromVerseNo)
                } else {
                    formattedStringResource(
                        R.string.strLabelVersesWithColon,
                        fromVerseNo,
                        toVerseNo
                    )
                },
                style = MaterialTheme.typography.bodyLarge,
                color = colorScheme.onSurfaceVariant,
            )
        }

        IconButton(onClick = onDelete) {
            Icon(
                painterResource(R.drawable.dr_icon_delete),
                stringResource(R.string.strLabelRemove),
                tint = colorScheme.error,
            )
        }
    }
}

@Composable
private fun BookmarkNoteTab(
    note: String,
    editing: Boolean,
    focusRequester: FocusRequester,
    onEdit: () -> Unit,
    onNoteChange: (String) -> Unit,
    onDone: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.strTitleNote),
                style = MaterialTheme.typography.labelLarge,
                color = colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = if (editing) onDone else onEdit) {
                Text(stringResource(if (editing) R.string.strLabelDone else R.string.strLabelEdit))
            }
        }

        if (editing) {
            OutlinedTextField(
                value = note,
                onValueChange = onNoteChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 150.dp)
                    .focusRequester(focusRequester),
                placeholder = { Text(stringResource(R.string.strHintBookmarkViewerNote)) },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { onDone() }),
            )
        } else {
            val hasNote = note.isNotBlank()
            Text(
                text = if (hasNote) note else stringResource(R.string.noNoteProvided),
                style = MaterialTheme.typography.bodyLarge,
                color = if (hasNote) colorScheme.onSurface else colorScheme.onSurface.alpha(0.6f),
                fontStyle = if (hasNote) FontStyle.Normal else FontStyle.Italic,
            )
        }
    }
}

@Composable
private fun BookmarkVersesTab(
    isLoading: Boolean,
    prepared: ReaderPreparedData?,
    verses: List<ReaderLayoutItem.VerseUI>,
) {
    if (isLoading) {
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(), contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    TextStyleProvider(prepared?.textStyles ?: emptyMap()) {
        LazyColumn(modifier = Modifier.fillMaxHeight()) {
            itemsIndexed(verses, key = { _, verseUi -> verseUi.key }) { index, verseUi ->
                VerseView(
                    verseUi = verseUi,
                    isBookmarked = false, /* not needed because the button is not shown */
                    showDivider = index < verses.lastIndex,
                    actionBarMode = VerseActionBarMode.Minimal,
                )
            }
        }
    }
}
