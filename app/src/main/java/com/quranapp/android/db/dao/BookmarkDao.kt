package com.quranapp.android.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.quranapp.android.db.entities.BookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {

    // ---------- INSERT ----------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: BookmarkEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bookmarks: List<BookmarkEntity>)

    // ---------- UPDATE ----------
    @Update
    suspend fun updateBookmark(item: BookmarkEntity): Int

    // ---------- DELETE ----------
    @Query(
        """
        DELETE FROM user_bookmarks
        WHERE chapter_no = :chapterNo
          AND from_verse_no = :fromVerse
          AND to_verse_no = :toVerse
    """
    )
    suspend fun removeBookmark(
        chapterNo: Int,
        fromVerse: Int,
        toVerse: Int
    ): Int

    @Query("DELETE FROM user_bookmarks WHERE id IN (:ids)")
    suspend fun removeBookmarksBulk(ids: List<Long>): Int

    @Query("DELETE FROM user_bookmarks")
    suspend fun removeAllBookmarks()

    // ---------- EXISTS ----------
    @Query(
        """
        SELECT COUNT(*) FROM user_bookmarks
        WHERE chapter_no = :chapterNo
          AND from_verse_no = :fromVerse
          AND to_verse_no = :toVerse
    """
    )
    suspend fun countBookmark(
        chapterNo: Int,
        fromVerse: Int,
        toVerse: Int
    ): Int

    @Query(
        """
        SELECT * FROM user_bookmarks
        WHERE chapter_no = :chapterNo
          AND from_verse_no = :fromVerse
          AND to_verse_no = :toVerse
        ORDER BY id DESC
        LIMIT 1
    """
    )
    suspend fun getBookmark(
        chapterNo: Int,
        fromVerse: Int,
        toVerse: Int
    ): BookmarkEntity?

    @Query("SELECT * FROM  user_bookmarks")
    fun getBookmarksFlow(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM user_bookmarks ORDER BY id DESC")
    suspend fun getBookmarks(): List<BookmarkEntity>

    @Query("SELECT * FROM user_bookmarks ORDER BY id DESC")
    fun getBookmarksPaginated(): PagingSource<Int, BookmarkEntity>

    @Query(
        """
        SELECT * FROM user_bookmarks
        WHERE chapter_no = :chapterNo
          AND from_verse_no = :fromVerse
          AND to_verse_no = :toVerse
        ORDER BY id DESC
        LIMIT 1
    """
    )
    fun getBookmarkFlow(
        chapterNo: Int,
        fromVerse: Int,
        toVerse: Int
    ): Flow<BookmarkEntity?>
}