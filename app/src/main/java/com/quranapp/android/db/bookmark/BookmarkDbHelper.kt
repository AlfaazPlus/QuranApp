package com.quranapp.android.db.bookmark

import android.content.ContentValues
import android.content.Context
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.widget.Toast
import androidx.core.database.sqlite.transaction
import com.quranapp.android.R
import com.quranapp.android.components.bookmark.BookmarkModel
import com.quranapp.android.db.bookmark.BookmarkContract.BookmarkEntry.COL_CHAPTER_NO
import com.quranapp.android.db.bookmark.BookmarkContract.BookmarkEntry.COL_DATETIME
import com.quranapp.android.db.bookmark.BookmarkContract.BookmarkEntry.COL_FROM_VERSE_NO
import com.quranapp.android.db.bookmark.BookmarkContract.BookmarkEntry.COL_NOTE
import com.quranapp.android.db.bookmark.BookmarkContract.BookmarkEntry.COL_TO_VERSE_NO
import com.quranapp.android.db.bookmark.BookmarkContract.BookmarkEntry.TABLE_NAME
import com.quranapp.android.db.bookmark.BookmarkContract.BookmarkEntry._ID
import com.quranapp.android.utils.univ.DBUtils
import com.quranapp.android.utils.univ.DateUtils

class BookmarkDbHelper(
    private val context: Context
) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        const val DB_NAME = "Bookmark.db"
        const val DB_VERSION = 2
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_NAME (
                $_ID INTEGER PRIMARY KEY,
                $COL_CHAPTER_NO INTEGER,
                $COL_FROM_VERSE_NO INTEGER,
                $COL_TO_VERSE_NO INTEGER,
                $COL_DATETIME TEXT,
                $COL_NOTE TEXT
            )
        """.trimIndent()

        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        val deleteTable = "DROP TABLE IF EXISTS $TABLE_NAME"
        db.execSQL(deleteTable)
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }

    fun addMultipleBookmarks(bookmarks: List<BookmarkModel>) {
        writableDatabase.transaction {
            bookmarks.forEach { bookmark ->
                val values = ContentValues().apply {
                    put(COL_CHAPTER_NO, bookmark.chapterNo)
                    put(COL_FROM_VERSE_NO, bookmark.fromVerseNo)
                    put(COL_TO_VERSE_NO, bookmark.toVerseNo)
                    put(COL_DATETIME, bookmark.date)
                    put(COL_NOTE, bookmark.note)
                }
                insert(TABLE_NAME, null, values)
            }
        }
    }

    fun addToBookmark(
        chapterNo: Int,
        fromVerse: Int,
        toVerse: Int,
        note: String?,
        callback: ((BookmarkModel) -> Unit)? = null
    ) {
        if (isBookmarked(chapterNo, fromVerse, toVerse)) {
            Toast.makeText(context, R.string.strMsgBookmarkAddedAlready, Toast.LENGTH_SHORT).show()
            return
        }

        val db = writableDatabase
        val dateTime = DateUtils.dateTimeNow

        val values = ContentValues().apply {
            put(COL_CHAPTER_NO, chapterNo)
            put(COL_FROM_VERSE_NO, fromVerse)
            put(COL_TO_VERSE_NO, toVerse)
            put(COL_DATETIME, dateTime)
            put(COL_NOTE, note)
        }

        val rowId = db.insert(TABLE_NAME, null, values)
        val inserted = rowId != -1L

        val msg = if (inserted) {
            callback?.invoke(
                BookmarkModel(
                    id = rowId,
                    chapterNo = chapterNo,
                    fromVerseNo = fromVerse,
                    toVerseNo = toVerse,
                    date = dateTime,
                    note = note
                )
            )
            R.string.strMsgBookmarkAdded
        } else {
            R.string.strMsgBookmarkAddFailed
        }

        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    fun updateBookmark(
        chapterNo: Int,
        fromVerse: Int,
        toVerse: Int,
        note: String?,
        callback: ((BookmarkModel) -> Unit)? = null
    ) {
        val db = writableDatabase

        val whereClause = "$COL_CHAPTER_NO=? AND $COL_FROM_VERSE_NO=? AND $COL_TO_VERSE_NO=?"
        val whereArgs = arrayOf(
            chapterNo.toString(),
            fromVerse.toString(),
            toVerse.toString()
        )

        val dateTime = DateUtils.dateTimeNow

        val values = ContentValues().apply {
            put(COL_CHAPTER_NO, chapterNo)
            put(COL_FROM_VERSE_NO, fromVerse)
            put(COL_TO_VERSE_NO, toVerse)
            put(COL_DATETIME, dateTime)
            put(COL_NOTE, note)
        }

        val rowsAffected = db.update(TABLE_NAME, values, whereClause, whereArgs)
        val updated = rowsAffected > 0

        if (updated) {
            callback?.invoke(
                BookmarkModel(
                    id = 0L, // same limitation as your original code
                    chapterNo = chapterNo,
                    fromVerseNo = fromVerse,
                    toVerseNo = toVerse,
                    date = dateTime,
                    note = note
                )
            )
        }
    }

    fun removeFromBookmark(
        chapterNo: Int,
        fromVerse: Int,
        toVerse: Int,
        runOnSucceed: (() -> Unit)? = null
    ) {
        val db = writableDatabase

        val whereClause = DBUtils.createDBSelection(
            COL_CHAPTER_NO,
            COL_FROM_VERSE_NO,
            COL_TO_VERSE_NO
        )
        val whereArgs = arrayOf(
            chapterNo.toString(),
            fromVerse.toString(),
            toVerse.toString()
        )

        val rowsAffected = db.delete(TABLE_NAME, whereClause, whereArgs)
        val deleted = rowsAffected >= 1

        val msg = if (deleted) {
            runOnSucceed?.invoke()
            R.string.strMsgBookmarkRemoved
        } else {
            R.string.strMsgBookmarkRemoveFailed
        }

        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    fun removeBookmarksBulk(ids: LongArray, runOnSucceed: (() -> Unit)? = null) {
        val db = writableDatabase

        var rowsAffected = 0
        ids.forEach { id ->
            rowsAffected += db.delete(TABLE_NAME, "$_ID=?", arrayOf(id.toString()))
        }

        val deleted = rowsAffected >= 1

        val msg = if (deleted) {
            runOnSucceed?.invoke()
            R.string.strMsgBookmarkRemoved
        } else {
            R.string.strMsgBookmarkRemoveFailed
        }

        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    fun isBookmarked(chapterNo: Int, fromVerse: Int, toVerse: Int): Boolean {
        val db = readableDatabase

        val selection = DBUtils.createDBSelection(
            COL_CHAPTER_NO,
            COL_FROM_VERSE_NO,
            COL_TO_VERSE_NO
        )
        val selectionArgs = arrayOf(
            chapterNo.toString(),
            fromVerse.toString(),
            toVerse.toString()
        )

        val count = DatabaseUtils.queryNumEntries(db, TABLE_NAME, selection, selectionArgs)
        return count > 0
    }

    fun removeAllBookmarks() {
        writableDatabase.delete(TABLE_NAME, null, null)
    }

    fun getBookmark(chapNo: Int, fromVerse: Int, toVerse: Int): BookmarkModel? {
        val db = readableDatabase

        val selection = DBUtils.createDBSelection(
            COL_CHAPTER_NO,
            COL_FROM_VERSE_NO,
            COL_TO_VERSE_NO
        )
        val selectionArgs = arrayOf(
            chapNo.toString(),
            fromVerse.toString(),
            toVerse.toString()
        )
        val sortOrder = "$_ID DESC"

        val cursor = db.query(
            TABLE_NAME,
            null,
            selection,
            selectionArgs,
            null,
            null,
            sortOrder
        )

        cursor.use {
            var model: BookmarkModel? = null

            while (it.moveToNext()) {
                model = BookmarkModel(
                    id = it.getLong(it.getColumnIndexOrThrow(_ID)),
                    chapterNo = it.getInt(it.getColumnIndexOrThrow(COL_CHAPTER_NO)),
                    fromVerseNo = it.getInt(it.getColumnIndexOrThrow(COL_FROM_VERSE_NO)),
                    toVerseNo = it.getInt(it.getColumnIndexOrThrow(COL_TO_VERSE_NO)),
                    date = it.getString(it.getColumnIndexOrThrow(COL_DATETIME)),
                    note = it.getString(it.getColumnIndexOrThrow(COL_NOTE))
                )
            }

            return model
        }
    }

    fun getBookmarks(): ArrayList<BookmarkModel> {
        val db = readableDatabase
        val sortOrder = "$_ID DESC"
        val verses = arrayListOf<BookmarkModel>()

        val cursor = db.query(
            TABLE_NAME,
            null,
            null,
            null,
            null,
            null,
            sortOrder
        )

        cursor.use {
            while (it.moveToNext()) {
                verses.add(
                    BookmarkModel(
                        id = it.getLong(it.getColumnIndexOrThrow(_ID)),
                        chapterNo = it.getInt(it.getColumnIndexOrThrow(COL_CHAPTER_NO)),
                        fromVerseNo = it.getInt(it.getColumnIndexOrThrow(COL_FROM_VERSE_NO)),
                        toVerseNo = it.getInt(it.getColumnIndexOrThrow(COL_TO_VERSE_NO)),
                        date = it.getString(it.getColumnIndexOrThrow(COL_DATETIME)),
                        note = it.getString(it.getColumnIndexOrThrow(COL_NOTE))
                    )
                )
            }
        }

        return verses
    }
}