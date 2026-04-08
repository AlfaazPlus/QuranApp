package com.quranapp.android.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quranapp.android.db.entities.ReadHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ReadHistoryEntity): Long

    @Query("SELECT * FROM read_history ORDER BY datetime DESC LIMIT 1")
    suspend fun getLatest(): ReadHistoryEntity?

    @Query("SELECT * FROM read_history ORDER BY datetime DESC")
    fun getAllFlow(): Flow<List<ReadHistoryEntity>>

    @Query("SELECT * FROM read_history ORDER BY datetime DESC LIMIT :limit")
    fun getAllFlow(limit: Int): Flow<List<ReadHistoryEntity>>

    @Query(
        """
        DELETE FROM read_history
        WHERE read_type = :readType
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
        WHERE id NOT IN (
            SELECT id FROM read_history ORDER BY datetime DESC LIMIT :keepCount
        )
        """
    )
    suspend fun trimToSize(keepCount: Int)

    @Query("DELETE FROM read_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM read_history")
    suspend fun deleteAll()
}
