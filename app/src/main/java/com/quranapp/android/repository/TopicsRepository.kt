package com.quranapp.android.repository

import android.content.Context
import com.quranapp.android.compose.utils.appFallbackLanguageCodes
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.db.TopicsDatabase
import com.quranapp.android.db.dao.TopicsDao
import com.quranapp.android.db.entities.topics.RelationshipType
import com.quranapp.android.db.entities.topics.TopicFlags
import com.quranapp.android.db.relations.topics.TopicRelationshipRow
import com.quranapp.android.db.relations.topics.TopicSummaryRow
import com.quranapp.android.utils.quran.QuranMeta
import com.quranapp.android.utils.reader.factory.QuranTranslationFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TopicsRepository(
    private val context: Context,
    private val database: TopicsDatabase,
) {
    private val topicsDao: TopicsDao get() = database.topicsDao()

    suspend fun getOntologyRootTopics(): List<TopicSummaryRow> = withContext(Dispatchers.IO) {
        topicsDao.getRootTopics(
            flagMask = TopicFlags.ONTOLOGY.dbValue,
            parentType = RelationshipType.ONTOLOGY_PARENT,
            langCode = preferredLanguageCode(),
        )
    }

    suspend fun getThematicRootTopics(): List<TopicSummaryRow> = withContext(Dispatchers.IO) {
        topicsDao.getRootTopics(
            flagMask = TopicFlags.THEMATIC.dbValue,
            parentType = RelationshipType.THEMATIC_PARENT,
            langCode = preferredLanguageCode(),
        )
    }

    suspend fun getTopicById(
        topicId: Int,
        tree: RelationshipType,
    ): TopicSummaryRow? = withContext(Dispatchers.IO) {
        topicsDao.getTopicById(topicId, tree, preferredLanguageCode())
    }

    suspend fun getTopicBySlug(
        slug: String,
        tree: RelationshipType,
    ): TopicSummaryRow? = withContext(Dispatchers.IO) {
        topicsDao.getTopicBySlug(slug, tree, preferredLanguageCode())
    }

    suspend fun getChildTopics(
        topicId: Int,
        tree: RelationshipType,
    ): List<TopicSummaryRow> = withContext(Dispatchers.IO) {
        topicsDao.getChildTopics(topicId, tree, preferredLanguageCode())
    }

    suspend fun getTopicVerseRefs(topicId: Int, limit: Int = 8): List<String> =
        withContext(Dispatchers.IO) {
            topicsDao.getTopicVerses(topicId, limit)
                .toVerseRefs()
        }

    suspend fun getAllTopicVerseRefs(topicId: Int): List<String> =
        withContext(Dispatchers.IO) {
            topicsDao.getAllTopicVerses(topicId).toVerseRefs()
        }

    suspend fun getTopicVersePreviews(topicId: Int, limit: Int = 5): List<TopicVersePreview> =
        withContext(Dispatchers.IO) {
            val rows = topicsDao.getTopicVerses(topicId, limit)
            if (rows.isEmpty()) return@withContext emptyList()

            val primarySlug = ReaderPreferences.primaryTranslationSlug()

            QuranTranslationFactory(context.applicationContext).use { factory ->
                rows.map { row ->
                    val (chapterNo, verseNo) = QuranMeta.getVerseNoFromAyahId(row.ayahId)
                    TopicVersePreview(
                        chapterNo = chapterNo,
                        verseNo = verseNo,
                        translation = factory
                            .getTranslationsSingleSlugVerse(primarySlug, chapterNo, verseNo)
                            ?.text
                            .orEmpty(),
                    )
                }
            }
        }

    suspend fun getTopicRelationships(
        topicId: Int,
        tree: RelationshipType,
        limit: Int = 8,
    ): List<TopicRelationshipRow> = withContext(Dispatchers.IO) {
        topicsDao.getTopicRelationships(
            topicId = topicId,
            parentType = tree,
            langCode = preferredLanguageCode(),
            limit = limit,
        )
    }

    private fun preferredLanguageCode(): String {
        return appFallbackLanguageCodes().firstOrNull().orEmpty().ifBlank { "en" }
    }

    private fun List<com.quranapp.android.db.relations.topics.TopicVerseRow>.toVerseRefs(): List<String> {
        return map { QuranMeta.getVerseNoFromAyahId(it.ayahId) }
            .map { (chapterNo, verseNo) -> "$chapterNo:$verseNo" }
    }
}

data class TopicVersePreview(
    val chapterNo: Int,
    val verseNo: Int,
    val translation: String,
)
