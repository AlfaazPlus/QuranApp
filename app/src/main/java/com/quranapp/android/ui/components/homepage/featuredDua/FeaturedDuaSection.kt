package com.quranapp.android.ui.components.homepage.featuredDua


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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.material.CircularProgressIndicator
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quranapp.android.R
import com.quranapp.android.activities.reference.ActivityDua
import com.quranapp.android.activities.reference.ActivityPropheticDuas
import com.quranapp.android.components.quran.ExclusiveVerse
import com.quranapp.android.ui.components.common.SectionHeader
import com.quranapp.android.utils.reader.factory.ReaderFactory
import com.quranapp.android.utils.univ.Keys
import com.quranapp.android.viewModels.FeaturedDuaViewModel

@Composable
fun FeaturedDuaSection() {
    val context = LocalContext.current
    val featuredDuaViewModel: FeaturedDuaViewModel = viewModel()
    featuredDuaViewModel.init(context)

    Column(
        modifier = Modifier
            .padding(vertical = 3.dp)
            .background(colorResource(id = R.color.colorBGHomePageItem))
    ) {
        SectionHeader(
            icon = R.drawable.dr_icon_rabbana,
            iconColor = R.color.colorPrimary,
            title = R.string.strTitleFeaturedDuas
        ) {
            context.startActivity(Intent(context, ActivityDua::class.java))
        }
        if (featuredDuaViewModel.isLoading) {
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
            FeaturedDuaList(featuredList = featuredDuaViewModel.duas)
        }

    }
}

@Composable
fun FeaturedDuaList(
    modifier: Modifier = Modifier,
    featuredList: List<ExclusiveVerse>
) {
    val context = LocalContext.current
    LazyRow(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        items(featuredList) {
            FeaturedDuaCard(
                featuredItem = it
            ) {
                val excluded = it.id in arrayOf(1, 2)
                if (it.id == 1) {
                    context.startActivity(Intent(context, ActivityPropheticDuas::class.java).apply {
                        putExtra(Keys.KEY_EXTRA_TITLE, it.name)
                    })
                } else {
                    val nameTitle = if (!excluded) context.getString(R.string.strMsgDuaFor, it.name)
                    else context.getString(R.string.strMsgReferenceInQuran, "\"" + it.name + "\"")

                    val description = context.getString(
                        R.string.strMsgReferenceFoundPlaces,
                        if (excluded) nameTitle else "\"" + nameTitle + "\"",
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
}


