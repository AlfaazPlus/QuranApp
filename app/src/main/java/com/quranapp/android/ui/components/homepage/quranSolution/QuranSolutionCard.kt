package com.quranapp.android.ui.components.homepage.quranSolution

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.quranapp.android.R
import com.quranapp.android.components.quran.ExclusiveVerse
import com.quranapp.android.ui.components.common.ExclusiveVersesText
import com.quranapp.android.ui.components.homepage.HomepageCard

@Composable
fun QuranSolutionCard(reference: ExclusiveVerse, onClick: () -> Unit) {
    val context = LocalContext.current

    val count = reference.verses.size
    val subTitle = if (count > 1) context.getString(R.string.places, count)
    else context.getString(R.string.place, count)

    HomepageCard(onClick = onClick) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.quran_wallpaper2),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.4f
            )

            ExclusiveVersesText(
                title = reference.name,
                subTitle = subTitle,
                inChapters = reference.inChapters
            )

        }

    }
}
