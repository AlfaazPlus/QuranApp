package com.quranapp.android.compose.components.player.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.quranapp.android.utils.mediaplayer.RecitationController
import com.quranapp.android.compose.utils.preferences.RecitationPreferences
import com.quranapp.android.utils.reader.recitation.RecitationUtils
import kotlinx.coroutines.launch

@Composable
fun RepeatOptionsSheet(
    controller: RecitationController,
    isOpen: Boolean,
    onClose: () -> Unit,
) {
    val selectedAudioOption = RecitationPreferences.observeAudioOption()
    val repeatSupported = selectedAudioOption == AudioOption.ONLY_QURAN

    val currentRepeatCount = RecitationPreferences.observeRepeatCount()
    val coroutineScope = rememberCoroutineScope()
    val repeatOptions = listOf(0, 1, 2, 4, 9)

    BottomSheet(
        isOpen = isOpen,
        onDismiss = onClose,
        icon = R.drawable.ic_repeat,
        title = stringResource(R.string.playbackCount),
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(
                start = 8.dp,
                end = 8.dp,
                top = 12.dp,
                bottom = 48.dp
            ),
        ) {
            item {
                AlertCard(
                    modifier = Modifier.padding(bottom = 8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.msgRepeat),
                        style = typography.bodyMedium
                    )
                }
            }
            items(
                count = repeatOptions.size,
                key = { repeatOptions[it] },
            ) { index ->
                val repeatCount = repeatOptions[index]

                RadioItem(
                    titleStr = when (repeatCount) {
                        0 -> stringResource(R.string.once)
                        1 -> stringResource(R.string.twice)
                        else -> stringResource(R.string.nTimes, repeatCount + 1)
                    },
                    enabled = repeatSupported,
                    selected = repeatCount == currentRepeatCount,
                    onClick = {
                        if (repeatCount == currentRepeatCount) return@RadioItem

                        coroutineScope.launch {
                            RecitationPreferences.setRepeatCount(repeatCount)
                            controller.setRepeatCount(repeatCount)
                        }
                    },
                )
            }
        }
    }
}