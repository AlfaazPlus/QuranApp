package com.quranapp.android.compose.components.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.compose.components.reader.dialogs.QuickReference
import com.quranapp.android.compose.components.reader.dialogs.QuickReferenceData
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.search.QuickLinkItem
import com.quranapp.android.search.stableKey
import com.quranapp.android.utils.reader.factory.ReaderFactory
import com.quranapp.android.viewModels.QuranSearchViewModel

@Composable
fun QuickLinks(viewModel: QuranSearchViewModel) {
    val quickLinks by viewModel.quickLinks.collectAsState()
    if (quickLinks.isEmpty()) return

    var quickRefData by remember { mutableStateOf<QuickReferenceData?>(null) }
    val context = LocalContext.current

    Surface(
        color = colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth(),
    ) {
        FlowRow(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            quickLinks.forEach { item ->
                key(item.stableKey()) {
                    CompactQuickLinkCard(
                        item = item,
                        onClick = {
                            viewModel.recordCurrentSearchQuery()

                            when (item) {
                                is QuickLinkItem.Verse -> {
                                    quickRefData = QuickReferenceData(
                                        chapterNo = item.chapterNo,
                                        verses = item.verseNo.toString(),
                                        slugs = emptySet()
                                    )
                                }

                                is QuickLinkItem.Tafsir -> {
                                    ReaderFactory.startTafsir(context, item.chapterNo, item.verseNo)
                                }

                                is QuickLinkItem.Chapter -> {
                                    ReaderFactory.startChapter(context, item.surah.surah.surahNo)
                                }

                                is QuickLinkItem.Juz -> {
                                    ReaderFactory.startJuz(context, item.juzNo)
                                }

                                is QuickLinkItem.Hizb -> {
                                    ReaderFactory.startHizb(context, item.hizbNo)
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    QuickReference(
        data = quickRefData,
        onOpenInReader = { chapterNo, range ->
            quickRefData = null
            ReaderFactory.startVerseRange(context, chapterNo, range.first, range.last)
        },
        onClose = { quickRefData = null },
    )
}

@Composable
private fun CompactQuickLinkCard(
    item: QuickLinkItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = when (item) {
        is QuickLinkItem.Chapter -> {
            val name = stringResource(R.string.strLabelSurah, item.surah.getCurrentName())
            listOf("${item.surah.surah.surahNo}.", name).joinToString(" ")
        }

        is QuickLinkItem.Verse ->
            stringResource(R.string.strLabelVerseWithChapNo, item.chapterNo, item.verseNo)

        is QuickLinkItem.Tafsir ->
            stringResource(
                R.string.tafsirForVerse,
                item.chapterNo,
                item.verseNo,
            )

        is QuickLinkItem.Juz ->
            stringResource(R.string.strLabelJuzNo, item.juzNo)

        is QuickLinkItem.Hizb ->
            stringResource(R.string.labelHizbNo, item.hizbNo)
    }

    val shape = RoundedCornerShape(8.dp)

    Surface(
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            width = 1.dp,
            color = colorScheme.outlineVariant.alpha(0.45f),
        ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        )
    }
}
