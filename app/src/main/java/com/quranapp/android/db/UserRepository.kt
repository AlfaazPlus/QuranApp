package com.quranapp.android.db

import android.content.Context
import android.widget.Toast
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.quranapp.android.R
import com.quranapp.android.db.entities.BookmarkEntity
import kotlinx.coroutines.flow.Flow

class UserRepository(
    private val context: Context,
    private val database: UserDatabase
) {
    private val bookmarkDao get() = database.bookmarkDao()

    suspend fun addMultipleBookmarks(bookmarks: List<BookmarkEntity>) {
        bookmarkDao.insertAll(bookmarks)
    }

    suspend fun addToBookmark(
        chapterNo: Int,
        fromVerse: Int,
        toVerse: Int,
        note: String?,
    ) {
        if (isBookmarked(chapterNo, fromVerse, toVerse)) {
            Toast.makeText(context, R.string.strMsgBookmarkAddedAlready, Toast.LENGTH_SHORT).show()
            return
        }

        val entity = BookmarkEntity(
            chapterNo = chapterNo,
            fromVerseNo = fromVerse,
            toVerseNo = toVerse,
            note = note
        )

        val rowId = bookmarkDao.insert(entity)
        val inserted = rowId != -1L

        val msg = if (inserted) {
            R.string.strMsgBookmarkAdded
        } else {
            R.string.strMsgBookmarkAddFailed
        }

        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    suspend fun updateBookmark(
        chapterNo: Int,
        fromVerse: Int,
        toVerse: Int,
        note: String?,
    ) {
        val existing = bookmarkDao.getBookmark(chapterNo, fromVerse, toVerse) ?: return
        bookmarkDao.updateBookmark(existing.copy(note = note))
    }

    suspend fun removeFromBookmark(
        chapterNo: Int,
        fromVerse: Int,
        toVerse: Int,
    ): Boolean {
        val rowsAffected = bookmarkDao.removeBookmark(chapterNo, fromVerse, toVerse)
        val deleted = rowsAffected >= 1

        val msg = if (deleted) {
            R.string.strMsgBookmarkRemoved
        } else {
            R.string.strMsgBookmarkRemoveFailed
        }

        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

        return deleted
    }

    suspend fun removeBookmarksBulk(
        ids: LongArray,
    ): Boolean {
        val rowsAffected = bookmarkDao.removeBookmarksBulk(ids.toList())
        val deleted = rowsAffected >= 1

        val msg = if (deleted) {
            R.string.strMsgBookmarkRemoved
        } else {
            R.string.strMsgBookmarkRemoveFailed
        }

        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

        return deleted
    }

    suspend fun isBookmarked(
        chapterNo: Int,
        fromVerse: Int,
        toVerse: Int
    ): Boolean {
        return bookmarkDao.countBookmark(chapterNo, fromVerse, toVerse) > 0
    }

    suspend fun removeAllBookmarks() {
        bookmarkDao.removeAllBookmarks()
    }

    suspend fun getBookmark(
        chapNo: Int,
        fromVerse: Int,
        toVerse: Int
    ): BookmarkEntity? {
        return bookmarkDao.getBookmark(chapNo, fromVerse, toVerse)
    }

    suspend fun getBookmarks(): ArrayList<BookmarkEntity> {
        return ArrayList(bookmarkDao.getBookmarks())
    }

    fun getBookmarksFlow(): Flow<List<BookmarkEntity>> {
        return bookmarkDao.getBookmarksFlow()
    }

    fun getBookmarksPagingFlow(
        pageSize: Int = 20
    ): Flow<PagingData<BookmarkEntity>> {
        return Pager(
            config = PagingConfig(
                pageSize = pageSize,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { bookmarkDao.getBookmarksPaginated() }
        ).flow
    }

    fun getBookmarkFlow(
        chapterNo: Int,
        fromVerse: Int,
        toVerse: Int
    ): Flow<BookmarkEntity?> {
        return bookmarkDao.getBookmarkFlow(chapterNo, fromVerse, toVerse)
    }
}