package com.quranapp.android.search

import android.content.Context
import com.quranapp.android.components.quran.ExclusiveVerse
import com.quranapp.android.components.quran.ExclusiveVersesDataset
import com.quranapp.android.components.quran.QuranExclusiveVerses
import com.quranapp.android.components.quran.QuranScienceItem
import com.quranapp.android.compose.screens.science.loadScienceItems
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.repository.TopicSearchHit

sealed class CollectionSearchResult {
    data class TopicsDbItem(
        val hit: TopicSearchHit,
    ) : CollectionSearchResult()

    data class ExclusiveVerseItem(
        val dataset: ExclusiveVersesDataset,
        val verse: ExclusiveVerse,
    ) : CollectionSearchResult()

    data class ScienceTopicItem(
        val item: QuranScienceItem,
    ) : CollectionSearchResult()
}

object ExclusiveVersesSearchProvider {
    const val TOPIC_RESULTS_LIMIT = 40
    private const val EXCLUSIVE_RESULTS_LIMIT = 40
    private const val SCIENCE_RESULTS_LIMIT = 30
    const val COMBINED_RESULTS_LIMIT = 90

    suspend fun search(context: Context, query: String): List<CollectionSearchResult> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) return emptyList()

        val topicsRepo = DatabaseProvider.getTopicsRepository(context)
        val topicMatches = topicsRepo.searchTopicHits(
            query = normalizedQuery,
            limit = TOPIC_RESULTS_LIMIT,
        )
            .map { hit -> CollectionSearchResult.TopicsDbItem(hit) }

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
        }.take(EXCLUSIVE_RESULTS_LIMIT)

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
            .take(SCIENCE_RESULTS_LIMIT)

        return (topicMatches + exclusiveMatches + scienceMatches)
            .take(COMBINED_RESULTS_LIMIT)
    }
}
