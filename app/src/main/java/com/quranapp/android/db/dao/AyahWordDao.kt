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
        INNER JOIN ayahs a ON a.surah_no = :chapterNo AND a.ayah_no = :verseNo
        WHERE aw.ayah_id = a.ayah_id AND s.code = :scriptCode
        ORDER BY aw.word_index
    """
    )
    suspend fun getWordsForAyah(
        chapterNo: Int,
        verseNo: Int,
        scriptCode: String
    ): List<AyahWordEntity>

    @Query(
        """
        SELECT aw.*
        FROM ayah_words aw
        INNER JOIN scripts s ON aw.script_id = s.script_id
        WHERE aw.ayah_id = :ayahId AND s.code = :scriptCode
        ORDER BY aw.word_index
    """
    )
    suspend fun getWordsForAyahById(
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

    @Query(
        """
        SELECT aw.*
        FROM ayah_words aw
        INNER JOIN scripts s ON aw.script_id = s.script_id
        WHERE aw.ayah_id IN (:ayahIds) AND s.code = :scriptCode
        ORDER BY aw.ayah_id ASC, aw.word_index ASC
        """
    )
    suspend fun getWordsForAyahs(
        ayahIds: List<Int>,
        scriptCode: String,
    ): List<AyahWordEntity>
}