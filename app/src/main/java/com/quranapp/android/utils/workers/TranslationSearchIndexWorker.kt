package com.quranapp.android.utils.workers

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.quranapp.android.R
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.db.searchindex.SEARCH_INDEX_CONTENT_VERSION
import com.quranapp.android.db.searchindex.TranslationSearchContentEntity
import com.quranapp.android.utils.quran.QuranMeta
import com.quranapp.android.search.SearchNormalizer
import com.quranapp.android.search.SearchIndexScheduler
import com.quranapp.android.utils.app.NotificationUtils
import com.quranapp.android.repository.TranslationRepository
import com.quranapp.android.utils.univ.StringUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TranslationSearchIndexWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    companion object {
        const val KEY_MODE = "mode"
        const val KEY_SLUG = "slug"
        const val MODE_SLUG = "slug"
        const val MODE_ALL = "all"
        const val MODE_REMOVE = "remove"
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = NotificationCompat.Builder(
            applicationContext,
            NotificationUtils.CHANNEL_ID_DEFAULT
        )
            .setContentTitle("Indexing translations data")
            .setContentText("Preparing...")
            .setSmallIcon(R.drawable.dr_logo)
            .setOngoing(true)
            .build()

        return ForegroundInfo(1001, notification)
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        when (val mode = inputData.getString(KEY_MODE)) {
            MODE_REMOVE -> {
                val slug = inputData.getString(KEY_SLUG) ?: return@withContext Result.failure()

                removeSlug(applicationContext, slug)

                Result.success()
            }

            MODE_SLUG -> {
                val slug = inputData.getString(KEY_SLUG) ?: return@withContext Result.failure()

                buildIndexForSlugIfNeeded(applicationContext, slug)

                Result.success()
            }

            MODE_ALL -> {
                DatabaseProvider.getTranslationRepository(applicationContext).let { factory ->
                    val slugs = factory.getBooks().keys

                    for (slug in slugs) {
                        if (isStopped) return@withContext Result.retry()

                        buildIndexForSlugIfNeeded(
                            applicationContext,
                            slug,
                            rebuildFts = false,
                        )
                    }

                    DatabaseProvider.getSearchIndexDatabase(applicationContext)
                        .searchIndexDao()
                        .rebuildFtsIndex()

                    SearchIndexScheduler.markIndexUpToDate(applicationContext)
                }

                Result.success()
            }

            else -> Result.failure()
        }
    }

    private suspend fun buildIndexForSlugIfNeeded(
        context: Context,
        slug: String,
        rebuildFts: Boolean = true,
    ): Boolean = withContext(Dispatchers.IO) {
            val dao = DatabaseProvider.getSearchIndexDatabase(context).searchIndexDao()

            DatabaseProvider.getTranslationRepository(context).let { factory ->
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
                    val plain = StringUtils.removeHTML(row.text, false)
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
        context: Context,
        slug: String,
        rebuildFts: Boolean = true,
    ): Boolean = withContext(Dispatchers.IO) {
        val dao = DatabaseProvider.getSearchIndexDatabase(context).searchIndexDao()
        dao.replaceSlugIndex(slug, emptyList(), "", rebuildFts)
        true
    }

}
