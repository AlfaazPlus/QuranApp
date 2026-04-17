package com.quranapp.android.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.quranapp.android.db.entities.wbw.WbwWordEntity

@Dao
interface WbwDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWords(words: List<WbwWordEntity>)

    @Query("DELETE FROM wbw_words WHERE wbw_id = :wbwId")
    suspend fun deleteByWbwId(wbwId: String)

    @Transaction
    suspend fun replaceByWbwId(wbwId: String, words: List<WbwWordEntity>) {
        deleteByWbwId(wbwId)

        if (words.isNotEmpty()) {
            upsertWords(words)
        }
    }

    @Query(
        """
        SELECT * FROM wbw_words
        WHERE ayah_id = :ayahId AND wbw_id = :wbwId
        ORDER BY word_index ASC
        """
    )
    suspend fun getWordsForAyah(
        ayahId: Int,
        wbwId: String
    ): List<WbwWordEntity>

    @Query(
        """
        SELECT * FROM wbw_words
        WHERE wbw_id = :wbwId AND ayah_id IN (:ayahIds)
        ORDER BY ayah_id ASC, word_index ASC
        """
    )
    suspend fun getWordsForAyahs(
        wbwId: String,
        ayahIds: List<Int>,
    ): List<WbwWordEntity>

    @Query(
        """
        SELECT DISTINCT wbw_id
        FROM wbw_words
        WHERE wbw_id IN (:wbwIds)
        """
    )
    suspend fun getDownloadedWbwIds(wbwIds: List<String>): List<String>
}