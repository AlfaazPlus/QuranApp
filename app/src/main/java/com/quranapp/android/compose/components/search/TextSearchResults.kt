package com.quranapp.android.compose.components.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.alfaazplus.sunnah.ui.theme.appFontFamily
import com.alfaazplus.sunnah.ui.theme.fontUrdu
import com.quranapp.android.R
import com.quranapp.android.compose.components.common.Loader
import com.quranapp.android.compose.components.reader.dialogs.QuickReference
import com.quranapp.android.compose.components.reader.dialogs.QuickReferenceData
import com.quranapp.android.compose.theme.alpha
import com.quranapp.android.search.SearchResult
import com.quranapp.android.search.SearchResultMatch
import com.quranapp.android.utils.extensions.copyToClipboard
import com.quranapp.android.utils.reader.TranslUtils
import com.quranapp.android.utils.reader.factory.ReaderFactory
import com.quranapp.android.utils.univ.StringUtils
import com.quranapp.android.viewModels.QuranSearchViewModel

@Composable
fun TextSearchResults(
    viewModel: QuranSearchViewModel,
    results: LazyPagingItems<SearchResult>,
    hasFilters: Boolean
) {
    if (results.loadState.refresh is LoadState.Loading) {
        return Loader(true)
    }

    if (results.itemCount == 0) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                stringResource(
                    if (hasFilters)
                        R.string.strMsgSearchNoResultsFoundAbsolute
                    else
                        R.string.noResults
                ),
                style = typography.labelLarge,
            )
        }

        return
    }

    val context = LocalContext.current
    var quickRefData by remember { mutableStateOf<QuickReferenceData?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(
            results.itemCount,
            key = {
                val result = results[it]
                if (result != null) {
                    "${result.chapterNo}:${result.verseNo}-${result.matches.size}"
                } else {
                    it
                }
            }
        ) {
            val result = results[it] ?: return@items

            TextSearchResultCard(result) {
                viewModel.recordCurrentSearchQuery()

                quickRefData = QuickReferenceData(
                    chapterNo = result.chapterNo,
                    verses = result.verseNo.toString(),
                    slugs = result.matches
                        .filterIsInstance<SearchResultMatch.TranslationMatch>()
                        .map { it.slug }
                        .toSet()
                )
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
private fun TextSearchResultCard(result: SearchResult, onClick: (SearchResult) -> Unit) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        tonalElevation = 1.dp,
        shape = shapes.small,
        border = BorderStroke(1.dp, colorScheme.outlineVariant.alpha(0.75f)),
        onClick = {
            onClick(result)
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(
                        R.string.strLabelVerseSerial,
                        result.chapterNo,
                        result.verseNo
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(colorScheme.background)
                        .clickable(
                            onClick = {
                                context.copyToClipboard("${result.chapterNo}:${result.verseNo}")
                            },
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    color = colorScheme.onBackground,
                    style = typography.labelLarge,
                )
            }

            result.matches.forEachIndexed { index, match ->
                if (index > 0) {
                    HorizontalDivider(
                        color = colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                when (match) {
                    is SearchResultMatch.TranslationMatch -> {
                        CompositionLocalProvider(
                            LocalLayoutDirection provides if (StringUtils.isRtlLanguage(
                                    match.slug
                                )
                            ) LayoutDirection.Rtl else LayoutDirection.Ltr
                        ) {
                            val isUrdu = TranslUtils.isUrdu(match.slug)
                            val fontFamily = if (isUrdu) fontUrdu else appFontFamily
                            val baseFontSize = typography.bodyMedium.fontSize

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = match.displayName,
                                    style = typography.labelMedium,
                                    color = colorScheme.primary,
                                    fontFamily = fontFamily
                                )

                                Text(
                                    text = match.preview,
                                    style = typography.bodyMedium,
                                    color = colorScheme.onSurface,
                                    fontFamily = fontFamily,
                                    lineHeight = if (isUrdu) baseFontSize * 2.5f else baseFontSize * 1.5,
                                )
                            }
                        }
                    }

                    is SearchResultMatch.QuranTextMatch -> {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = match.preview,
                                style = typography.bodyMedium.copy(
                                    textDirection = TextDirection.Rtl
                                ),
                                color = colorScheme.onSurface,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}
