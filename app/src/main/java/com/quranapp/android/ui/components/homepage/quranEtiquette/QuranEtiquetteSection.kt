package com.quranapp.android.ui.components.homepage.quranEtiquette


import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.material.CircularProgressIndicator
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quranapp.android.R
import com.quranapp.android.activities.reference.ActivityEtiquette
import com.quranapp.android.components.quran.ExclusiveVerse
import com.quranapp.android.ui.components.common.SectionHeader
import com.quranapp.android.utils.reader.factory.ReaderFactory
import com.quranapp.android.viewModels.QuranEtiquetteViewModel

@Composable
fun QuranEtiquetteSection() {
    val context = LocalContext.current
    val quranEtiquetteViewModel: QuranEtiquetteViewModel = viewModel()
    quranEtiquetteViewModel.init(context)

    Column(
        modifier = Modifier
            .padding(vertical = 3.dp)
            .background(colorResource(id = R.color.colorBGHomePageItem))
    ) {
        SectionHeader(
            icon = R.drawable.veiled_muslim,
            title = R.string.titleEtiquetteVerses
        ) {
            context.startActivity(Intent(context, ActivityEtiquette::class.java))
        }
        if (quranEtiquetteViewModel.isLoading) {
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
            QuranEtiquetteList(references = quranEtiquetteViewModel.references)
        }

    }
}

@Composable
fun QuranEtiquetteList(
    modifier: Modifier = Modifier,
    references: List<ExclusiveVerse>
) {
    val context = LocalContext.current

    for (i in references.indices) {
        QuranEtiquetteItem(
            modifier = if (i == references.indices.last) Modifier.padding(bottom = 20.dp) else Modifier,
            referenceItem = references[i]
        ) {
            // Take the first reference as it has only one verse
            val reference = references[i].verses.first()
            ReaderFactory.startVerseRange(
                context,
                reference.first,
                reference.second,
                reference.third,
            )
        }
    }

}

