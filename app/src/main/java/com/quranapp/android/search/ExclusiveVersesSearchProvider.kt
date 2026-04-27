package com.quranapp.android.search

import android.content.Context
import com.quranapp.android.components.quran.ExclusiveVerse
import com.quranapp.android.components.quran.ExclusiveVersesDataset
import com.quranapp.android.components.quran.QuranExclusiveVerses
import com.quranapp.android.components.quran.QuranScienceItem
import com.quranapp.android.compose.screens.science.loadScienceItems

sealed class CollectionSearchResult {
    data class ExclusiveVerseItem(
        val dataset: ExclusiveVersesDataset,
        val verse: ExclusiveVerse,
    ) : CollectionSearchResult()

    data class ScienceTopicItem(
        val item: QuranScienceItem,
    ) : CollectionSearchResult()
}

object ExclusiveVersesSearchProvider {
    suspend fun search(context: Context, query: String): List<CollectionSearchResult> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) return emptyList()

        val exclusiveMatches = ExclusiveVersesDataset.entries.flatMap { dataset ->
            QuranExclusiveVerses.get(context, dataset)
                .asSequence()
                .filter { verse ->
                    verse.title.contains(normalizedQuery, ignoreCase = true) ||
                            (verse.description?.contains(
                                normalizedQuery,
                                ignoreCase = true
                            ) == true)
                }
                .map { verse ->
                    CollectionSearchResult.ExclusiveVerseItem(
                        dataset = dataset,
                        verse = verse,
                    )
                }
                .toList()
        }

        val scienceMatches = loadScienceItems(context)
            .asSequence()
            .filter { topic ->
                topic.getTitle().contains(normalizedQuery, ignoreCase = true) ||
                        topic.path.contains(normalizedQuery, ignoreCase = true)
            }
            .map { topic ->
                CollectionSearchResult.ScienceTopicItem(topic)
            }
            .toList()

        return exclusiveMatches + scienceMatches
    }
}
