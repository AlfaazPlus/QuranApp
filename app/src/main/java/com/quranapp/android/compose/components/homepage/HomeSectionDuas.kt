package com.quranapp.android.compose.components.homepage

import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.activities.reference.ActivityExclusiveVerses
import com.quranapp.android.activities.reference.ActivityPropheticDuas
import com.quranapp.android.components.quran.ExclusiveVerse
import com.quranapp.android.components.quran.ExclusiveVersesDataset
import com.quranapp.android.components.quran.QuranExclusiveVerses
import com.quranapp.android.compose.screens.reference.ExclusiveVersesScreenKind
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.utils.reader.factory.ReaderFactory
import com.quranapp.android.utils.univ.Keys

@Composable
fun HomeSectionDuas() {
    val context = LocalContext.current
    val verses = QuranExclusiveVerses.observe(ExclusiveVersesDataset.Dua, 0..6)

    if (verses == null) return

    Column(
        modifier = Modifier
            .padding(vertical = 10.dp)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HomeSectionHeader(
            icon = R.drawable.dr_icon_rabbana,
            title = R.string.strTitleFeaturedDuas,
            onViewAllClick = {
                context.startActivity(
                    ActivityExclusiveVerses.intent(context, ExclusiveVersesScreenKind.Dua),
                )
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

    fun onClick() {
        if (model.id == 1) {
            context.startActivity(
                Intent(
                    context,
                    ActivityPropheticDuas::class.java
                ).apply {
                    putExtra(Keys.KEY_EXTRA_TITLE, model.title)
                })

            return
        }

        val excluded = model.id in arrayOf(1, 2)

        val nameTitle =
            if (!excluded) resources.getString(R.string.strMsgDuaFor, model.title)
            else resources.getString(
                R.string.strMsgReferenceInQuran,
                "\"" + model.title + "\""
            )

        val description = resources.getString(
            R.string.strMsgReferenceFoundPlaces,
            if (excluded) nameTitle else "\"" + nameTitle + "\"",
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
    }

    Box(
        modifier = Modifier
            .width(220.dp)
            .height(110.dp)
            .clip(shapes.medium)
            .background(colorScheme.surface)
            .border(1.dp, colorScheme.outline.alpha(0.3f), shapes.medium)
            .clickable {
                onClick()
            }
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = model.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                ),
                color = colorScheme.onSurface,
                maxLines = 2
            )

            if (model.id != 1 && model.chapters.isNotEmpty()) {
                Text(
                    text = model.inChapters,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Normal
                    ),
                    color = colorScheme.onSurface.copy(0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}