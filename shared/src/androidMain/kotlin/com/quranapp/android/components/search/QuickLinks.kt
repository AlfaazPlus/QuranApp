package com.quranapp.android.components.search

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quranapp.android.components.reader.dialogs.QuickReference
import com.quranapp.android.components.reader.dialogs.QuickReferenceData
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.labelHizbNo
import com.quranapp.android.generated.resources.strLabelJuzNo
import com.quranapp.android.generated.resources.strLabelSurah
import com.quranapp.android.generated.resources.strLabelVerseWithChapNo
import com.quranapp.android.generated.resources.tafsirForVerse
import com.quranapp.android.locale.LocalAppLocale
import com.quranapp.android.navigation.AppRoute
import com.quranapp.android.navigation.AppRoutes
import com.quranapp.android.platform.Platform
import com.quranapp.android.search.QuickLinkItem
import com.quranapp.android.search.stableKey
import com.quranapp.android.theme.alpha
import com.quranapp.android.utils.miscellenous.Keys
import com.quranapp.android.utils.reader.factory.ReaderFactory
import com.quranapp.android.viewmodel.QuranSearchViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun QuickLinks(viewModel: QuranSearchViewModel) {
    val quickLinks by viewModel.quickLinks.collectAsState()
    if (quickLinks.isEmpty()) return

    var quickRefData by remember { mutableStateOf<QuickReferenceData?>(null) }

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
                            viewModel.commitOnIntentionalOutcome()

                            when (item) {
                                is QuickLinkItem.Verse -> {
                                    quickRefData = QuickReferenceData(
                                        chapterNo = item.chapterNo,
                                        verses = item.verseNo.toString(),
                                        slugs = emptySet()
                                    )
                                }

                                is QuickLinkItem.Tafsir -> {
                                    Platform.navigator.navigate(
                                        AppRoute.Screen(
                                            screen = AppRoutes.TAFSIR,
                                            extras = mapOf(
                                                Keys.READER_KEY_CHAPTER_NO to item.chapterNo,
                                                Keys.READER_KEY_VERSE_NO to item.verseNo
                                            )
                                        )
                                    )
                                }

                                is QuickLinkItem.Chapter -> {
                                    Platform.navigator.navigate(
                                        AppRoute.Reader(
                                            ReaderFactory.chapter(
                                                item.surah.surah.surahNo
                                            )
                                        )
                                    )
                                }

                                is QuickLinkItem.Juz -> {
                                    Platform.navigator.navigate(
                                        AppRoute.Reader(
                                            ReaderFactory.juz(
                                                item.juzNo
                                            )
                                        )
                                    )
                                }

                                is QuickLinkItem.Hizb -> {
                                    Platform.navigator.navigate(
                                        AppRoute.Reader(
                                            ReaderFactory.hizb(
                                                item.hizbNo
                                            )
                                        )
                                    )
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
            Platform.navigator.navigate(
                AppRoute.Reader(
                    ReaderFactory.verseRange(
                        chapterNo,
                        range.first,
                        range.last
                    )
                )
            )
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
    val appLocale = LocalAppLocale.current
    val label = when (item) {
        is QuickLinkItem.Chapter -> {
            listOf(
                appLocale.format("%d.", item.surah.surah.surahNo),
                stringResource(Res.string.strLabelSurah, item.surah.getCurrentName()),
            ).joinToString(" ")
        }

        is QuickLinkItem.Verse ->
            stringResource(
                Res.string.strLabelVerseWithChapNo,
                item.chapterNo,
                item.verseNo
            )

        is QuickLinkItem.Tafsir ->
            stringResource(
                Res.string.tafsirForVerse,
                item.chapterNo,
                item.verseNo,
            )

        is QuickLinkItem.Juz ->
            stringResource(Res.string.strLabelJuzNo, item.juzNo)

        is QuickLinkItem.Hizb ->
            stringResource(Res.string.labelHizbNo, item.hizbNo)
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
