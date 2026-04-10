package com.quranapp.android.compose.components.homepage

import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.activities.reference.ActivitySolutionVerses
import com.quranapp.android.components.quran.ExclusiveVerse
import com.quranapp.android.components.quran.QuranSolutions
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.utils.reader.factory.ReaderFactory

@Composable
fun HomeSectionSolutions() {
    val context = LocalContext.current
    val verses by produceState<List<ExclusiveVerse>?>(null, context) {
        value = QuranSolutions.get(context).subList(0, 6)
    }

    if (verses == null) return

    Column(
        modifier = Modifier
            .padding(vertical = 10.dp)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HomeSectionHeader(
            icon = R.drawable.dr_icon_read_quran,
            title = R.string.titleSolutionVerses,
            onViewAllClick = {
                context.startActivity(Intent(context, ActivitySolutionVerses::class.java))
            }
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(verses!!, key = { it.id }) {
                ItemCard(it)
            }
        }
    }
}


@Composable
private fun ItemCard(
    model: ExclusiveVerse
) {
    val context = LocalContext.current
    val resources = LocalResources.current

    Box(
        modifier = Modifier
            .width(220.dp)
            .height(110.dp)
            .clip(shapes.medium)
            .background(Color.Black)
            .clickable {
                val nameTitle =
                    resources.getString(R.string.strMsgReferenceInQuran, "\"${model.title}\"")

                val description = resources.getString(
                    R.string.strMsgReferenceFoundPlaces,
                    "\"${model.title}\"",
                    model.verses.size
                )

                ReaderFactory.startReferenceVerse(
                    context,
                    true,
                    nameTitle,
                    description,
                    arrayOf(),
                    model.chapters,
                    model.versesRaw
                )
            },
    ) {
        Image(
            painter = painterResource(R.drawable.dr_quran_wallpaper),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.6f)
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0.5f to colorScheme.primary.alpha(0.1f),
                        1f to Color.Black.alpha(0.9f)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = model.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color.White,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = model.inChapters,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}