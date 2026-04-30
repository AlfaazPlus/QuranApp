package com.quranapp.android.db.searchindex

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.quranapp.android.db.relations.SearchResultSearchRow
import com.quranapp.android.db.relations.SearchResultVerseRow

@Dao
interface SearchIndexDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranslationRows(rows: List<TranslationSearchContentEntity>)

    @Query("DELETE FROM translation_search_content WHERE slug = :slug")
    suspend fun deleteTranslationRowsForSlug(slug: String)

    @Query("SELECT * FROM translation_index_meta WHERE slug = :slug")
    suspend fun getMeta(slug: String): TranslationIndexMetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeta(meta: TranslationIndexMetaEntity)

    @Query("DELETE FROM translation_index_meta WHERE slug = :slug")
    suspend fun deleteMeta(slug: String)

    @Transaction
    suspend fun replaceSlugIndex(
        slug: String,
        rows: List<TranslationSearchContentEntity>,
        fingerprint: String
    ) {
        deleteTranslationRowsForSlug(slug)

        if (rows.isNotEmpty()) {
            insertTranslationRows(rows)
        }

        if (rows.isEmpty()) {
            deleteMeta(slug)
        } else {
            upsertMeta(TranslationIndexMetaEntity(slug = slug, fingerprint = fingerprint))
        }
    }

    @RawQuery
    suspend fun pageMatchedVersesRaw(query: SupportSQLiteQuery): List<SearchResultVerseRow>

    @RawQuery
    suspend fun rowsForPagedVersesRaw(query: SupportSQLiteQuery): List<SearchResultSearchRow>

    suspend fun pageMatchedVersesFiltered(
        ftsQuery: String,
        slugs: Collection<String>?,
        surahNo: Int?,
        limit: Int,
        offset: Int,
    ): List<SearchResultVerseRow> {
        val args = mutableListOf<Any>()
        val sb = StringBuilder()

        sb.append("SELECT surahNo, ayahNo FROM translation_search_fts WHERE text MATCH ?")
        args += ftsQuery

        if (!slugs.isNullOrEmpty()) {
            sb.append(" AND slug IN (")
            sb.append(slugs.joinToString(",") { "?" })
            sb.append(")")

            args.addAll(slugs)
        }

        if (surahNo != null) {
            sb.append(" AND surahNo = ?")
            args += surahNo
        }

        sb.append(" GROUP BY surahNo, ayahNo ORDER BY surahNo, ayahNo LIMIT ? OFFSET ?")

        args += limit
        args += offset

        val sql = sb.toString()

        return pageMatchedVersesRaw(SimpleSQLiteQuery(sql, args.toTypedArray()))
    }

    suspend fun rowsForPagedVersesFiltered(
        ftsQuery: String,
        keys: List<String>,
        slugs: Collection<String>?,
    ): List<SearchResultSearchRow> {
        val args = mutableListOf<Any>()
        val sb = StringBuilder()

        sb.append(
            "SELECT slug, surahNo, ayahNo, text FROM translation_search_fts " +
                "WHERE text MATCH ?",
        )

        args += ftsQuery

        sb.append(" AND (surahNo || ':' || ayahNo) IN (")
        sb.append(keys.joinToString(",") { "?" })
        sb.append(")")

        args.addAll(keys)

        if (!slugs.isNullOrEmpty()) {
            sb.append(" AND slug IN (")
            sb.append(slugs.joinToString(",") { "?" })
            sb.append(")")

            args.addAll(slugs)
        }

        sb.append(" ORDER BY surahNo, ayahNo, slug")

        val sql = sb.toString()

        return rowsForPagedVersesRaw(SimpleSQLiteQuery(sql, args.toTypedArray()))
    }
}
