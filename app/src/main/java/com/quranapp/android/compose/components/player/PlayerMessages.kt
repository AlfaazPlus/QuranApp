package com.quranapp.android.compose.components.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.utils.mediaplayer.RecitationServiceState

@Composable
fun PlayerMessages(state: RecitationServiceState, isPlaying: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        state.downloadProgress?.let { progress ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (progress > 0) {
                    CircularProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = PlayerContentColor.alpha(0.9f),
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = PlayerContentColor.alpha(0.9f),
                    )
                }
                Text(
                    text = stringResource(R.string.textDownloading) + " " + progress + "%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PlayerContentColor.alpha(0.9f),
                )
            }
        }

        if (isPlaying && state.resolvingChapterNo != null && !state.isVerseSyncAvailable) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.dr_icon_info),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = PlayerContentColor.alpha(0.85f),
                )
                Text(
                    text = stringResource(R.string.strMsgNoVerseTiming),
                    style = MaterialTheme.typography.bodySmall,
                    color = PlayerContentColor.alpha(0.85f),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}