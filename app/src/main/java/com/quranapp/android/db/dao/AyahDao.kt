package com.quranapp.android.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.quranapp.android.db.entities.quran.AyahEntity
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

    @Query(
        """
        SELECT * FROM ayahs
        WHERE surah_no = :surahNo AND ayah_no BETWEEN :fromAyahNo AND :toAyahNo
        ORDER BY ayah_no ASC
        """
    )
    suspend fun getAyahsInRange(
        surahNo: Int,
        fromAyahNo: Int,
        toAyahNo: Int,
    ): List<AyahEntity>

    @Query(
        """
        SELECT * FROM ayahs
        WHERE ayah_id IN (:ayahIds)
        """
    )
    suspend fun getAyahsByIds(ayahIds: List<Int>): List<AyahEntity>

    @Query(
        """
        SELECT DISTINCT surah_no FROM ayahs
        WHERE IFNULL(sajdah_type, 0) != 0
        ORDER BY surah_no
        """
    )
    suspend fun getDistinctSurahNosWithSajdah(): List<Int>

    @Query(
        """
        SELECT DISTINCT juz_no FROM ayahs
        WHERE surah_no = :surahNo
        ORDER BY juz_no
        """
    )
    suspend fun getDistinctJuzNosForSurah(surahNo: Int): List<Int>
}