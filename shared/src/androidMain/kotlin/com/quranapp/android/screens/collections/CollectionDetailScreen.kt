package com.quranapp.android.screens.collections

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.quranapp.android.components.collections.CreateUpdateCollectionSheet
import com.quranapp.android.components.collections.EditCollectionItemNoteSheet
import com.quranapp.android.components.collections.collectionColor
import com.quranapp.android.components.collections.collectionIcon
import com.quranapp.android.components.common.AppBar
import com.quranapp.android.components.common.BottomSheetMenu
import com.quranapp.android.components.common.BottomSheetMenuItem
import com.quranapp.android.components.common.Loader
import com.quranapp.android.components.common.dialogs.AlertDialog
import com.quranapp.android.components.common.dialogs.AlertDialogAction
import com.quranapp.android.components.common.dialogs.AlertDialogActionStyle
import com.quranapp.android.components.player.MiniPlayerVisibility
import com.quranapp.android.components.player.RecitationPlayerSheet
import com.quranapp.android.components.player.rememberMiniPlayerVisibilityState
import com.quranapp.android.components.reader.LocalReaderViewModel
import com.quranapp.android.components.reader.LocalRecitation
import com.quranapp.android.components.reader.PlayControlButton
import com.quranapp.android.components.reader.QuranTextWbw
import com.quranapp.android.components.reader.ReaderProvider
import com.quranapp.android.components.reader.TextStyleProvider
import com.quranapp.android.components.reader.TranslationText
import com.quranapp.android.components.reader.VerseActionIconButton
import com.quranapp.android.components.reader.VerseSerial
import com.quranapp.android.db.entities.user.CollectionEntity
import com.quranapp.android.db.entities.user.CollectionItemEntity
import com.quranapp.android.db.entities.user.CollectionWithCount
import com.quranapp.android.db.relations.VerseWithDetails
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.delete_collection
import com.quranapp.android.generated.resources.dr_icon_delete
import com.quranapp.android.generated.resources.dr_icon_edit
import com.quranapp.android.generated.resources.dr_icon_menu
import com.quranapp.android.generated.resources.dr_icon_open
import com.quranapp.android.generated.resources.ic_book_open_text
import com.quranapp.android.generated.resources.moreOptions
import com.quranapp.android.generated.resources.msg_collection_deleted
import com.quranapp.android.generated.resources.msg_delete_collection
import com.quranapp.android.generated.resources.msg_remove_from_collection
import com.quranapp.android.generated.resources.no_collection_items
import com.quranapp.android.generated.resources.remove_from_collection
import com.quranapp.android.generated.resources.strLabelCancel
import com.quranapp.android.generated.resources.strLabelDelete
import com.quranapp.android.generated.resources.strLabelEdit
import com.quranapp.android.generated.resources.strLabelRead
import com.quranapp.android.generated.resources.strLabelRemove
import com.quranapp.android.generated.resources.strTitleAddNote
import com.quranapp.android.generated.resources.strTitleTafsir
import com.quranapp.android.generated.resources.strTitleVerseOptions
import com.quranapp.android.generated.resources.verse_count
import com.quranapp.android.models.ChapterVersePair
import com.quranapp.android.platform.PlatformServices
import com.quranapp.android.preferences.ReaderPreferences
import com.quranapp.android.reader.ReaderLayoutItem
import com.quranapp.android.reader.ReaderPreparedData
import com.quranapp.android.theme.alpha
import com.quranapp.android.utils.reader.ComposeUiConfig
import com.quranapp.android.utils.reader.LocalVerseActions
import com.quranapp.android.utils.reader.ReaderItemsBuilder
import com.quranapp.android.utils.reader.TextBuilderParams
import com.quranapp.android.utils.reader.factory.ReaderFactory
import com.quranapp.android.viewmodel.CollectionsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private data class CollectionScrollAnchor(
    val itemId: Long,
    val offset: Int,
)

@Composable
fun CollectionDetailScreen(
    vm: CollectionsViewModel,
    collectionId: Long,
) {
    ReaderProvider {
        CollectionDetailContent(vm, collectionId)
    }
}

@Composable
private fun CollectionDetailContent(
    vm: CollectionsViewModel,
    collectionId: Long,
) {
    val onBackPressedDispatcher =
        LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    val detail by remember(collectionId) { vm.observeCollection(collectionId) }
        .collectAsStateWithLifecycle(null)

    val collection = detail

    val items = remember(collectionId) { vm.collectionItems(collectionId) }
        .collectAsLazyPagingItems()
    val scope = rememberCoroutineScope()

    val listState = rememberLazyListState()
    var refreshAnchor by remember(collectionId) {
        mutableStateOf<CollectionScrollAnchor?>(null)
    }

    LaunchedEffect(items.loadState.refresh) {
        when (items.loadState.refresh) {
            is LoadState.Loading -> {
                if (refreshAnchor == null && listState.firstVisibleItemIndex > 0) {
                    items.peek(listState.firstVisibleItemIndex - 1)?.let { item ->
                        refreshAnchor = CollectionScrollAnchor(
                            itemId = item.id,
                            offset = listState.firstVisibleItemScrollOffset,
                        )
                    }
                }
            }

            is LoadState.NotLoading -> {
                refreshAnchor?.let { anchor ->
                    val itemIndex = items.itemSnapshotList.items.indexOfFirst {
                        it.id == anchor.itemId
                    }
                    if (itemIndex >= 0) {
                        listState.scrollToItem(itemIndex + 1, anchor.offset)
                    }
                }

                refreshAnchor = null
            }

            is LoadState.Error -> refreshAnchor = null
        }
    }

    val context = LocalContext.current
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val readerVm = LocalReaderViewModel.current
    val verseActions = LocalVerseActions.current
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography

    val arabicEnabled = ReaderPreferences.observeArabicTextEnabled()
    val script = ReaderPreferences.observeQuranScript()
    val arabicSizeMultiplier = ReaderPreferences.observeArabicTextSizeMultiplier()
    val translationSizeMultiplier = ReaderPreferences.observeTranlationTextSizeMultiplier()
    val translations = ReaderPreferences.observeTranslations()

    val textParams = remember(
        context, density, textMeasurer, verseActions, colors, type,
        arabicEnabled, script, arabicSizeMultiplier, translationSizeMultiplier, translations,
    ) {
        TextBuilderParams(
            uiConfig = ComposeUiConfig(
                context = context,
                textMeasurer = textMeasurer,
                density = density,
                colors = colors,
                type = type,
            ),
            fontResolver = readerVm.fontResolver,
            verseActions = verseActions,
            arabicEnabled = arabicEnabled,
            script = script,
            arabicSizeMultiplier = arabicSizeMultiplier,
            translationSizeMultiplier = translationSizeMultiplier,
            slugs = translations,
        )
    }

    // Paging adds rows while the user scrolls. Prepare only those new rows, in batches per
    // surah, instead of having every composed row start its own full reader-data build.
    val loadedItems = items.itemSnapshotList.items
    var preparedItems by remember(textParams) {
        mutableStateOf<Map<Long, ReaderPreparedData>>(emptyMap())
    }
    var settledItemIds by remember(textParams) {
        mutableStateOf<Set<Long>>(emptySet())
    }

    LaunchedEffect(loadedItems, textParams) {
        val missingItems =
            loadedItems.filter { it.id !in preparedItems && it.id !in settledItemIds }
        if (missingItems.isEmpty()) return@LaunchedEffect

        val newlyPrepared = try {
            withContext(Dispatchers.IO) {
                ReaderItemsBuilder.buildQuickReferenceItems(
                    params = textParams,
                    requests = missingItems.map {
                        ReaderItemsBuilder.QuickReferenceRequest(
                            key = it.id,
                            chapterNo = it.chapterNo,
                            verseNo = it.verseNo,
                        )
                    },
                )
            }
        } catch (_: Exception) {
            emptyMap()
        }

        preparedItems = preparedItems + newlyPrepared
        settledItemIds = settledItemIds + missingItems.map { it.id }
    }

    val showCollapsedTitle by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 70
        }
    }

    var collectionToUpdate by remember { mutableStateOf<CollectionEntity?>(null) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showDeleteAlert by remember { mutableStateOf(false) }
    val recState = LocalRecitation.current
    val playerVisibilityState = rememberMiniPlayerVisibilityState(
        visibility = MiniPlayerVisibility.HIDDEN_BY_DEFAULT,
    )

    DisposableEffect(recState.controller) {
        onDispose {
            recState.controller.clearMode()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = colorScheme.background,
            topBar = {
                AppBar(
                    title = if (showCollapsedTitle) collection?.collection?.name else null,
                    shadowElevation = 0.dp,
                    actions = {
                        if (collection == null) return@AppBar

                        IconButton(
                            modifier = Modifier.size(40.dp),
                            onClick = {
                                showMoreMenu = !showMoreMenu
                            }
                        ) {
                            Icon(
                                painter = painterResource(
                                    Res.drawable.dr_icon_menu
                                ),
                                contentDescription = stringResource(Res.string.moreOptions),
                            )
                        }

                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false },
                            border = BorderStroke(1.dp, colorScheme.outline.alpha(0.35f)),
                            shape = shapes.medium,
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.strLabelEdit)) },
                                leadingIcon = {
                                    Icon(
                                        painterResource(Res.drawable.dr_icon_edit),
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    collectionToUpdate = collection.collection
                                    showMoreMenu = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.delete_collection)) },
                                leadingIcon = {
                                    Icon(
                                        painterResource(Res.drawable.dr_icon_delete),
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showDeleteAlert = true
                                    showMoreMenu = false
                                }
                            )
                        }
                    }
                )
            },
        ) { padding ->
            val detail = collection
            if (detail == null) {
                Box(Modifier.padding(padding)) { Loader(fill = true) }
                return@Scaffold
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 128.dp)
            ) {
                item(key = "hero-${detail.collection.id}") { Hero(detail) }

                if (items.loadState.refresh is LoadState.Loading && items.itemCount == 0) {
                    item(key = "initial-loader") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (items.itemCount == 0) {
                    item(key = "empty") { CollectionItemsEmptyState() }
                } else {
                    items(
                        count = items.itemCount,
                        key = { index -> items[index]?.id ?: "placeholder-$index" },
                    ) { index ->
                        val item = items[index] ?: return@items
                        CollectionVerseItem(
                            vm = vm,
                            item = item,
                            prepared = preparedItems[item.id],
                            onPlayVerse = { verse ->
                                recState.controller.playControlRange(
                                    verse = verse,
                                    fromVerse = verse.verseNo,
                                    toVerse = verse.verseNo,
                                )
                            },
                        )
                    }
                }
            }
        }

        RecitationPlayerSheet(
            playerVisibilityState = playerVisibilityState,
        )
    }

    CreateUpdateCollectionSheet(
        collectionToUpdate != null,
        onClose = { collectionToUpdate = null },
        collectionToUpdate = collectionToUpdate
    )

    AlertDialog(
        isOpen = showDeleteAlert,
        onClose = { showDeleteAlert = false },
        title = stringResource(Res.string.delete_collection),
        actions = listOf(
            AlertDialogAction(text = stringResource(Res.string.strLabelCancel)),
            AlertDialogAction(
                text = stringResource(Res.string.strLabelDelete),
                style = AlertDialogActionStyle.Danger,
                onClick = {
                    scope.launch {
                        vm.deleteCollection(collectionId)

                        PlatformServices.messages.show(getString(Res.string.msg_collection_deleted))

                        onBackPressedDispatcher?.onBackPressed()
                    }
                },
            ),
        ),
    ) {
        Text(
            text = stringResource(
                Res.string.msg_delete_collection,
            ),
        )
    }
}

@Composable
private fun CollectionItemsEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(Res.string.no_collection_items),
            style = typography.bodyMedium,
            color = colorScheme.onSurfaceVariant.alpha(0.65f),
        )
    }
}

@Composable
private fun Hero(detail: CollectionWithCount) {
    val collection = detail.collection
    val color = collectionColor(collection.color)

    Surface(
        color = colorScheme.surfaceContainer,
        modifier = Modifier
            .padding(bottom = 8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                        .background(color),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(collectionIcon(collection.icon_key)),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }

                Column() {
                    Text(
                        text = collection.name,
                        style = typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )

                    Text(
                        text = pluralStringResource(
                            Res.plurals.verse_count,
                            detail.verseCount,
                            detail.verseCount
                        ),
                        style = typography.bodyMedium,
                    )
                }
            }

            val description = collection.description
            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    style = typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }

            Text(
                text = updatedLabel(collection.updatedAt),
                style = typography.bodySmall,
                color = colorScheme.onSurfaceVariant.alpha(0.6f),
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@Composable
private fun CollectionVerseItem(
    vm: CollectionsViewModel,
    item: CollectionItemEntity,
    prepared: ReaderPreparedData?,
    onPlayVerse: (ChapterVersePair) -> Unit,
) {
    val data = prepared ?: return

    val verseUi = data.items.firstOrNull { it is ReaderLayoutItem.VerseUI }
            as? ReaderLayoutItem.VerseUI ?: return

    TextStyleProvider(data.textStyles) {
        Surface(
            shape = shapes.small,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .border(1.dp, colorScheme.outlineVariant.alpha(0.5f), shapes.medium),
            color = colorScheme.surfaceContainer,
        ) {
            Column {
                CollectionVerseView(
                    verseUi = verseUi,
                    onRemove = {
                        vm.removeItem(item.id)
                    },
                    onPlayVerse = onPlayVerse,
                )
                NoteContainer(item)
            }
        }
    }
}

@Composable
private fun CollectionVerseView(
    verseUi: ReaderLayoutItem.VerseUI,
    onRemove: () -> Unit,
    onPlayVerse: (ChapterVersePair) -> Unit,
) {
    val verse = verseUi.verse

    val recState = LocalRecitation.current
    val isVersePlaying by remember(verse, recState) {
        derivedStateOf {
            recState.isAnyPlayingState.value && recState.playingVerseState.value.doesEqual(verse)
        }
    }

    Box {
        Column(
            modifier = Modifier
                .background(
                    if (isVersePlaying) colorScheme.primary.alpha(0.2f)
                    else Color.Transparent
                )
                .padding(horizontal = 12.dp, vertical = 16.dp)
        ) {
            CollectionVerseActionBar(verse, isVersePlaying, onRemove, onPlayVerse)

            QuranTextWbw(
                verseUi = verseUi,
                onWordClick = null
            )

            TranslationText(verseUi = verseUi)
        }
    }
}

@Composable
private fun CollectionVerseActionBar(
    verse: VerseWithDetails,
    isVersePlaying: Boolean,
    onRemove: () -> Unit,
    onPlayVerse: (ChapterVersePair) -> Unit,
) {
    val context = LocalContext.current
    val recitationState = LocalRecitation.current
    val controller = recitationState.controller

    val iconTint = colorScheme.onBackground.alpha(0.7f)

    var moreMenuOpen by remember { mutableStateOf(false) }
    var showDeleteAlert by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VerseActionIconButton(
                painter = painterResource(Res.drawable.dr_icon_menu),
                contentDescription = stringResource(Res.string.strTitleVerseOptions),
                tint = iconTint,
                iconModifier = Modifier.rotate(90f),
            ) {
                moreMenuOpen = true
            }

            PlayControlButton(
                controller = controller,
                isVersePlaying = isVersePlaying,
                iconTint = iconTint,
                verse = verse,
                onPlayClick = { onPlayVerse(ChapterVersePair(verse)) },
            )

            VerseActionIconButton(
                painter = painterResource(Res.drawable.ic_book_open_text),
                contentDescription = stringResource(Res.string.strTitleTafsir),
                tint = iconTint,
            ) { ReaderFactory.startTafsir(context, verse.chapterNo, verse.verseNo) }

            VerseActionIconButton(
                painter = painterResource(Res.drawable.dr_icon_open),
                contentDescription = stringResource(Res.string.strLabelRead),
                tint = iconTint,
            ) { ReaderFactory.startVerse(context, verse.chapterNo, verse.verseNo) }

        }
        Spacer(modifier = Modifier.width(10.dp))
        VerseSerial(
            verse = verse,
        )
    }

    BottomSheetMenu(
        isOpen = moreMenuOpen,
        onDismiss = { moreMenuOpen = false },
        title = stringResource(Res.string.strTitleVerseOptions),
    ) {
        BottomSheetMenuItem(
            text = stringResource(Res.string.remove_from_collection),
            icon = Res.drawable.dr_icon_delete,
            onClick = {
                showDeleteAlert = true
                moreMenuOpen = false
            },
        )
    }

    AlertDialog(
        isOpen = showDeleteAlert,
        onClose = { showDeleteAlert = false },
        title = stringResource(Res.string.remove_from_collection),
        actions = listOf(
            AlertDialogAction(text = stringResource(Res.string.strLabelCancel)),
            AlertDialogAction(
                text = stringResource(Res.string.strLabelRemove),
                style = AlertDialogActionStyle.Danger,
                onClick = onRemove,
            ),
        ),
    ) {
        Text(
            text = stringResource(
                Res.string.msg_remove_from_collection,
            ),
        )
    }
}

@Composable
private fun NoteContainer(item: CollectionItemEntity) {
    var itemToUpdate by remember { mutableStateOf<CollectionItemEntity?>(null) }

    EditCollectionItemNoteSheet(
        itemToUpdate = itemToUpdate,
    ) {
        itemToUpdate = null
    }

    if (!item.note.isNullOrBlank()) {
        Surface(
            color = colorScheme.surfaceContainer,
            tonalElevation = 2.dp,
        ) {
            Column(
                modifier = Modifier.padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 8.dp
                )
            ) {
                Text(item.note.orEmpty())

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = {
                            itemToUpdate = item
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = colorScheme.onSurface.alpha(0.75f)
                        )
                    ) {
                        Text(stringResource(Res.string.strLabelEdit))
                    }
                }
            }
        }
    } else {
        HorizontalDivider(color = colorScheme.outline.alpha(0.3f))
        TextButton(
            onClick = {
                itemToUpdate = item
            },
            colors = ButtonDefaults.textButtonColors(
                contentColor = colorScheme.onSurface.alpha(0.75f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            Text(
                stringResource(Res.string.strTitleAddNote),
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
                style = typography.bodyMedium,
            )
        }
    }
}
