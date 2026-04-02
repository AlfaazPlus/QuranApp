package com.quranapp.android.compose.components.player

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quranapp.android.utils.mediaplayer.RecitationController
import com.quranapp.android.utils.mediaplayer.RecitationServiceState
import com.quranapp.android.utils.univ.ErrorEvent
import com.quranapp.android.utils.univ.EventBus
import com.quranapp.android.utils.univ.MessageEvent
import com.quranapp.android.utils.univ.MessageUtils
import com.quranapp.android.viewModels.RecitationPlayerViewModel

private const val MINI_PLAYER_HEIGHT_DP = 72
private val SPEED_OPTIONS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

private val PlayerGradientTop = Color(0xFF25243A)
private val PlayerGradientBottom = Color(0xFF060608)
private val PlayerAccentOrange = Color(0xFFFF5F1F)
private val PlayerOnDark = Color.White
private val PlayerMutedOnDark = Color(0xFFB8B8C8)
private val PlayerSubtitleBlue = Color(0xFF7EC8E3)
private val PlayerBottomBarBg = Color(0xFF0C0C10)
private val PlayerHeartSyncOn = Color(0xFF4DD0E1)

@Composable
fun RecitationPlayerSheet(
    modifier: Modifier = Modifier,
) {
    val viewModel = viewModel<RecitationPlayerViewModel>()
    val context = LocalContext.current

    val state by viewModel.state.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isVisible = isPlaying || isLoading || state.currentVerse.isValid

    var expanded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        EventBus.events.collect {
            when (it) {
                is ErrorEvent -> {
                    MessageUtils.showRemovableToast(context, it.message, Toast.LENGTH_LONG)
                }

                is MessageEvent -> {
                    MessageUtils.showRemovableToast(context, it.message, Toast.LENGTH_LONG)
                }
            }
        }
    }

    LaunchedEffect(isVisible) {
        if (!isVisible) expanded = false
    }

    BackHandler(enabled = expanded) {
        expanded = false
    }

    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val miniPlayerTotalHeight = MINI_PLAYER_HEIGHT_DP.dp + navBarBottom

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val fullHeight = maxHeight

        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            PlayerContainer(
                expanded = expanded,
                fullHeight = fullHeight,
                miniPlayerTotalHeight = miniPlayerTotalHeight,
                state = state,
                isPlaying = isPlaying,
                isLoading = isLoading,
                controller = viewModel.controller,
                onExpand = { expanded = true },
                onCollapse = { expanded = false },
            )
        }
    }
}

@Composable
private fun PlayerContainer(
    expanded: Boolean,
    fullHeight: Dp,
    miniPlayerTotalHeight: Dp,
    state: RecitationServiceState,
    isPlaying: Boolean,
    isLoading: Boolean,
    controller: RecitationController,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
) {
    val targetHeight = if (expanded) fullHeight else miniPlayerTotalHeight
    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow),
        label = "playerHeight",
    )

    val targetCorner = if (expanded) 0f else 16f
    val animatedCorner by animateFloatAsState(
        targetValue = targetCorner,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow),
        label = "cornerRadius",
    )

    val clampedCorner = animatedCorner.coerceAtLeast(0f)
    val shape = RoundedCornerShape(topStart = clampedCorner.dp, topEnd = clampedCorner.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(animatedHeight)
            .shadow(elevation = if (expanded) 0.dp else 8.dp, shape = shape),
        shape = shape,
        color = if (expanded) Color.Transparent else MaterialTheme.colorScheme.surface,
        tonalElevation = if (expanded) 0.dp else 2.dp,
    ) {
        AnimatedContent(
            targetState = expanded,
            transitionSpec = {
                fadeIn(tween(350, delayMillis = 100)) togetherWith fadeOut(tween(250))
            },
            label = "playerContent",
        ) { isExpanded ->
            if (isExpanded) {
                ExpandedPlayer(
                    state = state,
                    isPlaying = isPlaying,
                    isLoading = isLoading,
                    controller = controller,
                    onCollapse = onCollapse,
                )
            } else {
                MiniPlayer(
                    state = state,
                    isPlaying = isPlaying,
                    isLoading = isLoading,
                    controller = controller,
                    onExpand = onExpand,
                    modifier = Modifier.navigationBarsPadding(),
                )
            }
        }
    }
}
