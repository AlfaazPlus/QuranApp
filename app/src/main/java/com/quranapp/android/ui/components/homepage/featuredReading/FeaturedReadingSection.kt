package com.quranapp.android.ui.components.homepage.featuredReading



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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quranapp.android.R
import com.quranapp.android.components.FeaturedQuranModel
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.ui.components.common.SectionHeader
import com.quranapp.android.utils.reader.factory.ReaderFactory
import com.quranapp.android.viewModels.FeaturedReadingViewModel

@Composable
fun FeaturedReadingSection() {
    val context = LocalContext.current
    val featuredReadingViewModel: FeaturedReadingViewModel = viewModel()

    LaunchedEffect(Unit) {
        featuredReadingViewModel.init(context)
    }

    val quranMeta by featuredReadingViewModel.quranMeta.collectAsState()

    Column(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .background(colorResource(id = R.color.colorBGHomePageItem))
    ) {
        SectionHeader(
            icon = R.drawable.dr_icon_feature,
            title = R.string.strTitleFeaturedQuran
        )
        if (featuredReadingViewModel.isLoading) {
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
            FeaturedReadingList(
                featuredList = featuredReadingViewModel.models,
                quranMeta = quranMeta
            )
        }

    }
}

@Composable
fun FeaturedReadingList(
    featuredList: List<FeaturedQuranModel>,
    quranMeta: QuranMeta
) {
    val context = LocalContext.current
    LazyRow(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(15.dp)
    ) {
        itemsIndexed(items = featuredList, key = {_, item -> item.hashCode()}) {_, it ->
            FeaturedReadingCard(
                featuredItem = it,
                onClick = {
                    val chapterNo = it.chapterNo
                    val verseRange = it.verseRange
                    if (QuranMeta.isChapterValid(chapterNo) &&
                        quranMeta.isVerseRangeValid4Chapter(
                            chapterNo,
                            verseRange.first,
                            verseRange.second
                        )
                    ) {
                        ReaderFactory.startVerseRange(context, chapterNo, verseRange)
                    }
                }
            )
        }
    }
}

