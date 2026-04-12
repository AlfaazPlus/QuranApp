package com.quranapp.android.compose.components.homepage

import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranapp.android.R
import com.quranapp.android.activities.reference.ActivityProphets
import com.quranapp.android.activities.reference.ActivityReference
import com.quranapp.android.components.quran.QuranProphet
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.utils.reader.factory.ReaderFactory.prepareReferenceVerseIntent
import java.text.MessageFormat

@Composable
fun HomeSectionProphets() {
    val context = LocalContext.current
    val verses = QuranProphet.observe(0..6)

    if (verses == null) return

    Column(
        modifier = Modifier
            .padding(vertical = 10.dp)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HomeSectionHeader(
            icon = R.drawable.prophets,
            title = R.string.strTitleFeaturedProphets,
            iconTint = null,
            onViewAllClick = {
                context.startActivity(Intent(context, ActivityProphets::class.java))
            }
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(verses, key = { it.name }) {
                ItemCard(it)
            }
        }
    }
}


@Composable
private fun ItemCard(
    prophet: QuranProphet.Prophet
) {
    val context = LocalContext.current
    val resources = LocalResources.current

    Surface(
        modifier = Modifier
            .width(280.dp)
            .height(96.dp)
            .clip(shape = shapes.medium)
            .border(1.dp, colorScheme.outline.alpha(0.3f), shapes.medium)
            .clickable {
                val title = resources.getString(
                    R.string.strMsgPropheticDuaInQuran,
                    MessageFormat.format("{0} ({1})", prophet.name, prophet.honorific)
                )

                val intent = prepareReferenceVerseIntent(
                    title,
                    resources.getString(R.string.strMsgReferenceDuas),
                    arrayOf(),
                    prophet.chapters,
                    prophet.verses
                ).apply {
                    setClass(context, ActivityReference::class.java)

                }

                context.startActivity(intent)
            },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Image(
                painter = painterResource(id = prophet.iconRes),
                contentDescription = prophet.name,
                modifier = Modifier
                    .size(64.dp),
                contentScale = ContentScale.Fit
            )

            Column(
                modifier = Modifier
                    .padding(start = 10.dp, end = 15.dp)
            ) {

                Text(
                    text = "${prophet.name} (${prophet.honorific})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "English: ${prophet.nameEn}",
                    fontSize = 14.sp
                )
            }
        }
    }
}