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
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.compose.components.reader.dialogs.BookmarkViewerData
import com.quranapp.android.compose.components.reader.dialogs.BookmarkViewerSheet
import com.quranapp.android.compose.components.reader.dialogs.FootnotePresenter
import com.quranapp.android.compose.components.reader.dialogs.FootnotePresenterData
import com.quranapp.android.compose.components.reader.dialogs.QuickReference
import com.quranapp.android.compose.components.reader.dialogs.QuickReferenceData
import com.quranapp.android.compose.components.reader.dialogs.VerseOptionsSheet
import com.quranapp.android.compose.components.reader.dialogs.WbwSheet
import com.quranapp.android.compose.components.reader.dialogs.WbwSheetData
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.db.entities.quran.AyahWordEntity
import com.quranapp.android.db.relations.VerseWithDetails
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.mediaplayer.RecitationController
import com.quranapp.android.utils.mediaplayer.WbwAudioPlayer
import com.quranapp.android.utils.quran.QuranUtils
import com.quranapp.android.utils.reader.LocalVerseActions
import com.quranapp.android.utils.reader.VerseActions
import com.quranapp.android.utils.reader.factory.ReaderFactory
import com.quranapp.android.viewModels.ReaderProviderViewModel
import kotlinx.coroutines.launch


val LocalReaderViewModel = staticCompositionLocalOf<ReaderProviderViewModel> {
    error("ReaderProviderViewModel not provided")
}

data class LocalRecitationStateData(
    val controller: RecitationController,
    val isAnyPlaying: Boolean,
    val playingVerse: ChapterVersePair,
)

val LocalRecitation = staticCompositionLocalOf<LocalRecitationStateData> {
    error("LocalRecitationState not provided")
}

data class LocalWbwStateData(
    val activeTooltipWord: AyahWordEntity?,
    val onDismissTooltip: () -> Unit,
    val onForcePlay: (AyahWordEntity) -> Unit,
    val onWordClick: (AyahWordEntity) -> Unit,
    val warmUpWord: (Int, Int, Int) -> Unit,
    val isWbwAudioLoading: (Int, Int, Int) -> Boolean,
    val toggleWbwSheet: (WbwSheetData?) -> Unit,
    val isWbwSheetOpen: Boolean,
)

val LocalWbwState = staticCompositionLocalOf<LocalWbwStateData> {
    error("LocalWbwState not provided")
}

@Composable
fun ReaderProvider(
    content: @Composable () -> Unit
) {
    val viewModel = viewModel<ReaderProviderViewModel>()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val controller = viewModel.controller
    val recitationState by controller.state.collectAsStateWithLifecycle()
    val isPlaying by controller.isPlayingState.collectAsStateWithLifecycle()

    var bookmarkViewerData by remember { mutableStateOf<BookmarkViewerData?>(null) }
    var footnotePresenterData by remember { mutableStateOf<FootnotePresenterData?>(null) }
    var verseOptionsVerse by remember { mutableStateOf<VerseWithDetails?>(null) }
    var quickReferenceData by remember { mutableStateOf<QuickReferenceData?>(null) }
    var wbwSheetData by remember { mutableStateOf<WbwSheetData?>(null) }

    var wbwWordLoadingKey by remember { mutableStateOf<String?>(null) }

    var activeTooltipWord by remember { mutableStateOf<AyahWordEntity?>(null) }

    fun playWord(word: AyahWordEntity) {
        val (chapterNo, verseNo) = QuranUtils.getVerseNoFromAyahId(word.ayahId)

        coroutineScope.launch {
            val key = "$chapterNo:$verseNo:${word.wordIndex}"
            wbwWordLoadingKey = key

            try {
                WbwAudioPlayer.play(
                    context,
                    chapterNo,
                    verseNo,
                    word.wordIndex,
                )
            } finally {
                if (wbwWordLoadingKey == key) {
                    wbwWordLoadingKey = null
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalReaderViewModel provides viewModel,
        LocalVerseActions provides remember {
            VerseActions(
                onReferenceClick = { slugs, chapterNo, verses ->
                    quickReferenceData = QuickReferenceData(slugs, chapterNo, verses)
                },
                onVerseOption = { verse -> verseOptionsVerse = verse },
                onFootnoteClick = { verse, footnote ->
                    Log.d("FOOTNOTE", verse, footnote)
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
            )
        },
        LocalRecitation provides LocalRecitationStateData(
            controller = controller,
            isAnyPlaying = isPlaying,
            playingVerse = recitationState.currentVerse,
        ),
        LocalWbwState provides LocalWbwStateData(
            warmUpWord = { chapterNo, verseNo, wordIndex ->
                coroutineScope.launch {
                    WbwAudioPlayer.warmUp(
                        context,
                        chapterNo,
                        verseNo,
                        wordIndex,
                    )
                }
            },
            isWbwAudioLoading = { chapterNo, verseNo, wordIndex ->
                wbwWordLoadingKey == "$chapterNo:$verseNo:$wordIndex"
            },
            activeTooltipWord = activeTooltipWord,
            onDismissTooltip = { activeTooltipWord = null },
            onForcePlay = ::playWord,
            onWordClick = { word ->
                val shouldPlay = ReaderPreferences.getWbwRecitationEnabled()

                if (shouldPlay) {
                    playWord(word)
                }

                val tooltipEnabled = ReaderPreferences.getWbwTooltipShowTranslation() ||
                        ReaderPreferences.getWbwTooltipShowTransliteration()

                activeTooltipWord = if (tooltipEnabled) {
                    word
                } else {
                    null
                }
            },
            toggleWbwSheet = { data ->
                wbwSheetData = data
            },
            isWbwSheetOpen = wbwSheetData != null,
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

        WbwSheet(
            data = wbwSheetData,
            onDismiss = { wbwSheetData = null },
        )
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