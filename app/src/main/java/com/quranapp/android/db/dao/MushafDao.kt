package com.quranapp.android.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.quranapp.android.db.entities.quran.MushafEntity
import com.quranapp.android.db.entities.quran.MushafMapEntity

@Dao
interface MushafDao {
    @Query("SELECT * FROM mushafs WHERE mushaf_id = :mushafId LIMIT 1")
    suspend fun getMushaf(mushafId: Int): MushafEntity?

    @Query(
        """
        SELECT * FROM mushaf_map
        WHERE mushaf_id = :mushafId AND page_number = :pageNumber
        ORDER BY line_number ASC
        """
    )
    suspend fun getPageLines(mushafId: Int, pageNumber: Int): List<MushafMapEntity>

    /**
     * Juz of the first ayah line on this mushaf page (ordered by line number), from [ayahs].
     */
    @Query(
        """
        SELECT a.juz_no FROM mushaf_map AS m
        INNER JOIN ayahs AS a ON m.start_ayah_id = a.ayah_id
        WHERE m.mushaf_id = :mushafId AND m.page_number = :pageNumber
          AND m.start_ayah_id IS NOT NULL
        ORDER BY m.line_number ASC
        LIMIT 1
        """
    )
    suspend fun getJuzForPage(mushafId: Int, pageNumber: Int): Int?
}
