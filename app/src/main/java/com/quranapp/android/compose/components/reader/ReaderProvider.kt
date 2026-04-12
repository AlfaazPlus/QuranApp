package com.quranapp.android.compose.components.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quranapp.android.compose.components.reader.dialogs.FootnotePresenter
import com.quranapp.android.compose.components.reader.dialogs.FootnotePresenterData
import com.quranapp.android.compose.components.reader.dialogs.QuickReference
import com.quranapp.android.compose.components.reader.dialogs.QuickReferenceData
import com.quranapp.android.compose.components.reader.dialogs.VerseOptionsSheet
import com.quranapp.android.compose.components.reader.dialogs.BookmarkViewerData
import com.quranapp.android.compose.components.reader.dialogs.BookmarkViewerSheet
import com.quranapp.android.db.relations.VerseWithDetails
import com.quranapp.android.utils.reader.LocalVerseActions
import com.quranapp.android.utils.reader.VerseActions
import com.quranapp.android.utils.reader.factory.ReaderFactory
import com.quranapp.android.viewModels.ReaderProviderViewModel
import kotlinx.coroutines.launch


val LocalReaderViewModel = staticCompositionLocalOf<ReaderProviderViewModel> {
    error("ReaderProviderViewModel not provided")
}

@Composable
fun ReaderProvider(
    onVerseRecitationStarted: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val viewModel = viewModel<ReaderProviderViewModel>()

    val coroutineScope = rememberCoroutineScope()

    val controller = viewModel.controller
    val recitationState by controller.state.collectAsStateWithLifecycle()
    val isPlaying by controller.isPlayingState.collectAsStateWithLifecycle()

    var bookmarkViewerData by remember { mutableStateOf<BookmarkViewerData?>(null) }
    var footnotePresenterData by remember { mutableStateOf<FootnotePresenterData?>(null) }
    var verseOptionsVerse by remember { mutableStateOf<VerseWithDetails?>(null) }
    var quickReferenceData by remember { mutableStateOf<QuickReferenceData?>(null) }

    val context = LocalContext.current

    CompositionLocalProvider(
        LocalReaderViewModel provides viewModel,
        LocalVerseActions provides VerseActions(
            onReferenceClick = { slugs, chapterNo, verses ->
                quickReferenceData = QuickReferenceData(slugs, chapterNo, verses)
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
                    if (viewModel.userRepository.isBookmarked(
                            chapterNo,
                            verseRange
                        )
                    ) {
                        bookmarkViewerData = BookmarkViewerData(
                            chapterNo = chapterNo,
                            fromVerse = verseRange.first,
                            toVerse = verseRange.last,
                            showOpenInReaderButton = false,
                        )
                    } else {
                        viewModel.userRepository.addToBookmark(
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
            onVerseRecitationStarted = onVerseRecitationStarted
        )
    ) {
        content()

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

    // Should stay outside the composition provider
    QuickReference(
        data = quickReferenceData,
        onOpenInReader = { chapterNo, range ->
            quickReferenceData = null
            ReaderFactory.startVerseRange(context, chapterNo, range.first, range.last)
        },
        onClose = {
            quickReferenceData = null
        },
    )
}