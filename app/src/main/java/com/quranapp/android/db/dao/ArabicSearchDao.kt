package com.quranapp.android.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.quranapp.android.db.entities.quran.ArabicSearchFtsEntity

@Dao
interface ArabicSearchDao {
    @Query(
        """
        SELECT ayah_id, text FROM arabic_search
        WHERE arabic_search MATCH :ftsQuery
        ORDER BY ayah_id
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun pageMatchedAyahs(
        ftsQuery: String,
        limit: Int,
        offset: Int,
    ): List<ArabicSearchFtsEntity>
}
