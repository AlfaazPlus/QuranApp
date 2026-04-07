package com.quranapp.android.db.dao

import androidx.room.Dao
import androidx.room.Query
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
}