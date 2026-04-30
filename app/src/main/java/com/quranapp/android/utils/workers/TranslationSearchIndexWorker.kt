package com.quranapp.android.utils.workers

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.quranapp.android.R
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.db.searchindex.SEARCH_INDEX_DB_VERSION
import com.quranapp.android.db.searchindex.TranslationSearchContentEntity
import com.quranapp.android.db.translation.QuranTranslContract.QuranTranslEntry.COL_CHAPTER_NO
import com.quranapp.android.db.translation.QuranTranslContract.QuranTranslEntry.COL_TEXT
import com.quranapp.android.db.translation.QuranTranslContract.QuranTranslEntry.COL_VERSE_NO
import com.quranapp.android.db.translation.QuranTranslDBHelper
import com.quranapp.android.search.SearchNormalizer
import com.quranapp.android.utils.app.NotificationUtils
import com.quranapp.android.utils.reader.factory.QuranTranslationFactory
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
                QuranTranslationFactory(applicationContext).use { factory ->
                    val slugs = factory.getAvailableTranslationBooksInfo().keys

                    for (slug in slugs) {
                        if (isStopped) break

                        if (factory.isTranslationDownloaded(slug)) {
                            buildIndexForSlugIfNeeded(
                                applicationContext,
                                slug,
                            )
                        } else {
                            removeSlug(applicationContext, slug)
                        }
                    }
                }

                Result.success()
            }

            else -> Result.failure()
        }
    }

    private suspend fun buildIndexForSlugIfNeeded(context: Context, slug: String): Unit =
        withContext(Dispatchers.IO) {
            val dao = DatabaseProvider.getSearchIndexDatabase(context).searchIndexDao()

            QuranTranslationFactory(context).use { factory ->
                if (!factory.isTranslationDownloaded(slug)) {
                    dao.replaceSlugIndex(slug, emptyList(), "")
                    return@use
                }

                val book = factory.getTranslationBookInfo(slug)
                val rowCount = countRows(factory, slug)
                val fingerprint = "${book.lastUpdated}|$rowCount|${SEARCH_INDEX_DB_VERSION}"

                val existing = dao.getMeta(slug)?.fingerprint
                if (existing == fingerprint) return@use

                val rows = ArrayList<TranslationSearchContentEntity>(rowCount.coerceAtMost(7000))
                val table = QuranTranslDBHelper.escapeTableName(slug)

                val q =
                    "SELECT $COL_CHAPTER_NO, $COL_VERSE_NO, $COL_TEXT FROM $table ORDER BY $COL_CHAPTER_NO ASC, $COL_VERSE_NO ASC"

                factory.dbHelper.readableDatabase.rawQuery(q, null).use { c ->
                    while (c.moveToNext()) {
                        val surah = c.getInt(0)
                        val ayah = c.getInt(1)
                        val raw = c.getString(2) ?: ""
                        val plain = StringUtils.removeHTML(raw, false)

                        val norm = SearchNormalizer.normalize(plain)
                        if (norm.isBlank()) continue

                        rows.add(
                            TranslationSearchContentEntity(
                                slug = slug,
                                surahNo = surah,
                                ayahNo = ayah,
                                text = norm,
                            )
                        )
                    }
                }

                dao.replaceSlugIndex(slug, rows, fingerprint)
            }
        }

    private suspend fun removeSlug(context: Context, slug: String) = withContext(Dispatchers.IO) {
        val dao = DatabaseProvider.getSearchIndexDatabase(context).searchIndexDao()
        dao.replaceSlugIndex(slug, emptyList(), "")
    }

    private fun countRows(factory: QuranTranslationFactory, slug: String): Int {
        val table = QuranTranslDBHelper.escapeTableName(slug)

        factory.dbHelper.readableDatabase.rawQuery("SELECT COUNT(*) FROM $table", null).use { c ->
            if (c.moveToFirst()) return c.getInt(0)
        }

        return 0
    }
}
