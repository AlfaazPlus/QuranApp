package com.quranapp.android.compose.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quranapp.android.compose.components.MainAppBar
import com.quranapp.android.compose.components.homepage.AppUpdateBanner
import com.quranapp.android.compose.components.homepage.HomeResourceUpdatesBanner
import com.quranapp.android.compose.components.homepage.HomeSectionFeaturedReading
import com.quranapp.android.compose.components.homepage.HomeSectionVersesCollections
import com.quranapp.android.compose.components.homepage.HomeTabbedSection
import com.quranapp.android.compose.components.player.MINI_PLAYER_HEIGHT

private const val HOME_HISTORY_LIMIT = 10

@Composable
fun HomeScreen() {
        Scaffold(
            contentWindowInsets = ScaffoldDefaults.contentWindowInsets.only(
                WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
            ),
            topBar = { MainAppBar() },
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .background(colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 64.dp)
                    .padding(bottom = MINI_PLAYER_HEIGHT),
            ) {
                AppUpdateBanner()
                HomeResourceUpdatesBanner()

                HomeTabbedSection()

                HomeSectionFeaturedReading()

                HomeSectionVersesCollections()
            }
        }
}
