package com.quranapp.android.compose.screens.reader

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quranapp.android.compose.components.player.MINI_PLAYER_HEIGHT_DP
import com.quranapp.android.compose.components.player.RecitationPlayerSheet
import com.quranapp.android.compose.components.reader.LocalRecitationState
import com.quranapp.android.compose.components.reader.LocalRecitationStateData
import com.quranapp.android.compose.components.reader.ReaderLayout
import com.quranapp.android.compose.components.reader.dialogs.FootnotePresenter
import com.quranapp.android.compose.components.reader.dialogs.FootnotePresenterData
import com.quranapp.android.compose.components.reader.dialogs.QuickReference
import com.quranapp.android.compose.components.reader.dialogs.QuickReferenceData
import com.quranapp.android.compose.components.reader.dialogs.VerseOptionsSheet
import com.quranapp.android.compose.components.reader.navigator.ReaderAppBar
import com.quranapp.android.db.relations.VerseWithDetails
import com.quranapp.android.utils.reader.LocalVerseActions
import com.quranapp.android.utils.reader.ReaderLaunchParams
import com.quranapp.android.utils.reader.VerseActions
import com.quranapp.android.utils.reader.factory.ReaderFactory
import com.quranapp.android.viewModels.ReaderViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(params: ReaderLaunchParams) {
    val readerVm = viewModel<ReaderViewModel>()

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    val isDark = isSystemInDarkTheme()

    val miniPlayerTotalHeight = MINI_PLAYER_HEIGHT_DP.dp
    val coroutineScope = rememberCoroutineScope()

    var isSyncing by readerVm.playerVerseSync

    var lastInitParams by remember { mutableStateOf<ReaderLaunchParams?>(null) }

    Providers(readerVm) {
        val verseActions = LocalVerseActions.current

        LaunchedEffect(params, lifecycleOwner, context, colors, type, verseActions) {
            if (lastInitParams != params) {
                readerVm.initReader(params)
                lastInitParams = params
            }

            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                readerVm.observeChanges(context, colors, type, verseActions)
            }
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
                        )
                    },
                    containerColor = if (isDark) colorScheme.background else colorScheme.surface
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
                        val willSync = !isSyncing
                        isSyncing = willSync

                        if (willSync) {
                            coroutineScope.launch { readerVm.syncToPlayingVerse() }
                        }
                    }
                )
            }
        }
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

    var bookmarkViewerData by remember { mutableStateOf<BookmarkViewerData?>(null) }
    var footnotePresenterData by remember { mutableStateOf<FootnotePresenterData?>(null) }
    var verseOptionsVerse by remember { mutableStateOf<VerseWithDetails?>(null) }
    var quickReferenceStack by remember { mutableStateOf<QuickReferenceData?>(null) }

    val context = LocalContext.current

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
                    if (readerVm.bookmarksRepository.isBookmarked(
                            chapterNo,
                            verseRange
                        )
                    ) {
                        bookmarkViewerData = BookmarkViewerData(
                            chapterNo = chapterNo,
                            fromVerse = verseRange.first,
                            toVerse = verseRange.first,
                            showOpenInReaderButton = false,
                        )
                    } else {
                        readerVm.bookmarksRepository.addToBookmark(
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
            onVerseRecitationStarted = {
                readerVm.playerVerseSync.value = true
            }
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
