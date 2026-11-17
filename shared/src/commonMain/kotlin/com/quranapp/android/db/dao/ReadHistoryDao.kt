package com.quranapp.android.db.dao

import androidx.paging.PagingSource
import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.quranapp.android.db.entities.user.ReadHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ReadHistoryEntity): Long

    @Query("SELECT * FROM read_history WHERE is_main = 0 ORDER BY datetime DESC LIMIT 1")
    suspend fun getLatestNonMain(): ReadHistoryEntity?

    @Query("SELECT * FROM read_history WHERE is_main = 1 ORDER BY datetime DESC LIMIT 1")
    fun getMainFlow(): Flow<ReadHistoryEntity?>

    @Query("SELECT * FROM read_history WHERE is_main = 1 ORDER BY datetime DESC LIMIT 1")
    suspend fun getMain(): ReadHistoryEntity?

    @Query(
        """
        UPDATE read_history
        SET from_verse_no = :fromVerseNo,
            to_verse_no = :toVerseNo,
            datetime = :datetime
        WHERE id = :id AND is_main = 0
        """
    )
    suspend fun updateVersePosition(
        id: Long,
        fromVerseNo: Int,
        toVerseNo: Int,
        datetime: Long,
    )

    @Query(
        """
        UPDATE read_history
        SET read_type = :readType,
            reader_mode = :readerMode,
            division_no = :divisionNo,
            chapter_no = :chapterNo,
            from_verse_no = :fromVerseNo,
            to_verse_no = :toVerseNo,
            mushaf_code = :mushafCode,
            mushaf_variant = :mushafVariant,
            page_no = :pageNo,
            datetime = :datetime
        WHERE id = :id AND is_main = 1
        """
    )
    suspend fun updateMainSession(
        id: Long,
        readType: String,
        readerMode: String,
        divisionNo: Int,
        chapterNo: Int,
        fromVerseNo: Int,
        toVerseNo: Int,
        mushafCode: String?,
        mushafVariant: String?,
        pageNo: Int?,
        datetime: Long,
    ): Int

    @Query("UPDATE read_history SET is_main = 0 WHERE is_main = 1")
    suspend fun clearMain()

    @Query("UPDATE read_history SET is_main = 1 WHERE id = :id")
    suspend fun setMain(id: Long): Int

    @Query("SELECT * FROM read_history ORDER BY is_main DESC, datetime DESC LIMIT :limit")
    fun getFlow(limit: Int): Flow<List<ReadHistoryEntity>>

    @Query("SELECT * FROM read_history WHERE is_main = 0 ORDER BY datetime DESC, id DESC")
    fun getNonMainPaginated(): PagingSource<Int, ReadHistoryEntity>

    @Query(
        """
        DELETE FROM read_history
        WHERE is_main = 0
          AND read_type = :readType
          AND reader_mode = :readerMode
          AND division_no = :divisionNo
          AND chapter_no = :chapterNo
          AND from_verse_no = :fromVerseNo
          AND to_verse_no = :toVerseNo
        """
    )
    suspend fun deleteDuplicate(
        readType: String,
        readerMode: String,
        divisionNo: Int,
        chapterNo: Int,
        fromVerseNo: Int,
        toVerseNo: Int,
    )

    @Query(
        """
        DELETE FROM read_history
        WHERE is_main = 0
          AND id NOT IN (
              SELECT id FROM read_history WHERE is_main = 0 ORDER BY datetime DESC LIMIT :keepCount
          )
        """
    )
    suspend fun trimToSize(keepCount: Int)

    @Query("DELETE FROM read_history WHERE id = :id AND is_main = 0")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM read_history WHERE is_main = 0")
    suspend fun deleteAllNonMain()
}
