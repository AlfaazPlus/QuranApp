package com.quranapp.android.compose.screens.reader

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
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
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.utils.reader.LocalVerseActions
import com.quranapp.android.utils.reader.QuranScriptUtils
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

    val coroutineScope = rememberCoroutineScope()

    var playerVerseSyncPref by readerVm.playerVerseSync
    var isSyncing by rememberSaveable { mutableStateOf(false) }
    val syncIndicatorLocked = playerVerseSyncPref && isSyncing

    val readerMode by readerVm.readerMode.collectAsStateWithLifecycle()

    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    var tajweedBarVisible by rememberSaveable { mutableStateOf(false) }

    val miniPlayerHeight = if (isFullscreen || tajweedBarVisible) 0.dp else MINI_PLAYER_HEIGHT

    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
    }

    LaunchedEffect(params) {
        isSyncing = false
        readerVm.initReaderIfNeeded(params)
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
                                bottom = miniPlayerHeight * (1f - chromeCollapsedFraction),
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


                val totalBottomOffset =
                    navBarBottomInset + miniPlayerHeight * (1f - chromeCollapsedFraction)

                FloatingBar(
                    isFullscreen,
                    onChangeFullscreen = { isFullscreen = it },
                    tajweedBarVisible,
                    onChangeTajweedBarVisible = { tajweedBarVisible = it },
                    chromeCollapsedFraction,
                    totalBottomOffset,
                )

                RecitationPlayerSheet(
                    collapsedBottomInset = navBarBottomInset,
                    barsCollapsedFraction = scrollBehavior.state.collapsedFraction,
                    showPlayer = !isFullscreen && !tajweedBarVisible,
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

@Composable
private fun BoxScope.FloatingBar(
    isFullscreen: Boolean,
    onChangeFullscreen: (Boolean) -> Unit,
    tajweedBarVisible: Boolean,
    onChangeTajweedBarVisible: (Boolean) -> Unit,
    chromeCollapsedFraction: Float,
    totalBottomOffset: Dp
) {
    val fullscreenButtonAlpha = if (isFullscreen) if (tajweedBarVisible) 1f else 0.65f
    else (1f - chromeCollapsedFraction).coerceIn(0f, 1f)

    val tajweedSupported =
        ReaderPreferences.observeQuranScript() == QuranScriptUtils.SCRIPT_KFQPC_V4

    LaunchedEffect(tajweedSupported) {
        if (!tajweedSupported) {
            onChangeTajweedBarVisible(false)
        }
    }

    val rulesMap = remember {
        mapOf(
            Color(0xFF999999) to R.string.tajweed_silent_letter,
            Color(0xFFffc1e0) to R.string.tajweed_normal_madd,
            Color(0xFFff8e3b) to R.string.tajweed_separated_madd,
            Color(0xFFff5e8e) to R.string.tajweed_connected_madd,
            Color(0xFFe30000) to R.string.tajweed_necessary_madd,
            Color(0xFF26b55d) to R.string.tajweed_ghunna,
            Color(0xFF00deff) to R.string.tajweed_qalqala,
            Color(0xFF3c84d5) to R.string.tajweed_tafkhim,
        )
    }

    Column(
        Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = totalBottomOffset)
            .padding(
                horizontal = 12.dp,
                vertical = 6.dp
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .alpha(fullscreenButtonAlpha),
            ) {
                if (tajweedSupported) {
                    TextButton(
                        onClick = {
                            onChangeTajweedBarVisible(!tajweedBarVisible)
                        },
                        modifier = Modifier.height(32.dp),
                        shape = shapes.extraLarge,
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = colorScheme.surfaceContainer,
                            contentColor = colorScheme.onSurface
                        ),
                        border = BorderStroke(1.dp, colorScheme.outlineVariant),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 2.dp
                        ),
                        contentPadding = PaddingValues(vertical = 0.dp, horizontal = 16.dp)
                    ) {
                        Text(stringResource(R.string.tajweedColors))
                        Icon(
                            painterResource(R.drawable.dr_icon_chevron_down),
                            contentDescription = null,
                            modifier = Modifier
                                .rotate(if (tajweedBarVisible) 0f else 180f)
                                .size(16.dp),
                        )
                    }
                }
            }

            if (isFullscreen || fullscreenButtonAlpha > 0.01f) {
                TextButton(
                    onClick = {
                        onChangeFullscreen(!isFullscreen)
                    },
                    modifier = Modifier
                        .height(32.dp)
                        .alpha(fullscreenButtonAlpha),
                    shape = shapes.extraLarge,
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = colorScheme.surfaceContainer,
                        contentColor = colorScheme.onSurface
                    ),
                    border = BorderStroke(1.dp, colorScheme.outlineVariant),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 2.dp
                    ),
                    contentPadding = PaddingValues(vertical = 0.dp, horizontal = 16.dp)
                ) {
                    Icon(
                        painter = painterResource(
                            if (isFullscreen) R.drawable.ic_shrink
                            else R.drawable.ic_expand
                        ),
                        contentDescription = stringResource(
                            if (isFullscreen) R.string.exitFullscreen
                            else R.string.enterFullscreen
                        ),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        AnimatedVisibility(tajweedSupported && tajweedBarVisible) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .shadow(4.dp, shape = shapes.large)
                    .background(
                        colorScheme.surfaceContainer,
                        shape = shapes.large
                    )
                    .border(
                        BorderStroke(1.dp, colorScheme.outlineVariant),
                        shape = shapes.large
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                rulesMap.forEach { (color, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            Modifier
                                .size(12.dp)
                                .background(color, shape = shapes.small)
                        )
                        Text(
                            stringResource(label),
                            style = MaterialTheme.typography.labelMedium,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
