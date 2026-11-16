package com.quranapp.android.db.searchindex

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.RawQuery
import androidx.room3.RoomRawQuery
import androidx.room3.Transaction
import com.quranapp.android.db.relations.SearchResultSearchRow

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

    @Query("INSERT INTO translation_search_fts(translation_search_fts) VALUES('rebuild')")
    suspend fun rebuildFtsIndex()

    @Transaction
    suspend fun replaceSlugIndex(
        slug: String,
        rows: List<TranslationSearchContentEntity>,
        fingerprint: String,
        rebuildFts: Boolean = true,
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

        if (rebuildFts) {
            rebuildFtsIndex()
        }
    }

    @RawQuery
    suspend fun pageMatchedRowsRaw(query: RoomRawQuery): List<SearchResultSearchRow>

    suspend fun pageMatchedRows(
        ftsQuery: String,
        slugs: Collection<String>?,
        limit: Int,
        offset: Int,
    ): List<SearchResultSearchRow> {
        val args = mutableListOf<Any>()
        val sql = buildString {
            append("WITH matched AS MATERIALIZED (")
            append("SELECT slug, surahNo, ayahNo, text FROM translation_search_fts WHERE text MATCH ?")
            args += ftsQuery

            if (!slugs.isNullOrEmpty()) {
                append(" AND slug IN (")
                append(slugs.joinToString(",") { "?" })
                append(")")
                args.addAll(slugs)
            }

            append(") SELECT slug, surahNo, ayahNo, text FROM matched ")
            append("WHERE surahNo || ':' || ayahNo IN (")
            append("SELECT surahNo || ':' || ayahNo FROM matched ")
            append("GROUP BY surahNo, ayahNo ORDER BY surahNo, ayahNo LIMIT ? OFFSET ?")
            append(") ORDER BY surahNo, ayahNo, slug")
            args += limit
            args += offset
        }

        return pageMatchedRowsRaw(buildRawQuery(sql, args))
    }
}

private fun buildRawQuery(sql: String, args: List<Any>): RoomRawQuery {
    return RoomRawQuery(sql) { statement ->
        args.forEachIndexed { index, arg ->
            val bindIndex = index + 1
            when (arg) {
                is String -> statement.bindText(bindIndex, arg)
                is Int -> statement.bindInt(bindIndex, arg)
                is Long -> statement.bindLong(bindIndex, arg)
                is Double -> statement.bindDouble(bindIndex, arg)
                is Float -> statement.bindFloat(bindIndex, arg)
                is Boolean -> statement.bindBoolean(bindIndex, arg)
                is ByteArray -> statement.bindBlob(bindIndex, arg)
                else -> statement.bindText(bindIndex, arg.toString())
            }
        }
    }
}
