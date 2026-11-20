package com.quranapp.android.components.player

import android.app.Activity
import android.os.SystemClock
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.core.view.WindowCompat
import com.quranapp.android.components.common.dialogs.SimpleTooltip
import com.quranapp.android.components.player.dialogs.AudioEndBehaviourSheet
import com.quranapp.android.components.player.dialogs.AudioOptionsSheet
import com.quranapp.android.components.player.dialogs.PlaybackSpeedSheet
import com.quranapp.android.components.player.dialogs.ReciterSelectorSheet
import com.quranapp.android.components.player.dialogs.RepeatOptionsSheet
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.audioBothArabicTranslation
import com.quranapp.android.generated.resources.audioOnlyArabic
import com.quranapp.android.generated.resources.audioOnlyTranslation
import com.quranapp.android.generated.resources.audioOption
import com.quranapp.android.generated.resources.dr_icon_chevron_down
import com.quranapp.android.generated.resources.dr_icon_menu
import com.quranapp.android.generated.resources.dr_icon_open
import com.quranapp.android.generated.resources.dr_icon_settings
import com.quranapp.android.generated.resources.expandedPlayerModeControls
import com.quranapp.android.generated.resources.expandedPlayerModeSpotlight
import com.quranapp.android.generated.resources.ic_forward_5
import com.quranapp.android.generated.resources.ic_mic
import com.quranapp.android.generated.resources.ic_pause
import com.quranapp.android.generated.resources.ic_play
import com.quranapp.android.generated.resources.ic_repeat
import com.quranapp.android.generated.resources.ic_replay_5
import com.quranapp.android.generated.resources.ic_skip_back
import com.quranapp.android.generated.resources.ic_skip_forward
import com.quranapp.android.generated.resources.icon_playback_speed
import com.quranapp.android.generated.resources.moreOptions
import com.quranapp.android.generated.resources.nTimes
import com.quranapp.android.generated.resources.notSupported
import com.quranapp.android.generated.resources.once
import com.quranapp.android.generated.resources.playNextSurah
import com.quranapp.android.generated.resources.playbackCount
import com.quranapp.android.generated.resources.playbackSpeed
import com.quranapp.android.generated.resources.repeatCurrentSurah
import com.quranapp.android.generated.resources.stopPlayback
import com.quranapp.android.generated.resources.strLabelNextVerse
import com.quranapp.android.generated.resources.strLabelOpenInReader
import com.quranapp.android.generated.resources.strLabelPause
import com.quranapp.android.generated.resources.strLabelPlay
import com.quranapp.android.generated.resources.strLabelPreviousVerse
import com.quranapp.android.generated.resources.strTitleSelectReciter
import com.quranapp.android.generated.resources.twice
import com.quranapp.android.generated.resources.whenChapterEnds
import com.quranapp.android.locale.LocalAppLocale
import com.quranapp.android.navigation.AppRoute
import com.quranapp.android.platform.Platform
import com.quranapp.android.preferences.RecitationPreferences
import com.quranapp.android.theme.alpha
import com.quranapp.android.utils.mediaplayer.AudioEndBehaviour
import com.quranapp.android.utils.mediaplayer.AudioOption
import com.quranapp.android.utils.mediaplayer.RecitationController
import com.quranapp.android.utils.mediaplayer.RecitationModelManager
import com.quranapp.android.utils.mediaplayer.RecitationServiceState
import com.quranapp.android.utils.miscellenous.DateUtils
import com.quranapp.android.utils.reader.factory.ReaderFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

val PlayerBgColor = Color(0xFF000000)
val PlayerContentColor = Color.White

private enum class ExpandedPlayerMode {
    Controls,
    Spotlight,
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpandedPlayer(
    state: RecitationServiceState,
    isPlaying: Boolean,
    isLoading: Boolean,
    controller: RecitationController,
    onCollapse: () -> Unit,
    onSyncRequest: ((forceJump: Boolean) -> Unit)?,
) {
    val verse = state.currentVerse

    var mode by remember { mutableStateOf(ExpandedPlayerMode.Controls) }

    val pagerState = rememberPagerState(
        initialPage = mode.ordinal,
        pageCount = { ExpandedPlayerMode.entries.size },
    )
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var spotlightHeaderVisible by remember { mutableStateOf(true) }
    val headerVisibilityProgress by animateFloatAsState(
        targetValue = if (mode == ExpandedPlayerMode.Spotlight && !spotlightHeaderVisible) 0f else 1f,
        animationSpec = tween(220),
        label = "expanded_player_header_visibility"
    )

    LaunchedEffect(pagerState.currentPage) {
        mode = ExpandedPlayerMode.entries[pagerState.currentPage]
    }

    LaunchedEffect(mode) {
        if (pagerState.currentPage != mode.ordinal) {
            pagerState.animateScrollToPage(mode.ordinal)
        }
        if (mode != ExpandedPlayerMode.Spotlight) {
            spotlightHeaderVisible = true
        }
    }

    val view = LocalView.current

    if (!view.isInEditMode) {
        DisposableEffect(Unit) {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)

            val previousLightNav = controller.isAppearanceLightNavigationBars
            val previousLightStatus = controller.isAppearanceLightStatusBars

            controller.isAppearanceLightNavigationBars = false
            controller.isAppearanceLightStatusBars = false

            onDispose {
                controller.isAppearanceLightNavigationBars = previousLightNav
                controller.isAppearanceLightStatusBars = previousLightStatus
            }
        }
    }

    val showReciterSelector = rememberSaveable { mutableStateOf(false) }
    val showAudioOptions = rememberSaveable { mutableStateOf(false) }
    val showRepeatOptions = rememberSaveable { mutableStateOf(false) }
    val showMoreMenu = rememberSaveable { mutableStateOf(false) }

    val showPlaybackSpeedOptions = rememberSaveable { mutableStateOf(false) }
    val showEndBehaviorOptions = rememberSaveable { mutableStateOf(false) }

    Background(
        modifier = Modifier
            .navigationBarsPadding()
            .fillMaxSize(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = headerVisibilityProgress
                        translationY = -size.height * (1f - headerVisibilityProgress) * 0.5f
                    },
            ) {
                ExpandedPlayerHeader(
                    mode = mode,
                    onModeChange = { selectedMode ->
                        scope.launch { pagerState.animateScrollToPage(selectedMode.ordinal) }
                    },
                    onCollapse = onCollapse,
                    onOpenInReader = {
                        if (onSyncRequest != null) onSyncRequest(true)
                        else Platform.navigator.navigate(
                            AppRoute.Reader(
                                ReaderFactory.verse(
                                    verse.chapterNo,
                                    verse.verseNo
                                )
                            )
                        )
                    }
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                val currentMode = ExpandedPlayerMode.entries[page]

                when (currentMode) {
                    ExpandedPlayerMode.Controls -> {
                        BoxWithConstraints(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            val wide = maxWidth > maxHeight
                            val shorterSide = minOf(maxWidth, maxHeight)
                            val gutter = shorterSide * 0.045f
                            val blockGap = gutter * 0.65f
                            val controlsPane = maxWidth * 0.6f
                            val playSize = if (wide) controlsPane * 0.1f else shorterSide * 0.16f
                            val sideControlSize = playSize * 0.7f

                            if (wide) {
                                val shortLandscape = maxHeight < maxWidth * 0.55f
                                val playbackColumn: @Composable () -> Unit = {
                                    PlayerConfigurations(
                                        controller = controller,
                                        showReciterSelector = showReciterSelector,
                                        showAudioOptions = showAudioOptions,
                                        showRepeatOptions = showRepeatOptions,
                                        showMoreMenu = showMoreMenu,
                                        showPlaybackSpeedOptions = showPlaybackSpeedOptions,
                                        showEndBehaviorOptions = showEndBehaviorOptions,
                                        horizontalPadding = 0.dp,
                                        verticalPadding = blockGap,
                                        gap = blockGap,
                                    )

                                    ProgressSeekBar(
                                        isPlaying = isPlaying,
                                        isLoading = isLoading,
                                        controller = controller,
                                        horizontalPadding = 0.dp,
                                    )

                                    Spacer(Modifier.height(blockGap))

                                    ExpandedTransportControls(
                                        isPlaying = isPlaying,
                                        isLoading = isLoading,
                                        controller = controller,
                                        bottomPadding = 0.dp,
                                        playSize = playSize,
                                        sideSize = sideControlSize,
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(gutter),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    LandscapePlayerIdentity(
                                        verse = verse,
                                        modifier = Modifier
                                            .weight(0.4f)
                                            .fillMaxHeight(),
                                    )

                                    Column(
                                        modifier = Modifier
                                            .weight(0.6f)
                                            .fillMaxHeight()
                                            .padding(start = gutter),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        if (shortLandscape) {
                                            PlayerMessages(
                                                state,
                                                isPlaying,
                                            )
                                            Spacer(Modifier.weight(1f))
                                            playbackColumn()
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxWidth(),
                                                contentAlignment = Alignment.BottomCenter,
                                            ) {
                                                PlayerMessages(
                                                    state,
                                                    isPlaying,
                                                )
                                            }
                                            playbackColumn()
                                            Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                            } else {
                                val thumbnailSize = minOf(
                                    maxWidth - gutter * 2,
                                    maxHeight * 0.36f,
                                )

                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = gutter, vertical = blockGap),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Box(
                                        Modifier.weight(1f),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        ExtendedThumbnail(
                                            verse = verse,
                                            modifier = Modifier.size(thumbnailSize),
                                        )
                                    }

                                    PlayerMessages(
                                        state,
                                        isPlaying,
                                    )

                                    PlayerConfigurations(
                                        controller = controller,
                                        showReciterSelector = showReciterSelector,
                                        showAudioOptions = showAudioOptions,
                                        showRepeatOptions = showRepeatOptions,
                                        showMoreMenu = showMoreMenu,
                                        showPlaybackSpeedOptions = showPlaybackSpeedOptions,
                                        showEndBehaviorOptions = showEndBehaviorOptions,
                                        horizontalPadding = 0.dp,
                                        verticalPadding = blockGap,
                                        gap = blockGap,
                                    )

                                    ProgressSeekBar(
                                        isPlaying = isPlaying,
                                        isLoading = isLoading,
                                        controller = controller,
                                        horizontalPadding = 0.dp,
                                    )

                                    Spacer(Modifier.height(blockGap))

                                    ExpandedTransportControls(
                                        isPlaying = isPlaying,
                                        isLoading = isLoading,
                                        controller = controller,
                                        bottomPadding = blockGap,
                                        playSize = playSize,
                                        sideSize = sideControlSize,
                                    )
                                }
                            }
                        }
                    }

                    ExpandedPlayerMode.Spotlight -> {
                        ExpandedPlayerSpotlightSection(
                            modifier = Modifier.fillMaxSize(),
                            verse = verse,
                            isPlaying = isPlaying,
                            isLoading = isLoading,
                            controller = controller,
                            chromeVisible = spotlightHeaderVisible,
                            onChromeVisibilityChanged = { spotlightHeaderVisible = it },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandedPlayerHeader(
    mode: ExpandedPlayerMode,
    onModeChange: (ExpandedPlayerMode) -> Unit,
    onCollapse: () -> Unit,
    onOpenInReader: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconButton(onClick = onCollapse) {
            Icon(
                painterResource(Res.drawable.dr_icon_chevron_down),
                contentDescription = "Collapse",
                modifier = Modifier.size(26.dp),
                tint = PlayerContentColor,
            )
        }

        ModeTabs(
            selected = mode,
            onSelect = onModeChange,
        )

        SimpleTooltip(
            stringResource(Res.string.strLabelOpenInReader)
        ) {
            IconButton(
                onClick = {
                    onOpenInReader()
                    onCollapse()
                },
            ) {
                Icon(
                    painterResource(Res.drawable.dr_icon_open),
                    contentDescription = stringResource(Res.string.strLabelOpenInReader),
                    modifier = Modifier.size(22.dp),
                    tint = PlayerContentColor,
                )
            }
        }
    }
}

@Composable
private fun RowScope.ModeTabs(
    selected: ExpandedPlayerMode,
    onSelect: (ExpandedPlayerMode) -> Unit,
) {
    Box(
        Modifier.weight(1f),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            ExpandedPlayerMode.entries.forEach { tab ->
                val isSelected = tab == selected

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (isSelected) Color.White.copy(alpha = 0.14f) else Color.Transparent,
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelect(tab) },
                        )
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(
                            when (tab) {
                                ExpandedPlayerMode.Controls -> Res.string.expandedPlayerModeControls
                                ExpandedPlayerMode.Spotlight -> Res.string.expandedPlayerModeSpotlight
                            },
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) PlayerContentColor else PlayerContentColor.alpha(
                            0.55f
                        ),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerConfigurations(
    controller: RecitationController,
    showReciterSelector: MutableState<Boolean>,
    showAudioOptions: MutableState<Boolean>,
    showRepeatOptions: MutableState<Boolean>,
    showMoreMenu: MutableState<Boolean>,
    showPlaybackSpeedOptions: MutableState<Boolean>,
    showEndBehaviorOptions: MutableState<Boolean>,
    horizontalPadding: Dp,
    verticalPadding: Dp,
    gap: Dp,
) {
    val appLocale = LocalAppLocale.current
    val context = LocalContext.current
    val audioOption = RecitationPreferences.observeAudioOption();
    val speed = RecitationPreferences.observeSpeed();
    val audioEndBehaviour = RecitationPreferences.observeAudioEndBehaviour()
    val repeatCount = RecitationPreferences.observeRepeatCount()
    val reciterNames =
        RecitationModelManager.rememberCurrentReciterNameForAudioOption()

    val audioOptionTextId = when (audioOption) {
        AudioOption.ONLY_TRANSLATION -> Res.string.audioOnlyTranslation
        AudioOption.BOTH -> Res.string.audioBothArabicTranslation
        else -> Res.string.audioOnlyArabic
    }

    val repeatSupported = audioOption == AudioOption.ONLY_QURAN
    val speedSubtext = appLocale.format("%.1fx", speed)
    val audioEndBehaviourSubtextId = when (audioEndBehaviour) {
        AudioEndBehaviour.STOP_PLAYBACK -> Res.string.stopPlayback
        AudioEndBehaviour.NEXT_CHAPTER -> Res.string.playNextSurah
        AudioEndBehaviour.REPEAT_CHAPTER -> Res.string.repeatCurrentSurah
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = verticalPadding),
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding),
        ) {
            PlayerConfigButton(
                text = stringResource(Res.string.strTitleSelectReciter),
                icon = painterResource(Res.drawable.ic_mic),
                subtext = reciterNames,
                onClick = {
                    showReciterSelector.value = true
                },
                modifier = Modifier.weight(1f),
            )

            PlayerConfigButton(
                text = stringResource(Res.string.audioOption),
                subtext = stringResource(audioOptionTextId),
                icon = painterResource(Res.drawable.dr_icon_settings),
                onClick = {
                    showAudioOptions.value = true
                },
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding),
        ) {
            PlayerConfigButton(
                text = stringResource(Res.string.playbackCount),
                icon = painterResource(Res.drawable.ic_repeat),
                enabled = repeatSupported,
                subtext = when {
                    !repeatSupported -> stringResource(Res.string.notSupported)
                    repeatCount == 0 -> stringResource(Res.string.once)
                    repeatCount == 1 -> stringResource(Res.string.twice)
                    else -> stringResource(Res.string.nTimes, repeatCount + 1)
                },
                onClick = {
                    showRepeatOptions.value = true
                },
                modifier = Modifier.weight(1f),
            )

            Box(
                modifier = Modifier.weight(1f),
            ) {
                PlayerConfigButton(
                    text = stringResource(Res.string.moreOptions),
                    icon = painterResource(Res.drawable.dr_icon_menu),
                    onClick = {
                        showMoreMenu.value = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                DropdownMenu(
                    expanded = showMoreMenu.value,
                    onDismissRequest = { showMoreMenu.value = false },
                    shape = RoundedCornerShape(12.dp),
                    offset = DpOffset(0.dp, 6.dp),
                    containerColor = PlayerBgColor.alpha(0.8f),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    border = BorderStroke(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.25f),
                    ),
                ) {
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                painterResource(Res.drawable.icon_playback_speed),
                                contentDescription = stringResource(Res.string.playbackSpeed),
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        text = {
                            Column(
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = stringResource(Res.string.playbackSpeed),
                                    color = PlayerContentColor,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                )
                                Text(
                                    text = speedSubtext,
                                    color = PlayerContentColor.alpha(0.70f),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                )
                            }
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = PlayerContentColor,
                        ),
                        onClick = {
                            showMoreMenu.value = false
                            showPlaybackSpeedOptions.value = true
                        },
                    )
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                painterResource(Res.drawable.dr_icon_settings),
                                contentDescription = stringResource(Res.string.whenChapterEnds),
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }, text = {
                            Column(
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = stringResource(Res.string.whenChapterEnds),
                                    color = PlayerContentColor,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                )
                                Text(
                                    text = stringResource(audioEndBehaviourSubtextId),
                                    color = PlayerContentColor.alpha(0.70f),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                )
                            }
                        }, colors = MenuDefaults.itemColors(
                            textColor = PlayerContentColor,
                        ), onClick = {
                            showMoreMenu.value = false
                            showEndBehaviorOptions.value = true
                        })
                }
            }
        }
    }

    ReciterSelectorSheet(
        controller = controller,
        isOpen = showReciterSelector.value,
    ) {
        showReciterSelector.value = false
    }

    AudioOptionsSheet(
        controller = controller,
        isOpen = showAudioOptions.value,
    ) {
        showAudioOptions.value = false
    }

    RepeatOptionsSheet(
        controller = controller,
        isOpen = showRepeatOptions.value,
    ) {
        showRepeatOptions.value = false
    }

    PlaybackSpeedSheet(
        controller = controller,
        isOpen = showPlaybackSpeedOptions.value,
    ) {
        showPlaybackSpeedOptions.value = false
    }

    AudioEndBehaviourSheet(
        controller = controller,
        isOpen = showEndBehaviorOptions.value,
    ) {
        showEndBehaviorOptions.value = false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgressSeekBar(
    isPlaying: Boolean,
    isLoading: Boolean,
    controller: RecitationController,
    horizontalPadding: Dp,
) {
    var positionMs by remember { mutableLongStateOf(controller.currentPositionMs) }
    var durationMs by remember { mutableLongStateOf(controller.durationMs) }
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    var pendingSeekMs by remember { mutableStateOf<Long?>(null) }
    var pendingSeekAt by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isPlaying) {
        fun syncFromPlayer() {
            durationMs = controller.durationMs

            if (isDragging) return

            val pending = pendingSeekMs
            val actual = controller.currentPositionMs

            val seekSettled = pending == null ||
                    abs(actual - pending) < 750L ||
                    SystemClock.elapsedRealtime() - pendingSeekAt > 1500L

            if (seekSettled) {
                pendingSeekMs = null
                positionMs = actual
            }
        }

        syncFromPlayer()

        while (isPlaying && isActive) {
            syncFromPlayer()
            delay(100.milliseconds)
        }

        if (!isPlaying) syncFromPlayer()
    }

    val progress = if (durationMs > 0) positionMs.toFloat() / durationMs.toFloat() else 0f
    val sliderValue = (if (isDragging) dragProgress else progress).coerceIn(0f, 1f)

    val displayedPositionMs = if (isDragging && durationMs > 0L) {
        (dragProgress * durationMs).toLong().fastCoerceIn(0L, durationMs)
    } else {
        positionMs.fastCoerceIn(0, durationMs)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
    ) {
        Slider(
            value = sliderValue,
            onValueChange = { value ->
                isDragging = true
                dragProgress = value
            },
            onValueChangeFinished = {
                if (durationMs > 0) {
                    val seekPosition = (dragProgress * durationMs).toLong().coerceIn(0L, durationMs)

                    positionMs = seekPosition
                    pendingSeekMs = seekPosition
                    pendingSeekAt = SystemClock.elapsedRealtime()
                    controller.seekTo(seekPosition)
                }

                isDragging = false
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),

            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent
            ),

            thumb = {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .shadow(6.dp, CircleShape)
                        .background(Color.White, CircleShape)
                )
            },

            track = { sliderPositions ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0x33FFFFFF))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(sliderPositions.value)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        colorScheme.primary, colorScheme.primaryContainer
                                    )
                                )
                            )
                    )
                }
            })

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = DateUtils.durationFormatted(displayedPositionMs),
                style = MaterialTheme.typography.labelMedium,
                color = PlayerContentColor.alpha(0.7f),
            )
            Text(
                text = DateUtils.durationFormatted(durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = PlayerContentColor.alpha(0.7f),
            )
        }
    }
}

@Composable
private fun ExpandedTransportControls(
    isPlaying: Boolean,
    isLoading: Boolean,
    controller: RecitationController,
    bottomPadding: Dp,
    playSize: Dp,
    sideSize: Dp,
) {
    val sideIconSize = sideSize * 0.62f
    val playIconSize = playSize * 0.48f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = bottomPadding),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { controller.previousVerse() },
            modifier = Modifier.size(sideSize),
        ) {
            Icon(
                painterResource(Res.drawable.ic_skip_back),
                contentDescription = stringResource(Res.string.strLabelPreviousVerse),
                modifier = Modifier.size(sideIconSize),
                tint = PlayerContentColor.alpha(.7f),
            )
        }

        IconButton(
            onClick = { controller.seekLeft() },
            modifier = Modifier.size(sideSize),
        ) {
            Icon(
                painterResource(Res.drawable.ic_replay_5),
                contentDescription = null,
                modifier = Modifier.size(sideIconSize),
                tint = PlayerContentColor.alpha(.7f),
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(playSize)
                .shadow(
                    elevation = playSize * 0.16f,
                    shape = CircleShape,
                    ambientColor = colorScheme.primary.copy(alpha = 0.35f),
                    spotColor = colorScheme.primary.copy(alpha = 0.45f),
                )
                .clip(CircleShape)
                .background(colorScheme.primary)
                .clickable(
                    onClick = { controller.playPause() },
                ),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(playIconSize),
                    strokeWidth = playSize * 0.04f,
                    color = colorScheme.onPrimary,
                )
            } else {
                Icon(
                    painterResource(
                        if (isPlaying) Res.drawable.ic_pause
                        else Res.drawable.ic_play,
                    ),
                    contentDescription = if (isPlaying) stringResource(Res.string.strLabelPause)
                    else stringResource(Res.string.strLabelPlay),
                    modifier = Modifier.size(playIconSize),
                    tint = colorScheme.onPrimary,
                )
            }
        }

        IconButton(
            onClick = { controller.seekRight() },
            modifier = Modifier.size(sideSize),
        ) {
            Icon(
                painterResource(Res.drawable.ic_forward_5),
                contentDescription = null,
                modifier = Modifier.size(sideIconSize),
                tint = PlayerContentColor.alpha(.7f),
            )
        }

        IconButton(
            onClick = { controller.nextVerse() },
            modifier = Modifier.size(sideSize),
        ) {
            Icon(
                painterResource(Res.drawable.ic_skip_forward),
                contentDescription = stringResource(Res.string.strLabelNextVerse),
                modifier = Modifier.size(sideIconSize),
                tint = PlayerContentColor.alpha(.7f),
            )
        }
    }
}
