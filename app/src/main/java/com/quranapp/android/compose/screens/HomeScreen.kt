package com.quranapp.android.compose.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quranapp.android.compose.components.homepage.AppUpdateBanner
import com.quranapp.android.compose.components.homepage.HomeSectionDuas
import com.quranapp.android.compose.components.homepage.HomeSectionEtiquettes
import com.quranapp.android.compose.components.homepage.HomeSectionFeaturedReading
import com.quranapp.android.compose.components.homepage.HomeSectionHeader
import com.quranapp.android.compose.components.homepage.HomeSectionMajorSins
import com.quranapp.android.compose.components.homepage.HomeSectionProphets
import com.quranapp.android.compose.components.homepage.HomeSectionQuranScience
import com.quranapp.android.compose.components.homepage.HomeSectionReadHistory
import com.quranapp.android.compose.components.homepage.HomeSectionSolutions
import com.quranapp.android.compose.components.homepage.HomeTabbedSection
import com.quranapp.android.compose.components.player.MINI_PLAYER_HEIGHT
import com.quranapp.android.viewModels.HomeViewModel

private const val HOME_HISTORY_LIMIT = 10

@Composable
fun HomeScreen(
    modifier: Modifier,
    homeVm: HomeViewModel = viewModel(),
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 64.dp)
            .padding(bottom = MINI_PLAYER_HEIGHT),
    ) {
        AppUpdateBanner()

        HomeTabbedSection()

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