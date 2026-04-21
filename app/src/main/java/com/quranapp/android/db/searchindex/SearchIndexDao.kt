package com.quranapp.android.db.searchindex

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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

    @Query(
        """
        SELECT surahNo, ayahNo
        FROM translation_search_fts
        WHERE translation_search_fts MATCH :ftsQuery
        GROUP BY surahNo, ayahNo
        ORDER BY surahNo, ayahNo
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun pageMatchedVerses(
        ftsQuery: String,
        limit: Int,
        offset: Int,
    ): List<SearchResultVerseRow>

    @Query(
        """
    SELECT slug, surahNo, ayahNo, text
    FROM translation_search_fts
    WHERE translation_search_fts MATCH :ftsQuery
    AND (surahNo || ':' || ayahNo) IN (:keys)
    ORDER BY surahNo, ayahNo, slug
    """
    )
    suspend fun rowsForPagedVerses(
        ftsQuery: String,
        keys: List<String>,
    ): List<SearchResultSearchRow>
}
