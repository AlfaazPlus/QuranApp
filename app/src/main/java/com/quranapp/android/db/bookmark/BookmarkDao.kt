package com.quranapp.android.db.bookmark

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quranapp.android.db.bookmark.BookmarkContract.BookmarkEntry.COL_CHAPTER_NO
import com.quranapp.android.db.bookmark.BookmarkContract.BookmarkEntry.COL_DATETIME
import com.quranapp.android.db.bookmark.BookmarkContract.BookmarkEntry.COL_FROM_VERSE_NO
import com.quranapp.android.db.bookmark.BookmarkContract.BookmarkEntry.COL_TO_VERSE_NO
import com.quranapp.android.db.bookmark.BookmarkContract.BookmarkEntry.TABLE_NAME
import com.quranapp.android.db.bookmark.BookmarkContract.BookmarkEntry._ID
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {

    // ---------- INSERT ----------
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(bookmark: BookmarkEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(bookmarks: List<BookmarkEntity>)

    // ---------- UPDATE ----------
    @Query(
        """
        UPDATE $TABLE_NAME
        SET $COL_DATETIME = :dateTime,
            ${BookmarkContract.BookmarkEntry.COL_NOTE} = :note
        WHERE $COL_CHAPTER_NO = :chapterNo
          AND $COL_FROM_VERSE_NO = :fromVerse
          AND $COL_TO_VERSE_NO = :toVerse
    """
    )
    suspend fun updateBookmark(
        chapterNo: Int,
        fromVerse: Int,
        toVerse: Int,
        note: String?,
        dateTime: String?
    ): Int

    // ---------- DELETE ----------
    @Query(
        """
        DELETE FROM $TABLE_NAME
        WHERE $COL_CHAPTER_NO = :chapterNo
          AND $COL_FROM_VERSE_NO = :fromVerse
          AND $COL_TO_VERSE_NO = :toVerse
    """
    )
    suspend fun removeBookmark(
        chapterNo: Int,
        fromVerse: Int,
        toVerse: Int
    ): Int

    @Query("DELETE FROM $TABLE_NAME WHERE $_ID IN (:ids)")
    suspend fun removeBookmarksBulk(ids: List<Long>): Int

    @Query("DELETE FROM $TABLE_NAME")
    suspend fun removeAllBookmarks()

    // ---------- EXISTS ----------
    @Query(
        """
        SELECT COUNT(*) FROM $TABLE_NAME
        WHERE $COL_CHAPTER_NO = :chapterNo
          AND $COL_FROM_VERSE_NO = :fromVerse
          AND $COL_TO_VERSE_NO = :toVerse
    """
    )
    suspend fun countBookmark(
        chapterNo: Int,
        fromVerse: Int,
        toVerse: Int
    ): Int

    @Query(
        """
        SELECT * FROM $TABLE_NAME
        WHERE $COL_CHAPTER_NO = :chapterNo
          AND $COL_FROM_VERSE_NO = :fromVerse
          AND $COL_TO_VERSE_NO = :toVerse
        ORDER BY $_ID DESC
        LIMIT 1
    """
    )
    suspend fun getBookmark(
        chapterNo: Int,
        fromVerse: Int,
        toVerse: Int
    ): BookmarkEntity?

    @Query("SELECT * FROM  $TABLE_NAME")
    fun getBookmarksFlow(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM $TABLE_NAME ORDER BY $_ID DESC")
    suspend fun getBookmarks(): List<BookmarkEntity>

    @Query("SELECT * FROM $TABLE_NAME ORDER BY $_ID DESC")
    fun getBookmarksPaginated(): PagingSource<Int, BookmarkEntity>

    @Query(
        """
        SELECT * FROM $TABLE_NAME
        WHERE $COL_CHAPTER_NO = :chapterNo
          AND $COL_FROM_VERSE_NO = :fromVerse
          AND $COL_TO_VERSE_NO = :toVerse
        ORDER BY $_ID DESC
        LIMIT 1
    """
    )
    fun getBookmarkFlow(
        chapterNo: Int,
        fromVerse: Int,
        toVerse: Int
    ): Flow<BookmarkEntity?>
}