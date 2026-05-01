package com.quranapp.android.compose.components.homepage

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
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alfaazplus.sunnah.ui.theme.tightTextStyle
import com.quranapp.android.R
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.compose.utils.LocalAppLocale
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.repository.QuranRepository
import com.quranapp.android.utils.reader.factory.ReaderFactory

private data class FeaturedQuranModel(
    val chapterNo: Int,
    val verseRange: Pair<Int, Int>,
) {
    var title: String = ""
    var subtext: String = ""
}

@Composable
fun HomeSectionFeaturedReading() {
    val featuredItems by getFeaturedQuranModels()

    if (featuredItems == null) return

    Column(
        modifier = Modifier
            .padding(vertical = 10.dp)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HomeSectionHeader(
            icon = R.drawable.dr_icon_feature,
            title = R.string.strTitleFeaturedQuran,
            iconTint = null
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(featuredItems!!, key = { it.chapterNo.toString() + it.subtext }) {
                FeaturedQuranCard(it)
            }
        }
    }
}


@Composable
private fun FeaturedQuranCard(
    model: FeaturedQuranModel
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .width(220.dp)
            .height(110.dp)
            .clip(shapes.medium)
            .background(Color.Black)
            .clickable {
                ReaderFactory.startVerseRange(context, model.chapterNo, model.verseRange)
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
                ).merge(tightTextStyle),
                color = Color.White,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = model.subtext,
                style = MaterialTheme.typography.labelSmall.merge(tightTextStyle),
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun getFeaturedQuranModels(): State<List<FeaturedQuranModel>?> {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val resources = LocalResources.current
    val appLocale = LocalAppLocale.current
    val locale = appLocale.platformLocale
    appLocale.fallbackLanguageCodes().toList()

    return produceState<List<FeaturedQuranModel>?>(
        null,
        context,
        configuration,
        resources,
        appLocale,
    ) {
        val repo = DatabaseProvider.getQuranRepository(context)

        val itemsArray = resources.obtainTypedArray(R.array.arrFeaturedQuranItems)

        val chapNameFormat = resources.getString(R.string.strLabelSurah)
        val verseNoFormat = resources.getString(R.string.strLabelVerseNo)
        val versesFormat = resources.getString(R.string.strLabelVerses)
        val miniInfoFormat = resources.getString(R.string.strLabelVerseWithChapNameWithBar)
        val miniInfoChapFormat = resources.getString(R.string.strLabelFeatureQuranMiniInfo)

        val models = List(itemsArray.length()) { i ->
            val raw = itemsArray.getString(i)!!
            val (chapterNo, start, end) = parseItem(raw, repo)

            val chapterName = repo.getChapterName(chapterNo)

            FeaturedQuranModel(
                chapterNo,
                start to end,
            ).apply {
                if (start == 1 && end == repo.getChapterVerseCount(chapterNo) && !raw.contains(":")) {
                    title = String.format(locale, chapNameFormat, chapterName)
                    subtext = String.format(locale, miniInfoChapFormat, chapterNo, 1, end)
                } else if (start == end) {
                    if (chapterNo == 2 && start == 255) {
                        title = resources.getString(R.string.strAyatulKursi)
                        subtext = String.format(locale, miniInfoFormat, chapterName, 255)
                    } else {
                        title = String.format(locale, chapNameFormat, chapterName)
                        subtext = String.format(locale, verseNoFormat, start)
                    }
                } else {
                    title = String.format(locale, chapNameFormat, chapterName)
                    subtext = String.format(locale, versesFormat, start, end)
                }
            }
        }

        itemsArray.recycle()

        value = models
    }
}

private suspend fun parseItem(
    raw: String,
    repo: QuranRepository
): Triple<Int, Int, Int> {
    val colonIndex = raw.indexOf(':')

    if (colonIndex == -1) {
        val chapter = raw.toInt()
        return Triple(
            chapter,
            1,
            repo.getChapterVerseCount(chapter)
        )
    }

    val chapter = raw.substring(0, colonIndex).toInt()
    val versePart = raw.substring(colonIndex + 1)

    val dashIndex = versePart.indexOfFirst { it == '-' || it == '–' }

    return if (dashIndex == -1) {
        val verse = versePart.toInt()
        Triple(chapter, verse, verse)
    } else {
        val start = versePart.substring(0, dashIndex).toInt()
        val end = versePart.substring(dashIndex + 1).toInt()
        Triple(chapter, start, end)
    }
}
