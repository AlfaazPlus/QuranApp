package com.quranapp.android.compose.screens.reader

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quranapp.android.R
import com.quranapp.android.compose.components.player.MINI_PLAYER_HEIGHT
import com.quranapp.android.compose.components.player.RecitationPlayerSheet
import com.quranapp.android.compose.components.reader.ReaderLayout
import com.quranapp.android.compose.components.reader.ReaderMode
import com.quranapp.android.compose.components.reader.ReaderProvider
import com.quranapp.android.compose.components.reader.navigator.FullscreenMushafHeader
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

    val colors by rememberUpdatedState(MaterialTheme.colorScheme)
    val type by rememberUpdatedState(MaterialTheme.typography)

    val isDark = isSystemInDarkTheme()

    val miniPlayerTotalHeight = MINI_PLAYER_HEIGHT
    val coroutineScope = rememberCoroutineScope()

    var playerVerseSyncPref by readerVm.playerVerseSync
    var isSyncing by remember { mutableStateOf(false) }
    val syncIndicatorLocked = playerVerseSyncPref && isSyncing

    val readerMode by readerVm.readerMode.collectAsStateWithLifecycle()
    var isFullscreen by rememberSaveable { mutableStateOf(false) }

    var lastInitParams by remember { mutableStateOf<ReaderLaunchParams?>(null) }

    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
    }

    LaunchedEffect(params) {
        isSyncing = false

        if (lastInitParams != params) {
            readerVm.initReader(params)
            lastInitParams = params
        }
    }

    ReaderProvider {
        val verseActions = LocalVerseActions.current

        LaunchedEffect(lifecycleOwner) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                readerVm.observeChanges(context, colors, type, verseActions)
            }
        }

        BoxWithConstraints {
            val isWideScreen = maxWidth > 600.dp

            Box(modifier = Modifier.fillMaxSize()) {
                val chromeCollapsedFraction = scrollBehavior.state.collapsedFraction
                val navBarBottomInset = WindowInsets.navigationBars.asPaddingValues()
                    .calculateBottomPadding()
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = {
                        if (!isFullscreen) {
                            ReaderAppBar(
                                readerVm = readerVm,
                                isWideScreen = isWideScreen,
                                scrollBehavior = scrollBehavior,
                            )
                        }
                    },
                    containerColor = if (isDark || readerMode == ReaderMode.Translation) colorScheme.background
                    else colorScheme.surface
                ) { padding ->
                    Column(
                        Modifier
                            .padding(padding)
                            .padding(
                                bottom = if (isFullscreen) 0.dp else {
                                    miniPlayerTotalHeight * (1f - chromeCollapsedFraction)
                                },
                            )
                    ) {
                        if (isFullscreen && readerMode == ReaderMode.Reading) {
                            FullscreenMushafHeader(
                                readerVm = readerVm,
                            )
                            HorizontalDivider(
                                color = colorScheme.outlineVariant.copy(alpha = 0.4f),
                            )
                        }

                        ReaderLayout(
                            readerVm = readerVm,
                            nestedScrollConnection = scrollBehavior.nestedScrollConnection,
                            onSyncStateChanged = { isSyncing = it },
                        )
                    }
                }

                val fullscreenButtonAlpha = if (isFullscreen) 0.38f
                else (1f - chromeCollapsedFraction).coerceIn(0f, 1f)

                if (isFullscreen || fullscreenButtonAlpha > 0.01f) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(
                                end = 12.dp,
                                bottom = navBarBottomInset +
                                        if (isFullscreen) 12.dp else {
                                            miniPlayerTotalHeight * (1f - chromeCollapsedFraction) + 12.dp
                                        }
                            )
                            .alpha(fullscreenButtonAlpha),
                        color = colorScheme.surfaceContainerHighest,
                        tonalElevation = 8.dp,
                        shadowElevation = 6.dp,
                        shape = MaterialTheme.shapes.extraLarge,
                    ) {
                        IconButton(onClick = { isFullscreen = !isFullscreen }) {
                            Icon(
                                painter = painterResource(
                                    if (isFullscreen) R.drawable.ic_shrink
                                    else R.drawable.ic_expand
                                ),
                                contentDescription = stringResource(
                                    if (isFullscreen) R.string.exitFullscreen
                                    else R.string.enterFullscreen
                                ),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }

                RecitationPlayerSheet(
                    collapsedBottomInset = navBarBottomInset,
                    barsCollapsedFraction = scrollBehavior.state.collapsedFraction,
                    showPlayer = !isFullscreen,
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
