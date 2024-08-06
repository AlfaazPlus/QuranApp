package com.quranapp.android.ui.components.homepage.quranSolution


import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.material.CircularProgressIndicator
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quranapp.android.R
import com.quranapp.android.activities.reference.ActivitySolutionVerses
import com.quranapp.android.components.quran.ExclusiveVerse
import com.quranapp.android.ui.components.common.SectionHeader
import com.quranapp.android.utils.reader.factory.ReaderFactory
import com.quranapp.android.viewModels.QuranSolutionViewModel

@Composable
fun QuranSolutionSection() {
    val context = LocalContext.current
    val quranSolutionViewModel: QuranSolutionViewModel = viewModel()
    quranSolutionViewModel.init(context)

    Column(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .background(colorResource(id = R.color.colorBGHomePageItem))
    ) {
        SectionHeader(
            icon = R.drawable.dr_icon_read_quran,
            iconColor = R.color.warning,
            title = R.string.titleSolutionVerses
        ) {
            context.startActivity(Intent(context, ActivitySolutionVerses::class.java))
        }
        if (quranSolutionViewModel.isLoading) {
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
            QuranSolutionList(references = quranSolutionViewModel.references)
        }

    }
}

@Composable
fun QuranSolutionList(
    references: List<ExclusiveVerse>
) {
    val context = LocalContext.current
    LazyRow(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(15.dp)
    ) {
        itemsIndexed(items = references, key = {_, item -> item.hashCode()}) {_, it ->
            QuranSolutionCard(
                reference = it
            ) {
                val nameTitle =
                    context.getString(R.string.strMsgReferenceInQuran, "\"" + it.name + "\"")

                val description = context.getString(
                    R.string.strMsgReferenceFoundPlaces,
                    "\"" + it.name + "\"",
                    it.verses.size
                )

                ReaderFactory.startReferenceVerse(
                    context,
                    true,
                    nameTitle,
                    description,
                    arrayOf(),
                    it.chapters,
                    it.versesRaw
                )

            }
        }
    }
}

