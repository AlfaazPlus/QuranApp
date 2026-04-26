package com.quranapp.android.compose.components.player

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import kotlinx.coroutines.launch

val MINI_PLAYER_HEIGHT = 72.dp
private val SPEED_OPTIONS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

private val PlayerMotionSpring = spring<Dp>(
    dampingRatio = 0.82f,
    stiffness = Spring.StiffnessLow,
)

@Composable
fun RecitationPlayerSheet(
    modifier: Modifier = Modifier,
    collapsedBottomInset: Dp = 0.dp,
    barsCollapsedFraction: Float = 0f,
    showPlayer: Boolean = true,
    isSyncing: Boolean = false,
    onSyncRequest: (() -> Unit)? = null,
) {
    val viewModel = viewModel<RecitationPlayerViewModel>()
    val context = LocalContext.current

    val state by viewModel.state.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isVisible = showPlayer && (isPlaying || isLoading || state.currentVerse.isValid)

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

    LaunchedEffect(showPlayer) {
        if (!showPlayer) expanded = false
    }

    BackHandler(enabled = expanded && showPlayer) {
        expanded = false
    }

    val miniPlayerTotalHeight = MINI_PLAYER_HEIGHT

    val targetBottomInset = if (expanded) 0.dp else collapsedBottomInset
    val animatedBottomInset by animateDpAsState(
        targetValue = targetBottomInset,
        animationSpec = PlayerMotionSpring,
        label = "playerBottomInset",
    )

    val hideOnScrollOffset =
        if (expanded) {
            0.dp
        } else {
            (MINI_PLAYER_HEIGHT + collapsedBottomInset) * barsCollapsedFraction.coerceIn(
                0f,
                1f
            )
        }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val fullHeight = maxHeight

        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = animatedBottomInset.coerceAtLeast(0.dp))
                .offset(y = hideOnScrollOffset),
        ) {
            PlayerContainer(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                fullHeight = fullHeight,
                miniPlayerTotalHeight = miniPlayerTotalHeight,
                controller = viewModel.controller,
                state = state,
                isPlaying = isPlaying,
                isLoading = isLoading,
                isSyncing = isSyncing,
                onSyncRequest = onSyncRequest,
            )
        }
    }
}

@Composable
private fun PlayerContainer(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    fullHeight: Dp,
    miniPlayerTotalHeight: Dp,
    controller: RecitationController,
    state: RecitationServiceState,
    isPlaying: Boolean,
    isLoading: Boolean,
    isSyncing: Boolean,
    onSyncRequest: (() -> Unit)?,
) {
    val density = LocalDensity.current
    val targetFraction = if (expanded) 1f else 0f

    val fractionAnim = remember { Animatable(targetFraction) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(expanded) {
        if (!isDragging && fractionAnim.targetValue != targetFraction) {
            fractionAnim.animateTo(
                targetValue = targetFraction,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow)
            )
        }
    }

    val fraction = fractionAnim.value

    val animatedHeight = miniPlayerTotalHeight + (fullHeight - miniPlayerTotalHeight) * fraction
    val clampedCorner = (16f * (1f - fraction)).coerceAtLeast(0f)
    val shape = RoundedCornerShape(topStart = clampedCorner.dp, topEnd = clampedCorner.dp)

    val surfaceColor = if (fraction > 0.99f) Color.Transparent else Color.Black
    val elevation = if (fraction > 0.99f) 0.dp else 2.dp

    val maxDragDistPx = remember(fullHeight, miniPlayerTotalHeight, density) {
        with(density) { (fullHeight - miniPlayerTotalHeight).coerceAtLeast(1.dp).toPx() }
    }

    val coroutineScope = rememberCoroutineScope()
    val draggableState = rememberDraggableState { delta ->
        val fractionChange = -(delta / maxDragDistPx)
        coroutineScope.launch {
            fractionAnim.snapTo((fractionAnim.value + fractionChange).coerceIn(0f, 1f))
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(animatedHeight)
            .draggable(
                state = draggableState,
                orientation = Orientation.Vertical,
                onDragStarted = { isDragging = true },
                onDragStopped = { velocity ->
                    isDragging = false
                    val isSwipingDown = velocity > 500f
                    val isSwipingUp = velocity < -500f
                    val target = when {
                        isSwipingDown -> 0f
                        isSwipingUp -> 1f
                        fractionAnim.value > 0.5f -> 1f
                        else -> 0f
                    }

                    val newExpanded = target == 1f

                    onExpandedChange(newExpanded)

                    if (expanded == newExpanded) {
                        coroutineScope.launch {
                            fractionAnim.animateTo(
                                targetValue = target,
                                initialVelocity = -(velocity / maxDragDistPx),
                                animationSpec = spring(
                                    dampingRatio = 0.82f,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                        }
                    }
                }
            ),
        shape = shape,
        color = surfaceColor,
        tonalElevation = elevation,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            val expandedAlpha = ((fraction - 0.2f) * 1.25f).coerceIn(0f, 1f)

            if (expandedAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = expandedAlpha },
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(Alignment.Top, unbounded = true)
                            .height(fullHeight)
                    ) {
                        ExpandedPlayer(
                            state = state,
                            isPlaying = isPlaying,
                            isLoading = isLoading,
                            controller = controller,
                            onCollapse = { onExpandedChange(false) },
                        )
                    }
                }
            }

            val miniAlpha = ((0.8f - fraction) * 1.25f).coerceIn(0f, 1f)
            if (miniAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = miniAlpha },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(miniPlayerTotalHeight)
                    ) {
                        MiniPlayer(
                            state = state,
                            controller = controller,
                            isPlaying = isPlaying,
                            isLoading = isLoading,
                            isSyncing = isSyncing,
                            onSyncRequest = onSyncRequest,
                            onExpand = { onExpandedChange(true) },
                        )
                    }
                }
            }
        }
    }
}
