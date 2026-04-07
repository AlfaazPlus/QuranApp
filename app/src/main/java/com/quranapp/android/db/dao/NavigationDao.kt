package com.quranapp.android.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.quranapp.android.db.entities.quran.NavigationRangeEntity
import com.quranapp.android.db.entities.quran.NavigationType
import kotlinx.coroutines.flow.Flow

@Dao
interface NavigationDao {
    @Query(
        """
        SELECT * FROM navigation_ranges
        WHERE type = :type
        ORDER BY surah_no
    """
    )
    fun getRanges(
        type: NavigationType,
    ): Flow<List<NavigationRangeEntity>>

    @Query(
        """
        SELECT * FROM navigation_ranges
        WHERE type = :type AND unit_no = :unitNo
        ORDER BY surah_no
    """
    )
    suspend fun getRangesByUnitNo(
        type: NavigationType,
        unitNo: Int
    ): List<NavigationRangeEntity>

    @Query(
        """
        SELECT * FROM navigation_ranges
        WHERE type = :type AND unit_no = :unitNo AND surah_no = :surahNo
        LIMIT 1
    """
    )
    suspend fun getRangeByUnitAndSurahNo(
        type: NavigationType,
        unitNo: Int,
        surahNo: Int
    ): NavigationRangeEntity?
}