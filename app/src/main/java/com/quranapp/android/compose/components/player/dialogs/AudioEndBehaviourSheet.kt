package com.quranapp.android.compose.components.player.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.compose.components.common.AlertCard
import com.quranapp.android.compose.components.common.RadioItem
import com.quranapp.android.compose.components.dialogs.BottomSheet
import com.quranapp.android.compose.utils.preferences.RecitationPreferences
import com.quranapp.android.utils.mediaplayer.RecitationController
import kotlinx.coroutines.launch


enum class AudioEndBehaviour(val value: String) {
    STOP_PLAYBACK("stop_playback"),
    NEXT_CHAPTER("next_chapter"),
    REPEAT_CHAPTER("repeat_chapter");

    companion object {
        val DEFAULT = STOP_PLAYBACK

        fun fromValue(value: String): AudioEndBehaviour {
            return entries.firstOrNull { it.value == value } ?: STOP_PLAYBACK
        }
    }
}

@Composable
fun AudioEndBehaviourSheet(
    controller: RecitationController,
    isOpen: Boolean,
    onClose: () -> Unit,
) {
    val selectedAudioOption = RecitationPreferences.observeAudioEndBehaviour()
    val coroutineScope = rememberCoroutineScope()

    val items = listOf(
        Pair(AudioEndBehaviour.STOP_PLAYBACK, R.string.stopPlayback),
        Pair(AudioEndBehaviour.NEXT_CHAPTER, R.string.playNextSurah),
        Pair(AudioEndBehaviour.REPEAT_CHAPTER, R.string.repeatCurrentSurah),
    )

    BottomSheet(
        isOpen = isOpen,
        onDismiss = onClose,
        title = stringResource(R.string.whenChapterEnds),
    ) {
        Column(
            modifier = Modifier
                .padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 48.dp)
                .verticalScroll(
                    rememberScrollState()
                ),
        ) {
            AlertCard(Modifier.padding(bottom = 12.dp)) {
                Text(
                    text = stringResource(R.string.whenChapterEndsDesc),
                    style = typography.bodyMedium
                )
            }

            items.forEach { (option, title) ->
                RadioItem(
                    title = title,
                    selected = option == selectedAudioOption,
                    onClick = {
                        coroutineScope.launch {
                            RecitationPreferences.setAudioEndBehaviour(option)
                            controller.setAudioEndBehaviour(option)
                        }

                        onClose()
                    },
                )
            }
        }
    }
}