package com.quranapp.android.compose.components.player


import android.app.Activity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.quranapp.android.R
import com.quranapp.android.compose.components.player.dialogs.AudioOption
import com.quranapp.android.compose.components.player.dialogs.AudioOptionsSheet
import com.quranapp.android.compose.components.player.dialogs.PlaybackSpeedSheet
import com.quranapp.android.compose.components.player.dialogs.ReciterSelectorSheet
import com.quranapp.android.compose.components.player.dialogs.RepeatOptionsSheet
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.compose.utils.appLocale
import com.quranapp.android.compose.utils.preferences.RecitationPreferences
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.mediaplayer.RecitationController
import com.quranapp.android.utils.mediaplayer.RecitationModelManager
import com.quranapp.android.utils.mediaplayer.RecitationServiceState
import com.quranapp.android.utils.univ.formatDuration
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
) {
    val verse = state.currentVerse
    val settings = state.settings

    var mode by remember { mutableStateOf(ExpandedPlayerMode.Controls) }

    val pagerState = rememberPagerState(
        initialPage = mode.ordinal,
        pageCount = { ExpandedPlayerMode.entries.size }
    )
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
                            val isWideLayout = maxWidth >= 600.dp && maxWidth > maxHeight

                            Log.d(maxWidth, this.constraints)

                            if (isWideLayout) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                    ) {
                                        ExtendedThumbnail(
                                            verse = verse,
                                            modifier = Modifier
                                                .weight(0.4f)
                                                .fillMaxHeight()
                                                .padding(16.dp),
                                        )

                                        Column(
                                            modifier = Modifier
                                                .weight(0.6f)
                                                .fillMaxHeight(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxWidth(),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                PlayerMessages(
                                                    state,
                                                    isPlaying,
                                                )
                                            }

                                            Configurations(
                                                controller = controller
                                            )

                                            ExpandedTransportControls(
                                                isPlaying = isPlaying,
                                                isLoading = isLoading,
                                                controller = controller,
                                                continueRange = settings.continueRange,
                                                bottomPadding = 8.dp,
                                            )
                                        }
                                    }

                                    ProgressSeekBar(
                                        isPlaying = isPlaying,
                                        isLoading = isLoading,
                                        controller = controller,
                                    )

                                    Spacer(Modifier.height(12.dp))
                                }
                            } else {
                                val thumbnailHeight: Dp =
                                    (maxHeight * 0.32f).coerceIn(150.dp, 340.dp)

                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Box(
                                        Modifier.weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        ExtendedThumbnail(
                                            verse = verse,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    start = 16.dp,
                                                    end = 16.dp,
                                                    bottom = 16.dp
                                                )
                                                .height(thumbnailHeight),
                                        )
                                    }

                                    PlayerMessages(
                                        state,
                                        isPlaying,
                                    )

                                    Configurations(
                                        controller = controller
                                    )

                                    ProgressSeekBar(
                                        isPlaying = isPlaying,
                                        isLoading = isLoading,
                                        controller = controller,
                                    )

                                    Spacer(Modifier.height(20.dp))

                                    ExpandedTransportControls(
                                        isPlaying = isPlaying,
                                        isLoading = isLoading,
                                        controller = controller,
                                        continueRange = settings.continueRange,
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
    onCollapse: () -> Unit
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
                painterResource(R.drawable.dr_icon_chevron_down),
                contentDescription = "Collapse",
                modifier = Modifier.size(26.dp),
                tint = PlayerContentColor,
            )
        }

        ModeTabs(
            selected = mode,
            onSelect = onModeChange,
        )
    }
}


@Composable
private fun RowScope.ModeTabs(
    selected: ExpandedPlayerMode,
    onSelect: (ExpandedPlayerMode) -> Unit,
) {
    Box(
        Modifier.weight(1f),
        contentAlignment = Alignment.CenterEnd
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
                                ExpandedPlayerMode.Controls -> R.string.expandedPlayerModeControls
                                ExpandedPlayerMode.Spotlight -> R.string.expandedPlayerModeSpotlight
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
private fun Configurations(
    controller: RecitationController,
) {
    val context = LocalContext.current
    val audioOption = RecitationPreferences.observeAudioOption();
    val speed = RecitationPreferences.observeSpeed();
    val repeatCount = RecitationPreferences.observeRepeatCount()
    val reciterNames =
        RecitationModelManager.get(context).rememberCurrentReciterNameForAudioOption()

    val audioOptionTextId = when (audioOption) {
        AudioOption.ONLY_TRANSLATION -> R.string.audioOnlyTranslation
        AudioOption.BOTH -> R.string.audioBothArabicTranslation
        else -> R.string.audioOnlyArabic
    }

    val repeatSupported = audioOption == AudioOption.ONLY_QURAN

    var showReciterSelector by rememberSaveable { mutableStateOf(false) }
    var showAudioOptions by rememberSaveable { mutableStateOf(false) }
    var showRepeatOptions by rememberSaveable { mutableStateOf(false) }
    var showPlaybackSpeedOptions by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        ) {
            PlayerConfigButton(
                text = stringResource(R.string.strTitleSelectReciter),
                icon = painterResource(R.drawable.ic_mic),
                subtext = reciterNames,
                onClick = {
                    showReciterSelector = true
                },
                modifier = Modifier.weight(1f),
            )

            PlayerConfigButton(
                text = stringResource(R.string.audioOption),
                subtext = stringResource(audioOptionTextId),
                icon = painterResource(R.drawable.dr_icon_settings),
                onClick = {
                    showAudioOptions = true
                },
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        ) {
            PlayerConfigButton(
                text = stringResource(R.string.playbackCount),
                icon = painterResource(R.drawable.ic_repeat),
                subtext = when {
                    !repeatSupported -> stringResource(R.string.notSupported)
                    repeatCount == 0 -> stringResource(R.string.once)
                    repeatCount == 1 -> stringResource(R.string.twice)
                    else -> stringResource(R.string.nTimes, repeatCount + 1)
                },
                onClick = {
                    showRepeatOptions = true
                },
                modifier = Modifier.weight(1f),
            )

            PlayerConfigButton(
                text = stringResource(R.string.playbackSpeed),
                subtext = String.format(appLocale(), "%.1fx", speed),
                icon = painterResource(R.drawable.icon_playback_speed),
                onClick = {
                    showPlaybackSpeedOptions = true
                },
                modifier = Modifier.weight(1f),
            )
        }
    }

    ReciterSelectorSheet(
        controller = controller,
        isOpen = showReciterSelector,
    ) {
        showReciterSelector = false
    }

    AudioOptionsSheet(
        controller = controller,
        isOpen = showAudioOptions,
    ) {
        showAudioOptions = false
    }

    RepeatOptionsSheet(
        controller = controller,
        isOpen = showRepeatOptions,
    ) {
        showRepeatOptions = false
    }

    PlaybackSpeedSheet(
        controller = controller,
        isOpen = showPlaybackSpeedOptions,
    ) {
        showPlaybackSpeedOptions = false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgressSeekBar(
    isPlaying: Boolean,
    isLoading: Boolean,
    controller: RecitationController,
) {
    var positionMs by remember { mutableLongStateOf(controller.currentPositionMs) }
    var durationMs by remember { mutableLongStateOf(controller.durationMs) }

    LaunchedEffect(isPlaying) {
        positionMs = controller.currentPositionMs
        durationMs = controller.durationMs
        while (isPlaying && isActive) {
            positionMs = controller.currentPositionMs
            durationMs = controller.durationMs
            delay(100)
        }

        if (!isPlaying) {
            positionMs = controller.currentPositionMs
            durationMs = controller.durationMs
        }
    }

    val progress = if (durationMs > 0) positionMs.toFloat() / durationMs.toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress, animationSpec = tween(300), label = ""
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Slider(
            value = animatedProgress,
            onValueChange = {
                val seekPosition = (it * durationMs).toLong()
                controller.seekTo(seekPosition)
                positionMs = seekPosition
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
                text = formatDuration(positionMs),
                style = MaterialTheme.typography.labelMedium,
                color = PlayerContentColor.alpha(0.7f),
            )
            Text(
                text = formatDuration(durationMs),
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
    continueRange: Boolean,
    bottomPadding: Dp = 48.dp,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, bottom = bottomPadding),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { controller.previousVerse() },
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                painterResource(R.drawable.ic_skip_back),
                contentDescription = stringResource(R.string.strLabelPreviousVerse),
                modifier = Modifier.size(30.dp),
                tint = PlayerContentColor.alpha(.7f),
            )
        }

        IconButton(
            onClick = {
                controller.seekLeft()
            },
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                painterResource(R.drawable.ic_replay_5),
                contentDescription = null,
                modifier = Modifier.size(30.dp),
                tint = PlayerContentColor.alpha(.7f),
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(74.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = CircleShape,
                    ambientColor = colorScheme.primary.copy(alpha = 0.35f),
                    spotColor = colorScheme.primary.copy(alpha = 0.45f),
                )
                .clip(CircleShape)
                .background(colorScheme.primary)
                .clickable(
                    onClick = {
                        controller.playPause()
                    },
                ),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(44.dp),
                    strokeWidth = 3.dp,
                    color = colorScheme.onPrimary,
                )
            } else {
                Icon(
                    painterResource(
                        if (isPlaying) R.drawable.ic_pause
                        else R.drawable.ic_play,
                    ),
                    contentDescription = if (isPlaying) stringResource(R.string.strLabelPause)
                    else stringResource(R.string.strLabelPlay),
                    modifier = Modifier.size(36.dp),
                    tint = colorScheme.onPrimary,
                )
            }
        }

        IconButton(
            onClick = { controller.seekRight() },
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                painterResource(R.drawable.ic_forward_5),
                contentDescription = null,
                modifier = Modifier.size(30.dp),
                tint = PlayerContentColor.alpha(.7f),
            )
        }

        IconButton(
            onClick = { controller.nextVerse() },
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                painterResource(R.drawable.ic_skip_forward),
                contentDescription = stringResource(R.string.strLabelNextVerse),
                modifier = Modifier.size(30.dp),
                tint = PlayerContentColor.alpha(.7f),
            )
        }
    }
}