package com.quranapp.android.components.player.dialogs

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
import com.quranapp.android.components.common.AlertCard
import com.quranapp.android.components.common.RadioItem
import com.quranapp.android.components.common.dialogs.BottomSheet
import com.quranapp.android.core.R
import com.quranapp.android.preferences.RecitationPreferences
import com.quranapp.android.preferences.observeAudioEndBehaviour
import com.quranapp.android.utils.mediaplayer.AudioEndBehaviour
import com.quranapp.android.utils.mediaplayer.RecitationController
import kotlinx.coroutines.launch

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
