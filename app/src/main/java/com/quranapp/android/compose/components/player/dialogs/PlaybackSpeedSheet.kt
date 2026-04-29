package com.quranapp.android.compose.components.player.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.compose.components.common.RadioItem
import com.quranapp.android.compose.components.dialogs.BottomSheet
import com.quranapp.android.utils.mediaplayer.RecitationController
import com.quranapp.android.compose.utils.appLocale
import com.quranapp.android.compose.utils.preferences.RecitationPreferences
import kotlinx.coroutines.launch

@Composable
fun PlaybackSpeedSheet(
    controller: RecitationController,
    isOpen: Boolean,
    onClose: () -> Unit,
) {
    val selectedSpeed = RecitationPreferences.observeSpeed()
    val coroutineScope = rememberCoroutineScope()
    val speedOptions = listOf(0.1f, 0.3f, 0.5f, 0.7f, 1f, 1.3f, 1.5f, 1.7f, 2f, 3f)

    BottomSheet(
        isOpen = isOpen,
        onDismiss = onClose,
        icon = R.drawable.icon_playback_speed,
        title = stringResource(R.string.playbackSpeed),
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 48.dp),
        ) {
            items(
                count = speedOptions.size,
                key = { speedOptions[it] },
            ) { index ->
                val speed = speedOptions[index]

                RadioItem(
                    titleStr = String.format(LocalLocale.current.platformLocale, "%.1fx", speed),
                    selected = speed == selectedSpeed,
                    onClick = {
                        if (speed == selectedSpeed) return@RadioItem

                        coroutineScope.launch {
                            RecitationPreferences.setSpeed(speed)
                            controller.setSpeed(speed)
                        }
                    },
                )
            }
        }
    }
}
