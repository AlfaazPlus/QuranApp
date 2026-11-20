package com.quranapp.android.components.homepage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.dr_icon_chevron_right
import com.quranapp.android.generated.resources.dr_icon_refresh
import com.quranapp.android.generated.resources.strLabelUpdate
import com.quranapp.android.generated.resources.strTitleRecitations
import com.quranapp.android.generated.resources.strTitleTranslations
import com.quranapp.android.generated.resources.updateAvailable
import com.quranapp.android.generated.resources.wordByWord
import com.quranapp.android.navigation.AppRoute
import com.quranapp.android.navigation.SettingRoutes
import com.quranapp.android.utils.mediaplayer.RecitationVersionManager
import com.quranapp.android.utils.miscellenous.Keys
import com.quranapp.android.utils.translation.TranslationVersionManager
import com.quranapp.android.utils.wbw.WbwVersionManager
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.quranapp.android.platform.PlatformServices
import com.quranapp.android.navigation.AppRoutes

@Composable
fun HomeResourceUpdatesBanner() {
    val context = LocalContext.current
    val recitationManager = remember { RecitationVersionManager.get(context) }
    val translationManager = remember { TranslationVersionManager.get() }
    val wbwManager = remember { WbwVersionManager.get() }

    LaunchedEffect(Unit) {
        recitationManager.refreshOutdatedState()
        translationManager.refreshOutdatedState()
        wbwManager.refreshOutdatedState()
    }

    val recitationState by recitationManager.updateUiState.collectAsState()
    val translationState by translationManager.updateUiState.collectAsState()
    val wbwState by wbwManager.updateUiState.collectAsState()

    val recitationCount = recitationState.outdatedReciters.size
    val translationCount = translationState.outdatedSlugs.size
    val wbwCount = wbwState.outdatedLanguageIds.size
    val total = recitationCount + translationCount + wbwCount

    if (total <= 0) return

    val openRoute: (String) -> Unit = { targetRoute ->
        PlatformServices.navigator.navigate(
            AppRoute.Screen(
                screen = AppRoutes.SETTINGS,
                extras = mapOf(Keys.NAV_DESTINATION to targetRoute)
            )
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        shape = shapes.medium,
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        border = BorderStroke(1.dp, colorScheme.tertiary.copy(alpha = 0.28f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.dr_icon_refresh),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )

                Text(
                    text = stringResource(Res.string.updateAvailable),
                    style = typography.titleSmall,
                    color = colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (recitationCount > 0) {
                ResourceUpdateRow(
                    title = stringResource(Res.string.strTitleRecitations),
                    count = recitationCount,
                    onClick = { openRoute(SettingRoutes.RECITATION_DOWNLOAD) },
                )
            }

            if (translationCount > 0) {
                ResourceUpdateRow(
                    title = stringResource(Res.string.strTitleTranslations),
                    count = translationCount,
                    onClick = { openRoute(SettingRoutes.TRANSLATIONS) },
                )
            }

            if (wbwCount > 0) {
                ResourceUpdateRow(
                    title = stringResource(Res.string.wordByWord),
                    count = wbwCount,
                    onClick = { openRoute(SettingRoutes.WWB) },
                )
            }
        }
    }
}

@Composable
private fun ResourceUpdateRow(
    title: String,
    count: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$title ($count)",
            style = typography.bodyMedium,
            color = colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(colorScheme.tertiaryContainer.copy(alpha = 0.55f))
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.strLabelUpdate),
                style = typography.labelMedium,
                color = colorScheme.onTertiaryContainer,
            )

            Icon(
                painter = painterResource(Res.drawable.dr_icon_chevron_right),
                contentDescription = null,
                tint = colorScheme.onTertiaryContainer,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}
