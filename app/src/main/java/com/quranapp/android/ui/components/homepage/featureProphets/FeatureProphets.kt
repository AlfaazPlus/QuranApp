package com.quranapp.android.ui.components.homepage.featureProphets

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quranapp.android.R
import com.quranapp.android.activities.reference.ActivityProphets
import com.quranapp.android.components.quran.QuranProphet
import com.quranapp.android.ui.components.common.SectionHeader
import com.quranapp.android.viewModels.FeatureProphetsViewModel


@Composable
fun FeatureProphetsSection() {
    val context = LocalContext.current
    val featureProphetsViewModel: FeatureProphetsViewModel = viewModel()
    featureProphetsViewModel.init(context)

    Column(
        modifier = Modifier
            .padding(vertical = 3.dp)
            .background(colorResource(id = R.color.colorBGHomePageItem))
    ) {
        SectionHeader(
            icon = R.drawable.prophets,
            title = R.string.strTitleFeaturedProphets
        ) {
            context.startActivity(Intent(context, ActivityProphets::class.java))
        }
        if (featureProphetsViewModel.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp), contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.width(40.dp),
                    color = colorResource(id = R.color.colorPrimary)
                )
            }
        } else {
            FeatureProphetsList(prophets = featureProphetsViewModel.prophets)
        }

    }
}

@Composable
fun FeatureProphetsList(
    modifier: Modifier = Modifier,
    prophets: List<QuranProphet.Prophet>
) {
    LazyRow(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        items(prophets) {
            FeatureProphetsCard(prophet = it)
        }
    }
}
