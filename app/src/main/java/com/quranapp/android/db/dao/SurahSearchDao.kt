package com.quranapp.android.db.dao

import androidx.room3.Dao
import androidx.room3.Query
import com.quranapp.android.db.relations.SurahNoSearchResult

@Dao
interface SurahSearchDao {
    @Query(
        """
    SELECT DISTINCT surah_no AS surahNo
    FROM surah_search_aliases_fts
    WHERE surah_search_aliases_fts MATCH :query
    LIMIT 20
    """
    )
    suspend fun searchSurahNos(
        query: String,
    ): List<SurahNoSearchResult>

    @Query(
        """
    SELECT DISTINCT surah_no AS surahNo
    FROM surah_search_aliases
    WHERE alias LIKE '%' || :query || '%' COLLATE NOCASE
    ORDER BY
        CASE WHEN alias LIKE :query || '%' COLLATE NOCASE THEN 0 ELSE 1 END,
        LENGTH(alias)
    LIMIT 20
    """
    )
    suspend fun searchSurahNosByAlias(query: String): List<SurahNoSearchResult>
}