package com.quranapp.android.compose.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quranapp.android.R
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.compose.components.VerseOfTheDay
import com.quranapp.android.compose.components.homepage.HomeSectionDuas
import com.quranapp.android.compose.components.homepage.HomeSectionEtiquettes
import com.quranapp.android.compose.components.homepage.HomeSectionFeaturedReading
import com.quranapp.android.compose.components.homepage.HomeSectionMajorSins
import com.quranapp.android.compose.components.homepage.HomeSectionProphets
import com.quranapp.android.compose.components.homepage.HomeSectionQuranScience
import com.quranapp.android.compose.components.homepage.HomeSectionReadHistory
import com.quranapp.android.compose.components.homepage.HomeSectionSolutions
import com.quranapp.android.viewModels.HomeViewModel
import com.quranapp.android.views.homepage2.HomepageCollectionLayoutBase

private const val HOME_HISTORY_LIMIT = 10

@Composable
fun HomeScreen(
    homeVm: HomeViewModel = viewModel(),
) {
    val showInlineUpdateBanner by homeVm.showInlineUpdateBanner.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 64.dp),
    ) {
        if (showInlineUpdateBanner) {
            AppUpdateBanner(
                onUpdateClick = homeVm::openUpdateInPlayStore,
            )
        }

        VerseOfTheDay()
        HomeSectionReadHistory()
        HomeSectionFeaturedReading()
        HomeSectionDuas()
        HomeSectionSolutions()
        HomeSectionEtiquettes()
        HomeSectionMajorSins()
        HomeSectionProphets()
        HomeSectionQuranScience()
    }
}

@Composable
fun <T : HomepageCollectionLayoutBase> HomepageSection(
    factory: (Context) -> T,
    quranMeta: QuranMeta?,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth(),
        factory = { context ->
            factory(context).apply {
                initialize()
            }
        },
        update = { view ->
            quranMeta?.let(view::refresh)
        },
    )
}

@Composable
private fun AppUpdateBanner(
    onUpdateClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surface,
        ),
        border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.18f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(14.dp),
                color = colorScheme.primary.copy(alpha = 0.12f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.dr_icon_update_app),
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Text(
                text = stringResource(R.string.strMsgUpdateAvailable),
                modifier = Modifier.weight(1f),
                style = typography.bodyMedium,
                color = colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )

            FilledTonalButton(onClick = onUpdateClick) {
                Text(text = stringResource(R.string.strLabelUpdate))
            }
        }
    }
}
