package com.quranapp.android.compose.screens.recitation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.utils.mediaplayer.PlaybackMode
import com.quranapp.android.utils.mediaplayer.RecitationController
import com.quranapp.android.utils.mediaplayer.RecitationServiceState

private data class ChapterEntry(val number: Int, val name: String, val verseCount: Int)

private val DEMO_CHAPTERS = listOf(
    ChapterEntry(1, "Al-Fatihah", 7),
    ChapterEntry(2, "Al-Baqarah", 286),
    ChapterEntry(36, "Ya-Sin", 83),
    ChapterEntry(55, "Ar-Rahman", 78),
    ChapterEntry(67, "Al-Mulk", 30),
    ChapterEntry(78, "An-Naba", 40),
    ChapterEntry(112, "Al-Ikhlas", 4),
    ChapterEntry(113, "Al-Falaq", 5),
    ChapterEntry(114, "An-Nas", 6),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecitationDemoScreen(controller: RecitationController) {
    val context = LocalContext.current
    val state by controller.state.collectAsState()

    Scaffold(
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
        topBar = {
            TopAppBar(
                title = { Text("Recitation Player") },
                navigationIcon = {
                    val activity = context as? android.app.Activity
                    IconButton(onClick = { activity?.finish() }) {
                        Icon(
                            painterResource(R.drawable.dr_icon_arrow_left),
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (state.isPlaying || state.isLoading || state.currentVerse.isValid) {
                NowPlayingPanel(
                    state = state,
                    onPlayPause = { controller.playPause() },
                    onStop = { controller.stop() },
                    onPrevious = { controller.previousVerse() },
                    onNext = { controller.nextVerse() },
                    onSeek = { controller.seekTo(it) },
                    onSeekLeft = { controller.seekLeft() },
                    onSeekRight = { controller.seekRight() },
                    onToggleRepeat = { controller.setRepeat(!state.settings.repeatVerse) },
                )
            }

            ChapterList(
                state = state,
                onChapterSelected = { chapter ->
                    controller.play(chapterNo = chapter.number, verseNo = 1)
                },
            )
        }
    }
}

@Composable
private fun NowPlayingPanel(
    state: RecitationServiceState,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekLeft: () -> Unit,
    onSeekRight: () -> Unit,
    onToggleRepeat: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val verse = state.currentVerse
            val modeLabel = when (state.playbackMode) {
                PlaybackMode.FULL_CHAPTER -> "Chapter"
                PlaybackMode.VERSE_BY_VERSE -> "Verse"
            }

            Text(
                text = if (verse.isValid) "$modeLabel ${verse.chapterNo}:${verse.verseNo}" else "Loading...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )

            Spacer(Modifier.height(4.dp))

            val reciter = state.settings.reciter ?: "Unknown"
            Text(
                text = reciter,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            )

            Spacer(Modifier.height(12.dp))

            ProgressBar(state = state, onSeek = onSeek)

            Spacer(Modifier.height(8.dp))

            TransportControls(
                state = state,
                onPlayPause = onPlayPause,
                onStop = onStop,
                onPrevious = onPrevious,
                onNext = onNext,
                onSeekLeft = onSeekLeft,
                onSeekRight = onSeekRight,
                onToggleRepeat = onToggleRepeat,
            )
        }
    }
}

@Composable
private fun ProgressBar(
    state: RecitationServiceState,
    onSeek: (Long) -> Unit,
) {
    var isSeeking by remember { mutableStateOf(false) }
    var seekValue by remember { mutableStateOf(0f) }

    val position = state.positionMs.toFloat()
    val duration = state.durationMs.toFloat().coerceAtLeast(1f)

    Column(modifier = Modifier.fillMaxWidth()) {
        if (state.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else {
            Slider(
                value = if (isSeeking) seekValue else position / duration,
                onValueChange = { v ->
                    isSeeking = true
                    seekValue = v
                },
                onValueChangeFinished = {
                    isSeeking = false
                    onSeek((seekValue * duration).toLong())
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatMs(state.positionMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
            )
            Text(
                text = formatMs(state.durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun TransportControls(
    state: RecitationServiceState,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeekLeft: () -> Unit,
    onSeekRight: () -> Unit,
    onToggleRepeat: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        IconButton(onClick = onToggleRepeat) {
            Icon(
                painterResource(R.drawable.dr_icon_player_repeat),
                contentDescription = "Repeat",
                tint = if (state.settings.repeatVerse) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
            )
        }

        IconButton(onClick = onPrevious) {
            Icon(
                painterResource(R.drawable.dr_icon_player_seek_left),
                contentDescription = "Previous verse",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }

        AnimatedVisibility(visible = state.isLoading, enter = fadeIn(), exit = fadeOut()) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 3.dp,
            )
        }

        AnimatedVisibility(visible = !state.isLoading, enter = fadeIn(), exit = fadeOut()) {
            FilledTonalIconButton(
                onClick = onPlayPause,
                modifier = Modifier.size(56.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(
                    painterResource(
                        if (state.isPlaying) R.drawable.dr_icon_pause2
                        else R.drawable.dr_icon_play2,
                    ),
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(32.dp),
                )
            }
        }

        IconButton(onClick = onNext) {
            Icon(
                painterResource(R.drawable.dr_icon_player_seek_right),
                contentDescription = "Next verse",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }

        IconButton(onClick = onStop) {
            Icon(
                painterResource(R.drawable.icon_verse_stop),
                contentDescription = "Stop",
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun ChapterList(
    state: RecitationServiceState,
    onChapterSelected: (ChapterEntry) -> Unit,
) {
    val activeChapter = state.currentVerse.chapterNo

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(DEMO_CHAPTERS, key = { it.number }) { chapter ->
            val isActive = chapter.number == activeChapter && state.isPlaying
            ChapterRow(
                chapter = chapter,
                isActive = isActive,
                currentVerse = if (isActive) state.currentVerse.verseNo else null,
                onClick = { onChapterSelected(chapter) },
            )
        }
    }
}

@Composable
private fun ChapterRow(
    chapter: ChapterEntry,
    isActive: Boolean,
    currentVerse: Int?,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = chapter.number.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isActive) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.width(36.dp),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chapter.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${chapter.verseCount} verses",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }

            if (currentVerse != null) {
                Text(
                    text = "Verse $currentVerse",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )
            }
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
