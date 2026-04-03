package com.quranapp.android.compose.screens.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quranapp.android.compose.components.dialogs.BottomSheet
import com.quranapp.android.compose.components.player.RecitationPlayerSheet
import com.quranapp.android.compose.components.reader.ReaderLayout
import com.quranapp.android.compose.components.reader.dialogs.FootnotePresenter
import com.quranapp.android.compose.components.reader.dialogs.FootnotePresenterData
import com.quranapp.android.compose.components.reader.navigator.ReaderAppBar
import com.quranapp.android.compose.components.reader.navigator.ReaderNavigator
import com.quranapp.android.utils.reader.LocalVerseActions
import com.quranapp.android.utils.reader.VerseActions
import com.quranapp.android.viewModels.ReaderIntentData
import com.quranapp.android.viewModels.ReaderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(data: ReaderIntentData) {
    val readerVm = viewModel<ReaderViewModel>()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    var showNavigatorSheet by remember { mutableStateOf(false) }
    var footnotePresenterData by remember { mutableStateOf<FootnotePresenterData?>(null) }

    LaunchedEffect(data) {
        readerVm.initReader(data)
    }

    CompositionLocalProvider(
        LocalVerseActions provides VerseActions(
            onReferenceClick = { slugs, chapterNo, verses ->

            },
            onFootnoteClick = { verse, footnote ->
                footnotePresenterData = FootnotePresenterData(
                    verse,
                    footnote
                )
            }
        )
    ) {
        BoxWithConstraints {
            val isWideScreen = maxWidth > 600.dp

            Box(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        ReaderAppBar(
                            readerVm = readerVm,
                            isWideScreen = isWideScreen,
                            scrollBehavior = scrollBehavior,
                            onReaderTitleClick = { showNavigatorSheet = true },
                        )
                    },
                ) { padding ->
                    Column(Modifier.padding(padding)) {
                        ReaderLayout(
                            readerVm = readerVm,
                        )
                    }
                }

                RecitationPlayerSheet()
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

    FootnotePresenter(
        data = footnotePresenterData,
    ) {
        footnotePresenterData = null
    }
}
