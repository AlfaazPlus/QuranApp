package com.quranapp.android.compose.components.search

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quranapp.android.R
import com.quranapp.android.activities.reference.ActivityQuranScienceContent
import com.quranapp.android.components.quran.ExclusiveVersesDataset
import com.quranapp.android.search.CollectionSearchResult
import com.quranapp.android.utils.reader.ExclusiveVerseNavigator
import com.quranapp.android.viewModels.QuranSearchViewModel

@Composable
fun ExclusiveSearchResults(
    viewModel: QuranSearchViewModel,
    results: List<CollectionSearchResult>,
) {
    if (results.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(R.string.noResults),
                style = typography.labelLarge,
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(
            items = results,
            key = {
                when (it) {
                    is CollectionSearchResult.ExclusiveVerseItem -> "${it.dataset.name}:${it.verse.id}"
                    is CollectionSearchResult.ScienceTopicItem -> "science:${it.item.path}"
                }
            },
        ) { result ->
            ExclusiveSearchResultCard(result) {
                viewModel.recordCurrentSearchQuery()
            }
        }
    }
}

@Composable
private fun ExclusiveSearchResultCard(
    result: CollectionSearchResult,
    onClick: () -> Unit,
) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.75f)),
        onClick = {
            onClick()
            when (result) {
                is CollectionSearchResult.ExclusiveVerseItem -> {
                    ExclusiveVerseNavigator.open(context, result.dataset, result.verse)
                }

                is CollectionSearchResult.ScienceTopicItem -> {
                    context.startActivity(
                        Intent(context, ActivityQuranScienceContent::class.java).apply {
                            putExtra("item", result.item)
                        }
                    )
                }
            }
        },
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(datasetTitleRes(result)),
                style = typography.labelMedium,
                color = colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )

            Text(
                text = resultTitle(result),
                style = typography.titleSmall,
                color = colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )

            if (result is CollectionSearchResult.ScienceTopicItem) {
                Text(
                    text = stringResource(
                        R.string.strLabelScienceReferences,
                        result.item.referencesCount
                    ),
                    style = typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun datasetTitleRes(result: CollectionSearchResult): Int {
    return when (result) {
        is CollectionSearchResult.ExclusiveVerseItem -> {
            when (result.dataset) {
                ExclusiveVersesDataset.Dua -> R.string.strTitleFeaturedDuas
                ExclusiveVersesDataset.Etiquette -> R.string.titleEtiquetteVerses
                ExclusiveVersesDataset.MajorSins -> R.string.strTitleMajorSins
                ExclusiveVersesDataset.Solution -> R.string.titleSolutionVerses
            }
        }

        is CollectionSearchResult.ScienceTopicItem -> R.string.quran_and_science
    }
}

private fun resultTitle(result: CollectionSearchResult): String {
    return when (result) {
        is CollectionSearchResult.ExclusiveVerseItem -> result.verse.title
        is CollectionSearchResult.ScienceTopicItem -> result.item.getTitle()
    }
}
