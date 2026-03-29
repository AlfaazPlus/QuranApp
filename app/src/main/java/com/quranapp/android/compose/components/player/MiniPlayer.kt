package com.quranapp.android.compose.components.player


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.components.quran.QuranMeta2
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.utils.mediaplayer.RecitationController
import com.quranapp.android.utils.mediaplayer.RecitationServiceState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val MinPlayerBgColor = Color(0xFF14141C)
private val MinPlayerContentColor = Color.White

@Composable
fun MiniPlayer(
    modifier: Modifier = Modifier,
    state: RecitationServiceState,
    isPlaying: Boolean,
    isLoading: Boolean,
    controller: RecitationController,
    onExpand: () -> Unit,
) {
    val context = LocalContext.current
    val quranMeta = QuranMeta2.rememberQuranMeta()

    val verse = state.currentVerse
    val chapterName = quranMeta?.getChapterName(context, verse.chapterNo) ?: "…"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MinPlayerBgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onExpand,
            ),
    ) {
        MiniProgressBar(
            isPlaying = isPlaying,
            isLoading = isLoading,
            controller = controller,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (verse.isValid) "$chapterName ${verse.chapterNo}:${verse.verseNo}"
                    else "Loading...",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MinPlayerContentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                val reciter = state.settings.reciter
                if (!reciter.isNullOrBlank()) {
                    Text(
                        text = reciter,
                        style = MaterialTheme.typography.bodySmall,
                        color = MinPlayerContentColor.alpha(0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            IconButton(onClick = { controller.playPause() }) {
                Icon(
                    painterResource(
                        if (isPlaying) R.drawable.dr_icon_pause_verse
                        else R.drawable.dr_icon_play_verse,
                    ),
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(28.dp),
                    tint = colorScheme.primary,
                )
            }

            IconButton(onClick = { controller.stop() }) {
                Icon(
                    painterResource(R.drawable.dr_icon_close),
                    contentDescription = "Close",
                    modifier = Modifier.size(20.dp),
                    tint = MinPlayerContentColor.alpha(0.7f),
                )
            }
        }
    }
}

@Composable
private fun MiniProgressBar(
    isPlaying: Boolean,
    isLoading: Boolean,
    controller: RecitationController,
) {
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isPlaying) {
        while (isPlaying && isActive) {
            positionMs = controller.currentPositionMs
            durationMs = controller.durationMs
            delay(250)
        }
        if (!isPlaying) {
            positionMs = controller.currentPositionMs
            durationMs = controller.durationMs
        }
    }

    val progress = if (durationMs > 0) positionMs.toFloat() / durationMs.toFloat() else 0f

    if (isLoading) {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp),
            color = colorScheme.primary,
            trackColor = colorScheme.primary.copy(alpha = 0.15f),
        )
    } else {
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp),
            color = colorScheme.primary,
            trackColor = colorScheme.primary.copy(alpha = 0.15f),
        )
    }
}