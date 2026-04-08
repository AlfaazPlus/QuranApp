package com.quranapp.android.compose.components.reader.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranapp.android.R
import com.quranapp.android.db.relations.VerseWithDetails
import com.quranapp.android.compose.components.dialogs.AlertDialog
import com.quranapp.android.compose.components.dialogs.AlertDialogAction
import com.quranapp.android.compose.components.dialogs.AlertDialogActionStyle
import com.quranapp.android.compose.components.reader.LocalRecitationState
import com.quranapp.android.compose.components.reader.LocalRecitationStateData
import com.quranapp.android.compose.components.reader.ReaderLayoutItem
import com.quranapp.android.compose.components.reader.VerseView
import com.quranapp.android.compose.extensions.bottomBorder
import com.quranapp.android.compose.screens.reader.BookmarkViewerData
import com.quranapp.android.compose.screens.reader.BookmarkViewerSheet
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.db.UserRepository
import com.quranapp.android.utils.mediaplayer.RecitationController
import com.quranapp.android.utils.reader.FontResolver
import com.quranapp.android.utils.reader.LocalVerseActions
import com.quranapp.android.utils.reader.ReaderItemsBuilder
import com.quranapp.android.utils.reader.TextBuilderParams
import com.quranapp.android.utils.reader.VerseActions
import com.quranapp.android.utils.reader.factory.ReaderFactory
import com.quranapp.android.utils.univ.RegexPattern
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class QuickReferenceData(
    val slugs: Set<String>,
    val chapterNo: Int,
    val verses: String,
)

private sealed class ParsedVerses {
    data class Range(val range: IntRange) : ParsedVerses()
    data class Discrete(val verseNos: List<Int>) : ParsedVerses()
    data object ChapterOnly : ParsedVerses()
}

private fun parseVerses(versesStr: String): ParsedVerses {
    if (versesStr.isBlank()) return ParsedVerses.ChapterOnly

    val parts = versesStr.split(",")
    if (parts.size > 1) {
        val ints = parts.mapNotNull { it.trim().toIntOrNull() }.sorted()
        return ParsedVerses.Discrete(ints)
    }

    val matcher = RegexPattern.VERSE_RANGE_PATTERN.matcher(versesStr)
    if (matcher.find() && matcher.groupCount() >= 2) {
        val from = matcher.group(1)!!.toInt()
        val to = matcher.group(2)!!.toInt()
        return ParsedVerses.Range(from..to)
    }

    val single = versesStr.trim().toIntOrNull()
    return if (single != null) ParsedVerses.Range(single..single)
    else ParsedVerses.ChapterOnly
}

private fun parsedVersesToList(parsed: ParsedVerses): List<Int> = when (parsed) {
    is ParsedVerses.Range -> parsed.range.toList()
    is ParsedVerses.Discrete -> parsed.verseNos
    is ParsedVerses.ChapterOnly -> emptyList()
}

private fun parsedVersesToIntRange(parsed: ParsedVerses): IntRange? = when (parsed) {
    is ParsedVerses.Range -> parsed.range
    is ParsedVerses.Discrete -> {
        if (parsed.verseNos.isNotEmpty()) parsed.verseNos.min()..parsed.verseNos.max()
        else null
    }

    is ParsedVerses.ChapterOnly -> null
}

private fun formatTitle(chapterNo: Int, parsed: ParsedVerses): String {
    val prefix = "Quran $chapterNo:"
    return when (parsed) {
        is ParsedVerses.Range -> {
            if (parsed.range.first == parsed.range.last) "$prefix${parsed.range.first}"
            else "$prefix${parsed.range.first}-${parsed.range.last}"
        }

        is ParsedVerses.Discrete -> {
            prefix + parsed.verseNos.joinToString(", ")
        }

        is ParsedVerses.ChapterOnly -> "Quran $chapterNo"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickReference(
    data: QuickReferenceData?,
    onOpenInReader: (chapterNo: Int, range: IntRange) -> Unit,
    onClose: () -> Unit,
) {
    if (data == null) return

    val parsed = remember(data) { parseVerses(data.verses) }

    if (parsed is ParsedVerses.ChapterOnly) {
        ChapterOnlyDialog(
            chapterNo = data.chapterNo,
            slugs = data.slugs,
            onOpen = {
                onOpenInReader(data.chapterNo, 1..Int.MAX_VALUE)
                onClose()
            },
            onClose = onClose,
        )
        return
    }

    val context = LocalContext.current
    val controller = RecitationController.remember() ?: return
    val recitationState by controller.state.collectAsStateWithLifecycle()
    val isPlaying by controller.isPlayingState.collectAsStateWithLifecycle()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    var bookmarkViewerData by remember { mutableStateOf<BookmarkViewerData?>(null) }
    var footnotePresenterData by remember { mutableStateOf<FootnotePresenterData?>(null) }
    var verseOptionsVerse by remember { mutableStateOf<VerseWithDetails?>(null) }
    var quickReferenceStack by remember { mutableStateOf<QuickReferenceData?>(null) }

    CompositionLocalProvider(
        LocalVerseActions provides VerseActions(
            onReferenceClick = { slugs, chapterNo, verses ->
                quickReferenceStack = QuickReferenceData(slugs, chapterNo, verses)
            },
            onVerseOption = { verse -> verseOptionsVerse = verse },
            onFootnoteClick = { verse, footnote ->
                footnotePresenterData = FootnotePresenterData(
                    verse,
                    footnote
                )
            },
            onBookmarkRequest = { chapterNo, verseRange ->
                coroutineScope.launch {
                    val repo = DatabaseProvider.getUserRepository(context)

                    if (repo.isBookmarked(
                            chapterNo,
                            verseRange,
                        )
                    ) {
                        bookmarkViewerData = BookmarkViewerData(
                            chapterNo = chapterNo,
                            fromVerse = verseRange.first,
                            toVerse = verseRange.last,
                            showOpenInReaderButton = false,
                        )
                    } else {
                        repo.addToBookmark(
                            chapterNo = chapterNo,
                            verseRange,
                            note = null
                        )
                    }
                }
            }
        ),
        LocalRecitationState provides LocalRecitationStateData(
            controller = controller,
            isAnyPlaying = isPlaying,
            playingVerse = recitationState.currentVerse,
        )
    ) {
        ModalBottomSheet(
            onDismissRequest = onClose,
            sheetState = sheetState,
            scrimColor = colorScheme.scrim.alpha(0.5f),
            containerColor = colorScheme.surface,
            contentColor = colorScheme.onSurface,
            dragHandle = null
        ) {
            QuickReferenceContent(
                data = data,
                parsed = parsed,
                onOpenInReader = onOpenInReader,
                onClose = onClose,
            )
        }

        VerseOptionsSheet(
            vwd = verseOptionsVerse,
            onFootnotes = { v ->
                verseOptionsVerse = null
                footnotePresenterData = FootnotePresenterData(v, null)
            },
        ) { verseOptionsVerse = null }

        FootnotePresenter(footnotePresenterData) {
            footnotePresenterData = null
        }

        BookmarkViewerSheet(bookmarkViewerData) {
            bookmarkViewerData = null
        }
    }

    QuickReference(
        data = quickReferenceStack,
        onOpenInReader = { chapterNo, range ->
            quickReferenceStack = null
            ReaderFactory.startVerseRange(context, chapterNo, range.first, range.last)
        },
        onClose = {
            quickReferenceStack = null
        },
    )
}

@Composable
private fun QuickReferenceContent(
    data: QuickReferenceData,
    parsed: ParsedVerses,
    onOpenInReader: (Int, IntRange) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current

    val repository = remember(context) { DatabaseProvider.getQuranRepository(context) }
    val bookmarksRepo = remember(context) { DatabaseProvider.getUserRepository(context) }
    val fontResolver = remember(context) { FontResolver.getInstance(context) }

    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    val verseActions = LocalVerseActions.current

    val verseNos = remember(parsed) { parsedVersesToList(parsed) }
    val verseRange = remember(parsed) { parsedVersesToIntRange(parsed) }
    val title = remember(data.chapterNo, parsed) { formatTitle(data.chapterNo, parsed) }

    var items by remember { mutableStateOf<List<ReaderLayoutItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val isBookmarked by if (verseRange != null) {
        bookmarksRepo
            .isBookmarkedFlow(data.chapterNo, verseRange)
            .collectAsStateWithLifecycle(false)
    } else {
        remember { mutableStateOf(false) }
    }

    LaunchedEffect(data, verseActions) {
        isLoading = true

        withContext(Dispatchers.IO) {
            val params = TextBuilderParams(
                context = context,
                fontResolver = fontResolver,
                verseActions = verseActions,
                colors = colors,
                type = type,
                arabicEnabled = ReaderPreferences.getArabicTextEnabled(),
                script = ReaderPreferences.getQuranScript(),
                arabicSizeMultiplier = ReaderPreferences.getArabicTextSizeMultiplier(),
                translationSizeMultiplier = ReaderPreferences.getTranslationTextSizeMultiplier(),
                slugs = ReaderPreferences.getTranslations(),
            )

            items = ReaderItemsBuilder.buildQuickReferenceItems(
                context, params, repository, data.chapterNo, verseNos
            )
        }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f),
    ) {
        QuickReferenceHeader(
            title = title,
            isBookmarked = isBookmarked,
            showActions = verseRange != null && !isLoading,
            onBookmark = {
                if (verseRange == null) return@QuickReferenceHeader
                verseActions.onBookmarkRequest?.invoke(data.chapterNo, verseRange)
            },
            onOpen = {
                if (verseRange != null) {
                    onOpenInReader(data.chapterNo, verseRange)
                    onClose()
                }
            },
            onClose = onClose,
        )

        if (data.slugs.isEmpty()) {
            TranslationWarning()
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                itemsIndexed(
                    items.filterIsInstance<ReaderLayoutItem.VerseUI>(),
                    key = { _, item -> item.key }
                ) { index, verseUi ->
                    VerseViewWrapped(
                        bookmarksRepo,
                        verseUi = verseUi,
                        showDivier = index < items.size - 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun VerseViewWrapped(
    bookmarksRepo: UserRepository,
    verseUi: ReaderLayoutItem.VerseUI,
    showDivier: Boolean
) {
    val verse = verseUi.verse

    val isBookmarked by bookmarksRepo
        .isBookmarkedFlow(verse.chapterNo, verse.verseNo..verse.verseNo)
        .collectAsStateWithLifecycle(false)

    VerseView(
        verseUi = verseUi,
        isBookmarked = isBookmarked,
        showDivider = showDivier,
    )
}

@Composable
private fun QuickReferenceHeader(
    title: String,
    isBookmarked: Boolean,
    showActions: Boolean,
    onBookmark: () -> Unit,
    onOpen: () -> Unit,
    onClose: () -> Unit,
) {
    val bookmarkTint = if (isBookmarked) colorResource(R.color.colorPrimary)
    else colorScheme.onSurface.alpha(0.7f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bottomBorder()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(
                painter = painterResource(R.drawable.dr_icon_close),
                contentDescription = stringResource(R.string.strLabelClose),
                tint = colorScheme.onSurface,
            )
        }

        Text(
            text = title,
            style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = colorScheme.primary,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
        )

        if (showActions) {
            IconButton(onClick = onBookmark) {
                Icon(
                    painter = painterResource(
                        if (isBookmarked) R.drawable.dr_icon_bookmark_added
                        else R.drawable.dr_icon_bookmark_outlined
                    ),
                    contentDescription = stringResource(R.string.strLabelBookmark),
                    tint = bookmarkTint,
                )
            }

            IconButton(onClick = onOpen) {
                Icon(
                    painter = painterResource(R.drawable.dr_icon_open),
                    contentDescription = stringResource(R.string.strLabelOpen),
                    tint = colorScheme.onSurface,
                )
            }
        } else {
            Spacer(modifier = Modifier.width(48.dp))
        }
    }
}

@Composable
private fun TranslationWarning() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.dr_icon_info),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = colorScheme.error,
        )
        Text(
            text = stringResource(R.string.strMsgTranslNoneSelected),
            style = typography.bodySmall,
            color = colorScheme.error,
        )
    }
}

@Composable
private fun ChapterOnlyDialog(
    chapterNo: Int,
    slugs: Set<String>,
    onOpen: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val repository = remember(context) { DatabaseProvider.getQuranRepository(context) }
    var chapterName by remember { mutableStateOf("") }

    LaunchedEffect(chapterNo) {
        chapterName = withContext(Dispatchers.IO) {
            repository.getChapterName(chapterNo)
        }
    }

    AlertDialog(
        isOpen = true,
        onClose = onClose,
        title = stringResource(R.string.strLabelOpen),
        actions = listOf(
            AlertDialogAction(
                text = stringResource(R.string.strLabelCancel),
                onClick = onClose,
            ),
            AlertDialogAction(
                text = stringResource(R.string.strLabelOpen),
                style = AlertDialogActionStyle.Primary,
                onClick = onOpen,
            ),
        ),
    ) {
        Text(
            text = chapterName.ifEmpty { "Surah $chapterNo" },
            style = typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
