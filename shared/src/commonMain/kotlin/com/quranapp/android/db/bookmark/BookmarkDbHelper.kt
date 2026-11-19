package com.quranapp.android.db.bookmark

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.quranapp.android.db.openBundled
import com.quranapp.android.db.textOrNull
import com.quranapp.android.models.BookmarkModel
import com.quranapp.android.platform.PlatformServices
import com.quranapp.android.utils.app.AppFiles

/**
 * Reads the pre-Room bookmark file during one-time migration.
 * New bookmarks live in [com.quranapp.android.db.UserDatabase].
 */
class BookmarkDbHelper() {
    fun getBookmarks(): ArrayList<BookmarkModel> {
        val file = PlatformServices.files.databaseDirectory / DB_NAME
        if (!AppFiles.exists(file)) return arrayListOf()

        val connection = openBundled(file, DB_VERSION) { db ->
            db.execSQL(CREATE_TABLE)
        }
        return connection.use { readBookmarks(it) }
    }

    private fun readBookmarks(connection: SQLiteConnection): ArrayList<BookmarkModel> {
        val bookmarks = arrayListOf<BookmarkModel>()

        connection.prepare(SELECT_BOOKMARKS).use { statement ->
            while (statement.step()) {
                bookmarks.add(
                    BookmarkModel(
                        id = statement.getLong(0),
                        chapterNo = statement.getLong(1).toInt(),
                        fromVerseNo = statement.getLong(2).toInt(),
                        toVerseNo = statement.getLong(3).toInt(),
                        date = statement.textOrNull(4),
                        note = statement.textOrNull(5),
                    )
                )
            }
        }

        return bookmarks
    }

    private companion object {
        const val DB_NAME = "Bookmark.db"
        const val DB_VERSION = 2
        const val CREATE_TABLE = """
            CREATE TABLE IF NOT EXISTS QuranBookmark (
                _id INTEGER PRIMARY KEY,
                ChapterNumber INTEGER,
                FromVerseNumber INTEGER,
                ToVerseNumber INTEGER,
                Date TEXT,
                Note TEXT
            )
        """
        const val SELECT_BOOKMARKS = """
            SELECT _id, ChapterNumber, FromVerseNumber, ToVerseNumber, Date, Note
            FROM QuranBookmark
            ORDER BY _id DESC
        """
    }
}
