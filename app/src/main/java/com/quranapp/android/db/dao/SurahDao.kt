package com.quranapp.android.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.quranapp.android.db.entities.quran.SurahEntity
import com.quranapp.android.db.entities.quran.SurahLocalizationEntity
import com.quranapp.android.db.relations.SurahWithLocalizations
import kotlinx.coroutines.flow.Flow

@Dao
interface SurahDao {
    @Query("SELECT * FROM surahs ORDER BY surah_no")
    fun getAllSurahs(): Flow<List<SurahEntity>>

    @Query("SELECT * FROM surahs ORDER BY surah_no")
    fun getAllSurahsWithLocalizations(): Flow<List<SurahWithLocalizations>>

    @Transaction
    @Query("SELECT * FROM surahs WHERE surah_no IN (:surahNos)")
    suspend fun getSurahsWithLocalizationsByNos(surahNos: List<Int>): List<SurahWithLocalizations>

    @Query("SELECT * FROM surahs WHERE surah_no = :surahNo LIMIT 1")
    suspend fun getSurah(surahNo: Int): SurahEntity?

    @Query("SELECT * FROM surahs WHERE surah_no = :surahNo LIMIT 1")
    suspend fun getSurahWithLocalization(surahNo: Int): SurahWithLocalizations?

    @Query(
        """
        SELECT * FROM surah_localizations
        WHERE surah_no = :surahNo AND lang_code = :langCode
        LIMIT 1
    """
    )
    suspend fun getLocalization(
        surahNo: Int,
        langCode: String
    ): SurahLocalizationEntity?

    @Query(
        """
    SELECT * FROM surah_localizations
    WHERE surah_no IN (:surahNos) AND lang_code = :langCode
    """
    )
    suspend fun getLocalizations(
        surahNos: List<Int>,
        langCode: String
    ): List<SurahLocalizationEntity>
}