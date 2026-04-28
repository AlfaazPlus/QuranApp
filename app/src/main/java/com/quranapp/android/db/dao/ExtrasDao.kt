package com.quranapp.android.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.quranapp.android.db.entities.extras.SimilarVerseEntity

@Dao
interface ExtrasDao {
    @Query(
        """
        SELECT COUNT(*) FROM similar_verses
        WHERE source_ayah_id = :verseId
        """
    )
    suspend fun countSimilarVerses(verseId: Int): Int

    @Query(
        """
        SELECT * FROM similar_verses
        WHERE source_ayah_id = :verseId
        ORDER BY score DESC
        """
    )
    suspend fun getSimilarVerses(verseId: Int): List<SimilarVerseEntity>
}
