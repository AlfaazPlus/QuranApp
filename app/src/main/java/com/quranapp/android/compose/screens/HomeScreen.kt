package com.quranapp.android.compose.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.compose.components.VerseOfTheDay
import com.quranapp.android.compose.components.homepage.AppUpdateBanner
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 64.dp),
    ) {
        AppUpdateBanner()
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
