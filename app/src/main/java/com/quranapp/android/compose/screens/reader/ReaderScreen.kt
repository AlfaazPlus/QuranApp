package com.quranapp.android.compose.screens.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quranapp.android.components.quran.subcomponents.Verse
import com.quranapp.android.compose.components.dialogs.BottomSheet
import com.quranapp.android.compose.components.player.MINI_PLAYER_HEIGHT_DP
import com.quranapp.android.compose.components.player.RecitationPlayerSheet
import com.quranapp.android.compose.components.reader.LocalRecitationState
import com.quranapp.android.compose.components.reader.LocalRecitationStateData
import com.quranapp.android.compose.components.reader.ReaderLayout
import com.quranapp.android.compose.components.reader.dialogs.FootnotePresenter
import com.quranapp.android.compose.components.reader.dialogs.FootnotePresenterData
import com.quranapp.android.compose.components.reader.dialogs.VerseOptionsSheet
import com.quranapp.android.compose.components.reader.navigator.ReaderAppBar
import com.quranapp.android.compose.components.reader.navigator.ReaderNavigator
import com.quranapp.android.utils.reader.LocalVerseActions
import com.quranapp.android.utils.reader.VerseActions
import com.quranapp.android.viewModels.ReaderIntentData
import com.quranapp.android.viewModels.ReaderViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(data: ReaderIntentData) {
    val readerVm = viewModel<ReaderViewModel>()

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography

    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val miniPlayerTotalHeight = MINI_PLAYER_HEIGHT_DP.dp + navBarBottom

    var showNavigatorSheet by remember { mutableStateOf(false) }
    var isSyncing by readerVm.playerVerseSync

    LaunchedEffect(data) {
        readerVm.initReader(data)
    }

    Providers(readerVm) {
        val verseActions = LocalVerseActions.current

        LaunchedEffect(context, colors, type, verseActions) {
            readerVm.observeChanges(context, colors, type, verseActions)
        }

        BoxWithConstraints {
            val isWideScreen = maxWidth > 600.dp

            Box(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = {
                        ReaderAppBar(
                            readerVm = readerVm,
                            isWideScreen = isWideScreen,
                            scrollBehavior = scrollBehavior,
                            onNavigatorRequest = { showNavigatorSheet = true },
                        )
                    },
                ) { padding ->
                    Column(
                        Modifier
                            .padding(padding)
                            .padding(bottom = miniPlayerTotalHeight)
                    ) {
                        ReaderLayout(
                            readerVm = readerVm,
                        )
                    }
                }

                RecitationPlayerSheet(
                    isSyncing = isSyncing,
                    onSyncRequest = {
                        // TODO: if the playing verse is not list then navigate
                        isSyncing = !isSyncing
                    }
                )
            }
        }
    }

    BottomSheet(
        isOpen = showNavigatorSheet,
        onDismiss = { showNavigatorSheet = false },
        dragHandle = null,
    ) {
        ReaderNavigator(
            readerVm = readerVm,
            isInBottomSheet = true,
            onClose = { showNavigatorSheet = false },
        )
    }
}

@Composable
private fun Providers(
    readerVm: ReaderViewModel,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    val controller = readerVm.controller
    val recitationState by controller.state.collectAsStateWithLifecycle()
    val isPlaying by controller.isPlayingState.collectAsStateWithLifecycle()

    var footnotePresenterData by remember { mutableStateOf<FootnotePresenterData?>(null) }
    var verseOptionsVerse by remember { mutableStateOf<Verse?>(null) }
    var bookmarkViewerData by remember { mutableStateOf<BookmarkViewerData?>(null) }

    CompositionLocalProvider(
        LocalVerseActions provides VerseActions(
            onReferenceClick = { slugs, chapterNo, verses ->

            },
            onVerseOption = { verse -> verseOptionsVerse = verse },
            onFootnoteClick = { verse, footnote ->
                footnotePresenterData = FootnotePresenterData(
                    verse,
                    footnote
                )
            },
            onBookmarkRequest = {
                coroutineScope.launch {
                    if (readerVm.bookmarksRepository.isBookmarked(
                            it.chapterNo,
                            it.verseNo,
                            it.verseNo
                        )
                    ) {
                        bookmarkViewerData = BookmarkViewerData(
                            chapterNo = it.chapterNo,
                            fromVerse = it.verseNo,
                            toVerse = it.verseNo,
                            showOpenInReaderButton = false,
                        )
                    } else {
                        readerVm.bookmarksRepository.addToBookmark(
                            chapterNo = it.chapterNo,
                            fromVerse = it.verseNo,
                            toVerse = it.verseNo,
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
            onVerseRecitationStarted = {
                readerVm.playerVerseSync.value = true
            }
        )
    ) {
        content()

        VerseOptionsSheet(
            verse = verseOptionsVerse,
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
}
