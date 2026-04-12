package com.quranapp.android.compose.components.player


import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.compose.components.dialogs.SimpleTooltip
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.utils.mediaplayer.RecitationController
import com.quranapp.android.utils.mediaplayer.RecitationModelManager
import com.quranapp.android.utils.mediaplayer.RecitationServiceState
import com.quranapp.android.utils.univ.MessageUtils
import com.quranapp.android.utils.univ.formatDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

@Composable
fun MiniPlayer(
    controller: RecitationController,
    state: RecitationServiceState,
    isPlaying: Boolean,
    isLoading: Boolean,
    isSyncing: Boolean,
    onSyncRequest: (() -> Unit)?,
    onExpand: () -> Unit,
) {
    val context = LocalContext.current

    val verse = state.currentVerse
    val repository = remember(context) { DatabaseProvider.getQuranRepository(context) }
    var chapterName by remember { mutableStateOf("") }

    LaunchedEffect(verse.chapterNo) {
        chapterName = withContext(Dispatchers.IO) {
            repository.getChapterName(verse.chapterNo)
        }
    }

    val reciterNames = RecitationModelManager.get(context)
        .rememberCurrentReciterNameForAudioOption()
    val (positionMs, durationMs) = rememberTimestamp(isPlaying, controller)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlayerBgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onExpand,
            ),
    ) {
        MiniProgressBar(
            positionMs = positionMs,
            durationMs = durationMs,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(start = 16.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MiniPlayerThumbnail(verse = verse)

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        R.string.strLabelVerseSerialWithChapter,
                        chapterName,
                        verse.chapterNo,
                        verse.verseNo
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = PlayerContentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = reciterNames,
                    style = MaterialTheme.typography.bodySmall,
                    color = PlayerContentColor.alpha(0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (onSyncRequest != null) {
                SimpleTooltip(
                    text = if (isSyncing) stringResource(R.string.verseSyncOn) else stringResource(R.string.verseSyncOff)
                ) {
                    IconButton(
                        onClick = {
                            onSyncRequest()

                            MessageUtils.showRemovableToast(
                                context,
                                if (isSyncing) R.string.verseSyncOn else R.string.verseSyncOff,
                                Toast.LENGTH_SHORT
                            )
                        },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            painterResource(if (isSyncing) R.drawable.ic_lock_keyhole_closed else R.drawable.ic_lock_open),
                            contentDescription = stringResource(
                                if (isSyncing) R.string.verseSyncOn else
                                    R.string.verseSyncOff
                            ),
                            modifier = Modifier.size(20.dp),
                            tint = if (isSyncing) colorScheme.primary else PlayerContentColor.alpha(
                                .7f
                            ),
                        )
                    }
                }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
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
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
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
                        modifier = Modifier.size(24.dp),
                        tint = colorScheme.onPrimary,
                    )
                }
            }

            Timestamp(
                positionMs = positionMs,
                durationMs = durationMs,
            )
        }
    }
}


@Composable
private fun MiniProgressBar(
    positionMs: Long,
    durationMs: Long,
) {
    val progress = if (durationMs > 0) positionMs.toFloat() / durationMs.toFloat() else 0f

    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp),
        color = colorScheme.primary,
        trackColor = colorScheme.primary.copy(alpha = 0.15f),
    )
}

@Composable
fun Timestamp(
    positionMs: Long,
    durationMs: Long,
) {
    val current = formatDuration(positionMs)
    val total = formatDuration(durationMs)

    BoxWithConstraints(
        modifier = Modifier.padding(start = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        val isCompact = maxWidth < 600.dp

        if (isCompact) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.wrapContentHeight()
            ) {
                Text(
                    text = current,
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )

                Text(
                    text = total,
                    color = Color.White.copy(alpha = 0.60f),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.wrapContentWidth()
            ) {
                Text(
                    text = current,
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )

                Text(
                    text = " / ",
                    color = Color.White.copy(alpha = 0.45f),
                    style = MaterialTheme.typography.labelSmall
                )

                Text(
                    text = total,
                    color = Color.White.copy(alpha = 0.60f),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun rememberTimestamp(
    isPlaying: Boolean,
    controller: RecitationController,
): Pair<Long, Long> {
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

    return positionMs to durationMs
}