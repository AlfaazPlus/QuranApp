package com.quranapp.android.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.quranapp.android.db.entities.quran.AyahEntity
import com.quranapp.android.db.relations.AyahWithWords
import kotlinx.coroutines.flow.Flow

@Dao
interface AyahDao {
    @Query(
        """
        SELECT * FROM ayahs
        WHERE surah_no = :surahNo
        ORDER BY ayah_no
    """
    )
    fun getAyahsBySurah(surahNo: Int): Flow<List<AyahEntity>>

    @Query(
        """
        SELECT * FROM ayahs
        WHERE surah_no = :surahNo AND ayah_no = :ayahNo
        LIMIT 1
    """
    )
    suspend fun getAyah(surahNo: Int, ayahNo: Int): AyahEntity?

    @Query(
        """
        SELECT * FROM ayahs
        WHERE juz_no = :juzNo
        ORDER BY surah_no, ayah_no
    """
    )
    suspend fun getAyahsByJuz(juzNo: Int): List<AyahEntity>

    @Query(
        """
        SELECT * FROM ayahs
        WHERE hizb_no = :hizbNo
        ORDER BY surah_no, ayah_no
    """
    )
    suspend fun getAyahsByHizb(hizbNo: Int): List<AyahEntity>

    @Query(
        """
        SELECT * FROM ayahs
        WHERE rub_no = :rubNo
        ORDER BY surah_no, ayah_no
    """
    )
    suspend fun getAyahsByRub(rubNo: Int): List<AyahEntity>

    @Query(
        """
        SELECT * FROM ayahs
        WHERE ayah_id > :startAyahId AND ayah_id < :endAyahId
        ORDER BY ayah_id ASC
        """
    )
    suspend fun getAyahsStrictlyBetween(startAyahId: Int, endAyahId: Int): List<AyahEntity>

    @Query(
        """
        SELECT * FROM ayahs
        WHERE ayah_id = :ayahId
        LIMIT 1
        """
    )
    suspend fun getAyahById(ayahId: Int): AyahEntity?
}