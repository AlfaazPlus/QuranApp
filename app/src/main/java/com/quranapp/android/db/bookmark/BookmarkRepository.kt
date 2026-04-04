package com.quranapp.android.db.bookmark

import android.content.Context
import android.widget.Toast
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.quranapp.android.R
import com.quranapp.android.utils.univ.DateUtils
import kotlinx.coroutines.flow.Flow


class BookmarkRepository(
    private val context: Context,
    private val database: BookmarkDatabase
) {
    private val dao get() = database.bookmarkDao()

    suspend fun addMultipleBookmarks(bookmarks: List<BookmarkEntity>) {
        dao.insertAll(bookmarks)
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

        val dateTime = DateUtils.dateTimeNow

        val entity = BookmarkEntity(
            chapterNo = chapterNo,
            fromVerseNo = fromVerse,
            toVerseNo = toVerse,
            dateTime = dateTime,
            note = note
        )

        val rowId = dao.insert(entity)
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
        dao.updateBookmark(
            chapterNo = chapterNo,
            fromVerse = fromVerse,
            toVerse = toVerse,
            note = note,
            dateTime = DateUtils.dateTimeNow
        )
    }

    suspend fun removeFromBookmark(
        chapterNo: Int,
        fromVerse: Int,
        toVerse: Int,
    ): Boolean {
        val rowsAffected = dao.removeBookmark(chapterNo, fromVerse, toVerse)
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
        val rowsAffected = dao.removeBookmarksBulk(ids.toList())
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
        return dao.countBookmark(chapterNo, fromVerse, toVerse) > 0
    }

    suspend fun removeAllBookmarks() {
        dao.removeAllBookmarks()
    }

    suspend fun getBookmark(
        chapNo: Int,
        fromVerse: Int,
        toVerse: Int
    ): BookmarkEntity? {
        return dao.getBookmark(chapNo, fromVerse, toVerse)
    }

    suspend fun getBookmarks(): ArrayList<BookmarkEntity> {
        return ArrayList(dao.getBookmarks())
    }

    fun getBookmarksFlow(): Flow<List<BookmarkEntity>> {
        return dao.getBookmarksFlow()
    }

    fun getBookmarksPagingFlow(
        pageSize: Int = 20
    ): Flow<PagingData<BookmarkEntity>> {
        return Pager(
            config = PagingConfig(
                pageSize = pageSize,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { dao.getBookmarksPaginated() }
        ).flow
    }

    fun getBookmarkFlow(
        chapterNo: Int,
        fromVerse: Int,
        toVerse: Int
    ): Flow<BookmarkEntity?> {
        return dao.getBookmarkFlow(chapterNo, fromVerse, toVerse)
    }
}