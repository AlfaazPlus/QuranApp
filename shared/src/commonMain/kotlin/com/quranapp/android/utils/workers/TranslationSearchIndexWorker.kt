package com.quranapp.android.utils.workers

import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.db.searchindex.SEARCH_INDEX_CONTENT_VERSION
import com.quranapp.android.db.searchindex.TranslationSearchContentEntity
import com.quranapp.android.search.SearchIndexScheduler
import com.quranapp.android.search.SearchNormalizer
import com.quranapp.android.task.TaskContext
import com.quranapp.android.task.TaskResult
import com.quranapp.android.task.TaskWorker
import com.quranapp.android.utils.miscellenous.stripHtmlTags
import com.quranapp.android.utils.quran.QuranMeta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TranslationSearchIndexWorker() : TaskWorker {
    companion object {
        const val KEY_MODE = "mode"
        const val KEY_SLUG = "slug"
        const val MODE_SLUG = "slug"
        const val MODE_ALL = "all"
        const val MODE_REMOVE = "remove"
    }

    override suspend fun doWork(context: TaskContext): TaskResult = withContext(Dispatchers.IO) {
        when (val mode = context.params.get<String>(KEY_MODE)) {
            MODE_REMOVE -> {
                val slug = context.params.get<String>(KEY_SLUG) ?: return@withContext TaskResult.Failure()

                removeSlug(slug)

                TaskResult.Success()
            }

            MODE_SLUG -> {
                val slug = context.params.get<String>(KEY_SLUG) ?: return@withContext TaskResult.Failure()

                buildIndexForSlugIfNeeded(slug)

                TaskResult.Success()
            }

            MODE_ALL -> {
                DatabaseProvider.getTranslationRepository().let { factory ->
                    val slugs = factory.getBooks().keys

                    for (slug in slugs) {
                        if (context.isCancelled) return@withContext TaskResult.Failure(retry = true)

                        buildIndexForSlugIfNeeded(
                            slug,
                            rebuildFts = false,
                        )
                    }

                    DatabaseProvider.getSearchIndexDatabase()
                        .searchIndexDao()
                        .rebuildFtsIndex()

                    SearchIndexScheduler.markIndexUpToDate()
                }

                TaskResult.Success()
            }

            else -> TaskResult.Failure()
        }
    }

    private suspend fun buildIndexForSlugIfNeeded(
        slug: String,
        rebuildFts: Boolean = true,
    ): Boolean = withContext(Dispatchers.IO) {
        val dao = DatabaseProvider.getSearchIndexDatabase().searchIndexDao()

        DatabaseProvider.getTranslationRepository().let { factory ->
            if (!factory.isDownloaded(slug)) {
                dao.replaceSlugIndex(slug, emptyList(), "", rebuildFts)
                return@let true
            }

            val book = factory.getBook(slug)
            val rowCount = factory.countVerses(slug)
            val fingerprint = "${book.version}|$rowCount|$SEARCH_INDEX_CONTENT_VERSION"

            val existing = dao.getMeta(slug)?.fingerprint
            if (existing == fingerprint) return@let false

            val rows = ArrayList<TranslationSearchContentEntity>(rowCount.coerceAtMost(7000))

            for (row in factory.getVerseTexts(slug)) {
                val plain = row.text.stripHtmlTags(false)
                val norm = SearchNormalizer.normalizeForTranslationIndex(plain)
                if (norm.isBlank()) continue

                val (surahNo, ayahNo) = QuranMeta.getVerseNoFromAyahId(row.ayahId)
                rows.add(
                    TranslationSearchContentEntity(
                        slug = slug,
                        surahNo = surahNo,
                        ayahNo = ayahNo,
                        text = norm,
                    )
                )
            }

            dao.replaceSlugIndex(slug, rows, fingerprint, rebuildFts)
            true
        }
    }

    private suspend fun removeSlug(
        slug: String,
        rebuildFts: Boolean = true,
    ): Boolean = withContext(Dispatchers.IO) {
        val dao = DatabaseProvider.getSearchIndexDatabase().searchIndexDao()
        dao.replaceSlugIndex(slug, emptyList(), "", rebuildFts)
        true
    }
}
