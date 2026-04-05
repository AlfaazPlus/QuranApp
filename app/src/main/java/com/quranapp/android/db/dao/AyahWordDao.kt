package com.quranapp.android.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.quranapp.android.db.entities.quran.AyahWordEntity

@Dao
interface AyahWordDao {
    @Query(
        """
        SELECT aw.*
        FROM ayah_words aw
        INNER JOIN scripts s ON aw.script_id = s.script_id
        WHERE aw.ayah_id = :ayahId AND s.code = :scriptCode
        ORDER BY aw.word_index
    """
    )
    suspend fun getWordsForAyah(
        ayahId: Int,
        scriptCode: String
    ): List<AyahWordEntity>

    @Query(
        """
        SELECT aw.*
        FROM ayah_words aw
        INNER JOIN scripts s ON aw.script_id = s.script_id
        WHERE aw.ayah_id = :ayahId
          AND s.code = :scriptCode
          AND aw.word_index BETWEEN :startIndex AND :endIndex
        ORDER BY aw.word_index
    """
    )
    suspend fun getWordsForAyahByIndexRange(
        ayahId: Int,
        scriptCode: String,
        startIndex: Int,
        endIndex: Int
    ): List<AyahWordEntity>

    @Query(
        """
    SELECT MAX(aw.word_index)
    FROM ayah_words aw
    INNER JOIN scripts s ON aw.script_id = s.script_id
    WHERE aw.ayah_id = :ayahId 
      AND s.code = :scriptCode
    """
    )
    suspend fun getLastWordIndexForAyah(
        ayahId: Int,
        scriptCode: String
    ): Int?
}