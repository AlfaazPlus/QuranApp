package com.quranapp.android.components.homepage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.dr_icon_update_app
import com.quranapp.android.generated.resources.recitationDownloadBannerUpdateAvailable
import com.quranapp.android.generated.resources.strLabelManage
import com.quranapp.android.navigation.SettingRoutes
import com.quranapp.android.utils.app.AppDestinations
import com.quranapp.android.utils.mediaplayer.RecitationVersionManager
import com.quranapp.android.utils.miscellenous.Keys
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun RecitationUpdateBanner() {
    val context = LocalContext.current
    val versionManager = remember { RecitationVersionManager.get(context) }
    val state by versionManager.updateUiState.collectAsState()

    if (!state.hasOutdatedDownloads) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        border = BorderStroke(1.dp, colorScheme.tertiary.copy(alpha = 0.28f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(Res.drawable.dr_icon_update_app),
                contentDescription = null,
                tint = colorScheme.tertiary,
                modifier = Modifier.size(22.dp),
            )

            Text(
                text = stringResource(Res.string.recitationDownloadBannerUpdateAvailable),
                modifier = Modifier.weight(1f),
                style = typography.bodyMedium,
                color = colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )

            FilledTonalButton(
                onClick = {
                    context.startActivity(
                        AppDestinations.intent(context, AppDestinations.SETTINGS).apply {
                            putExtra(Keys.NAV_DESTINATION, SettingRoutes.RECITATION_DOWNLOAD)
                        },
                    )
                },
            ) {
                Text(text = stringResource(Res.string.strLabelManage))
            }
        }
    }
}
