package com.quranapp.android.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.quranapp.android.db.entities.quran.MushafEntity
import com.quranapp.android.db.entities.quran.MushafMapEntity
import com.quranapp.android.db.projections.AyahPageProjection
import com.quranapp.android.db.projections.PageHizbProjection
import com.quranapp.android.db.projections.PageJuzProjection

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

    @Query(
        """
        SELECT MIN(m.page_number) FROM mushaf_map AS m
        WHERE m.mushaf_id = :mushafId AND m.surah_no = :chapterNo
        """
    )
    suspend fun getFirstPageOfChapter(mushafId: Int, chapterNo: Int): Int?

    @Query(
        """
        SELECT m.page_number FROM mushaf_map AS m
        WHERE m.mushaf_id = :mushafId
          AND :ayahId BETWEEN m.start_ayah_id AND m.end_ayah_id
        LIMIT 1
        """
    )
    suspend fun getPageForVerse(mushafId: Int, ayahId: Int): Int?

    @Query(
        """
        SELECT MIN(m.page_number) FROM mushaf_map AS m
        INNER JOIN ayahs AS a ON m.start_ayah_id = a.ayah_id
        WHERE m.mushaf_id = :mushafId AND a.juz_no = :juzNo
          AND m.start_ayah_id IS NOT NULL
        """
    )
    suspend fun getFirstPageOfJuz(mushafId: Int, juzNo: Int): Int?

    @Query(
        """
        SELECT MIN(m.page_number) FROM mushaf_map AS m
        INNER JOIN ayahs AS a ON m.start_ayah_id = a.ayah_id
        WHERE m.mushaf_id = :mushafId AND a.hizb_no = :hizbNo
          AND m.start_ayah_id IS NOT NULL
        """
    )
    suspend fun getFirstPageOfHizb(mushafId: Int, hizbNo: Int): Int?

    @Query(
        """
        SELECT MIN(
            CASE
                WHEN m.start_ayah_id > :surahBase
                THEN m.start_ayah_id - :surahBase
                ELSE 1
            END
        ) FROM mushaf_map AS m
        WHERE m.mushaf_id = :mushafId AND m.page_number = :pageNo
          AND m.end_ayah_id > :surahBase
          AND m.start_ayah_id < :surahBase + 1000
          AND m.start_ayah_id IS NOT NULL AND m.end_ayah_id IS NOT NULL
        """
    )
    suspend fun getFirstVerseOnPage(mushafId: Int, pageNo: Int, surahBase: Int): Int?

    @Query(
        """
        SELECT MIN(m.start_ayah_id) FROM mushaf_map AS m
        WHERE m.mushaf_id = :mushafId AND m.page_number = :pageNo
          AND m.start_ayah_id IS NOT NULL
        """
    )
    suspend fun getFirstAyahIdOnPage(mushafId: Int, pageNo: Int): Int?

    @Query(
        """
        SELECT * FROM mushaf_map
        WHERE mushaf_id = :mushafId AND page_number IN (:pageNumbers)
        ORDER BY page_number ASC, line_number ASC
        """
    )
    suspend fun getPageLinesForPages(
        mushafId: Int,
        pageNumbers: List<Int>,
    ): List<MushafMapEntity>

    /**
     * Juz from the first ayah line per page (by [MushafMapEntity.lineNumber]).
     */
    @Query(
        """
        SELECT m.page_number, a.juz_no
        FROM mushaf_map m
        INNER JOIN ayahs a ON m.start_ayah_id = a.ayah_id
        WHERE m.mushaf_id = :mushafId AND m.page_number IN (:pageNumbers)
          AND m.start_ayah_id IS NOT NULL
          AND m.line_number = (
            SELECT MIN(m2.line_number) FROM mushaf_map m2
            WHERE m2.mushaf_id = :mushafId AND m2.page_number = m.page_number
              AND m2.start_ayah_id IS NOT NULL
          )
        """
    )
    suspend fun getJuzForPages(
        mushafId: Int,
        pageNumbers: List<Int>,
    ): List<PageJuzProjection>

    /**
     * All hizb numbers present on each mushaf page.
     */
    @Query(
        """
        SELECT DISTINCT m.page_number, a.hizb_no
        FROM mushaf_map m
        INNER JOIN ayahs a ON m.start_ayah_id = a.ayah_id
        WHERE m.mushaf_id = :mushafId AND m.page_number IN (:pageNumbers)
          AND m.start_ayah_id IS NOT NULL
        ORDER BY m.page_number ASC, a.hizb_no ASC
        """
    )
    suspend fun getHizbForPages(
        mushafId: Int,
        pageNumbers: List<Int>,
    ): List<PageHizbProjection>

    @Query(
        """
        SELECT a.ayah_id AS ayah_id, MIN(m.page_number) AS page_number
        FROM ayahs a
        INNER JOIN mushaf_map m ON m.mushaf_id = :mushafId
            AND a.ayah_id BETWEEN m.start_ayah_id AND m.end_ayah_id
        WHERE a.ayah_id IN (:ayahIds)
        GROUP BY a.ayah_id
        """
    )
    suspend fun getPagesForAyahIds(
        mushafId: Int,
        ayahIds: List<Int>,
    ): List<AyahPageProjection>
}
