package com.quranapp.android.compose.screens.reader

import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quranapp.android.compose.components.player.MINI_PLAYER_HEIGHT
import com.quranapp.android.compose.components.player.RecitationPlayerSheet
import com.quranapp.android.compose.components.reader.ReaderLayout
import com.quranapp.android.compose.components.reader.ReaderProvider
import com.quranapp.android.compose.components.reader.navigator.ReaderAppBar
import com.quranapp.android.compose.components.reader.navigator.ReaderAppBarExpandedHeight
import com.quranapp.android.utils.reader.LocalVerseActions
import com.quranapp.android.utils.reader.ReaderLaunchParams
import com.quranapp.android.viewModels.ReaderViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(params: ReaderLaunchParams) {
    val readerVm = viewModel<ReaderViewModel>()

    val density = LocalDensity.current
    val readerTopBarState = rememberTopAppBarState(
        initialHeightOffsetLimit = with(density) { -ReaderAppBarExpandedHeight.toPx() },
    )

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(readerTopBarState)
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    val isDark = isSystemInDarkTheme()

    val miniPlayerTotalHeight = MINI_PLAYER_HEIGHT
    val coroutineScope = rememberCoroutineScope()

    var playerVerseSyncPref by readerVm.playerVerseSync
    var isSyncing by remember { mutableStateOf(false) }
    val syncIndicatorLocked = playerVerseSyncPref && isSyncing

    var lastInitParams by remember { mutableStateOf<ReaderLaunchParams?>(null) }

    LaunchedEffect(params) {
        isSyncing = false
    }

    ReaderProvider {
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
                    val chromeCollapsedFraction = scrollBehavior.state.collapsedFraction

                    Column(
                        Modifier
                            .padding(padding)
                            .padding(
                                bottom = miniPlayerTotalHeight * (1f - chromeCollapsedFraction),
                            )
                    ) {
                        ReaderLayout(
                            readerVm = readerVm,
                            nestedScrollConnection = scrollBehavior.nestedScrollConnection,
                            onSyncStateChanged = { isSyncing = it },
                        )
                    }
                }

                RecitationPlayerSheet(
                    collapsedBottomInset = WindowInsets.navigationBars.asPaddingValues()
                        .calculateBottomPadding(),
                    barsCollapsedFraction = scrollBehavior.state.collapsedFraction,
                    isSyncing = syncIndicatorLocked,
                    onSyncRequest = {
                        val willSync = !playerVerseSyncPref
                        playerVerseSyncPref = willSync

                        if (willSync) {
                            coroutineScope.launch { readerVm.syncToPlayingVerse() }
                        }
                    }
                )
            }
        }
    }
}