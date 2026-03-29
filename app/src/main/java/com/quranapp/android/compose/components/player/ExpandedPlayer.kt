package com.quranapp.android.compose.components.player


import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranapp.android.R
import com.quranapp.android.components.quran.QuranMeta2
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.compose.components.ChapterIcon
import com.quranapp.android.compose.components.dialogs.BottomSheet
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.utils.mediaplayer.RecitationController
import com.quranapp.android.utils.mediaplayer.RecitationServiceState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val PlayerBgColor = Color(0xFF14141C)
private val PlayerContentColor = Color.White

@Composable
fun ExpandedPlayer(
    state: RecitationServiceState,
    isPlaying: Boolean,
    isLoading: Boolean,
    controller: RecitationController,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val verse = state.currentVerse
    val settings = state.settings

    var showReciterSheet by remember { mutableStateOf(false) }
    var overflowOpen by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = PlayerBgColor,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(
                        painterResource(R.drawable.dr_icon_chevron_down),
                        contentDescription = "Collapse",
                        modifier = Modifier.size(26.dp),
                        tint = PlayerContentColor,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                /*IconButton(onClick = { showReciterSheet = true }) {
                    Icon(
                        painterResource(R.drawable.dr_icon_translations),
                        contentDescription = "Reciter and details",
                        modifier = Modifier.size(24.dp),
                        tint = PlayerContentColor,
                    )
                }*/
            }
        }
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Header(
                verse = verse,
            )

            TrackInfoRow(
                chapterName = "…",
                verseLabel = if (verse.isValid) "Verse ${verse.verseNo}" else null,
                reciter = settings.reciter,
                verseSyncOn = settings.verseSync,
                onVerseSyncClick = { controller.setVerseSync(!settings.verseSync) },
                onOverflowClick = { overflowOpen = true },
                overflowOpen = overflowOpen,
                onDismissOverflow = { overflowOpen = false },
                state = state,
                controller = controller,
            )

            Spacer(Modifier.height(20.dp))

            WaveformSeekBar(
                isPlaying = isPlaying,
                isLoading = isLoading,
                controller = controller,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            )

            Spacer(Modifier.height(20.dp))

            ExpandedTransportControls(
                isPlaying = isPlaying,
                isLoading = isLoading,
                controller = controller,
                continueRange = settings.continueRange,
                repeatVerse = settings.repeatVerse,
            )

            Spacer(Modifier.weight(1f, fill = true))
        }
    }

    ReciterBottomSheet(
        isOpen = showReciterSheet,
        reciterName = state.settings.reciter,
        onDismiss = { showReciterSheet = false },
    )
}

@Composable
private fun Header(
    verse: ChapterVersePair,
) {
    val context = LocalContext.current
    val quranMeta = QuranMeta2.rememberQuranMeta()
    val headerShape = RoundedCornerShape(28.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            .clip(headerShape),
    ) {
        Image(
            painter = painterResource(R.drawable.dr_quran_wallpaper),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(colorScheme.primary.copy(alpha = 0.4f)),
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = 0.6f)),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ChapterIcon(
                chapterNo = verse.chapterNo,
                fontSize = 72.sp,
                color = PlayerContentColor,
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(
                    R.string.strLabelVerseWithChapNameWithColon,
                    quranMeta?.getChapterName(context, verse.chapterNo) ?: "…",
                    verse.verseNo
                ),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = PlayerContentColor,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TrackInfoRow(
    chapterName: String,
    verseLabel: String?,
    reciter: String?,
    verseSyncOn: Boolean,
    onVerseSyncClick: () -> Unit,
    onOverflowClick: () -> Unit,
    overflowOpen: Boolean,
    onDismissOverflow: () -> Unit,
    state: RecitationServiceState,
    controller: RecitationController,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onVerseSyncClick) {
            Icon(
                painterResource(
                    if (verseSyncOn) R.drawable.dr_icon_heart_filled
                    else R.drawable.icon_heart_outlined,
                ),
                contentDescription = "Reader scroll sync",
                modifier = Modifier.size(26.dp),
                tint = if (verseSyncOn) colorScheme.primary else PlayerContentColor,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = chapterName.ifBlank { "…" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = PlayerContentColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = buildString {
                if (!verseLabel.isNullOrBlank()) append(verseLabel)
                if (!verseLabel.isNullOrBlank() && !reciter.isNullOrBlank()) append(" · ")
                if (!reciter.isNullOrBlank()) append(reciter)
            }
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PlayerContentColor.alpha(0.7f),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun WaveformSeekBar(
    isPlaying: Boolean,
    isLoading: Boolean,
    controller: RecitationController,
    modifier: Modifier = Modifier,
    barCount: Int = 44,
) {
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isPlaying) {
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

    val progress =
        if (durationMs > 0) positionMs.toFloat() / durationMs.toFloat() else 0f

    val barHeights = remember(barCount) {
        List(barCount) { i ->
            val t = (kotlin.math.sin(i * 0.55) * 0.5 + 0.5).toFloat()
            0.22f + t * 0.75f
        }
    }

    Column(modifier = modifier) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .pointerInput(durationMs, isLoading) {
                    if (!isLoading && durationMs > 0L) {
                        detectTapGestures { offset ->
                            val f = (offset.x / size.width).coerceIn(0f, 1f)
                            controller.seekTo((f * durationMs).toLong())
                        }
                    }
                },
        ) {
            val maxH = maxHeight
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                repeat(barCount) { i ->
                    val lit = (i + 0.5f) / barCount <= progress
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(maxH * barHeights[i])
                                .background(
                                    color = if (lit) {
                                        PlayerContentColor
                                    } else {
                                        PlayerContentColor.copy(alpha = 0.28f)
                                    },
                                    shape = RoundedCornerShape(2.dp),
                                ),
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatMs(positionMs),
                style = MaterialTheme.typography.labelMedium,
                color = PlayerContentColor.alpha(0.7f),
            )
            Text(
                text = formatMs(durationMs),
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
    repeatVerse: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { controller.setContinue(!continueRange) },
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                painterResource(R.drawable.dr_icon_shuffle),
                contentDescription = "Continue after verse",
                modifier = Modifier.size(24.dp),
                tint = if (continueRange) colorScheme.primary else PlayerContentColor.alpha(.7f),
            )
        }

        IconButton(
            onClick = { controller.previousVerse() },
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                painterResource(R.drawable.dr_icon_player_seek_left),
                contentDescription = "Previous verse",
                modifier = Modifier.size(30.dp),
                tint = PlayerContentColor.alpha(.7f),
            )
        }

        AnimatedContent(
            targetState = isLoading,
            label = "playPauseContent",
        ) { loading ->
            if (loading) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(76.dp),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(44.dp),
                        strokeWidth = 3.dp,
                        color = colorScheme.primary,
                    )
                }
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(76.dp)
                        .shadow(
                            elevation = 12.dp,
                            shape = CircleShape,
                            ambientColor = colorScheme.primary.copy(alpha = 0.35f),
                            spotColor = colorScheme.primary.copy(alpha = 0.45f),
                        )
                        .clip(CircleShape)
                        .background(colorScheme.primary)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { controller.playPause() },
                        ),
                ) {
                    Icon(
                        painterResource(
                            if (isPlaying) R.drawable.dr_icon_pause_verse
                            else R.drawable.dr_icon_play_verse,
                        ),
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(36.dp),
                        tint = PlayerContentColor.alpha(.7f),
                    )
                }
            }
        }

        IconButton(
            onClick = { controller.nextVerse() },
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                painterResource(R.drawable.dr_icon_player_seek_right),
                contentDescription = "Next verse",
                modifier = Modifier.size(30.dp),
                tint = PlayerContentColor.alpha(.7f),
            )
        }

        IconButton(
            onClick = { controller.setRepeat(!repeatVerse) },
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                painterResource(R.drawable.dr_icon_player_repeat),
                contentDescription = "Repeat verse",
                modifier = Modifier.size(24.dp),
                tint = if (repeatVerse) colorScheme.primary else PlayerContentColor.alpha(.7f),
            )
        }
    }
}


@Composable
private fun ReciterBottomSheet(
    isOpen: Boolean,
    reciterName: String?,
    onDismiss: () -> Unit,
) {
    BottomSheet(
        isOpen = isOpen,
        onDismiss = onDismiss,
        title = "Reciter",
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            if (!reciterName.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                        ) {
                            Text(
                                text = reciterName.first().uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Column {
                            Text(
                                text = reciterName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                text = "Currently selected",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                    alpha = 0.7f
                                ),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "To change the reciter, go to Settings > Recitation.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp),
            )
        }
    }
}


private fun formatMs(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
