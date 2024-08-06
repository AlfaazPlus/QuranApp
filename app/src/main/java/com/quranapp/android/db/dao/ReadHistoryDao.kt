package com.quranapp.android.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.quranapp.android.db.entities.ReadHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadHistoryDao {
    @Query("SELECT * FROM read_history")
    suspend fun getAll(): List<ReadHistory>

    @Query("SELECT * FROM read_history")
    fun getAllFlow(): Flow<List<ReadHistory>>

    @Insert
    suspend fun insert(readHistory: ReadHistory)

    @Delete
    suspend fun delete(readHistory: ReadHistory)

    @Query("DELETE FROM read_history WHERE read_type = :readType " +
            "AND read_style = :readerStyle AND juz_number = :juzNo " +
            "AND chapter_number = :chapterNo AND from_verse_number = :fromVerse " +
            "AND to_verse_number = :toVerse")
    suspend fun deleteDuplicate(readType: Int, readerStyle: Int, juzNo: Int, chapterNo: Int, fromVerse: Int, toVerse: Int)

    @Query("DELETE FROM read_history")
    suspend fun deleteAll()
}

