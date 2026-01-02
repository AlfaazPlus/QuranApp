package com.quranapp.android.db.tafsir

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.sqlite.transaction
import com.quranapp.android.api.models.tafsir.TafsirInfoModel
import com.quranapp.android.api.models.tafsir.TafsirModel
import com.quranapp.android.db.tafsir.QuranTafsirContract.QuranTafsirEntry
import com.quranapp.android.db.tafsir.QuranTafsirInfoContract.QuranTafsirInfoEntry
import java.util.Date

private const val DB_NAME = "QuranTafsir.db"
private const val DB_VERSION = 1

class QuranTafsirDBHelper(private val context: Context) : SQLiteOpenHelper(
    context,
    DB_NAME,
    null,
    DB_VERSION
) {
    override fun onUpgrade(DB: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onCreate(DB)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }

    override fun onCreate(DB: SQLiteDatabase) {
        createInfoTable(DB)
        createMainTable(DB)
    }

    private fun createInfoTable(DB: SQLiteDatabase) {
        DB.execSQL(
            "CREATE TABLE IF NOT EXISTS ${QuranTafsirInfoEntry.TABLE_NAME} (" +
                    "${QuranTafsirInfoEntry.COL_TAFSIR_KEY} TEXT PRIMARY KEY," +
                    "${QuranTafsirInfoEntry.COL_LANG_CODE} TEXT," +
                    "${QuranTafsirInfoEntry.COL_LANG_NAME} TEXT," +
                    "${QuranTafsirInfoEntry.COL_BOOK_NAME} TEXT," +
                    "${QuranTafsirInfoEntry.COL_AUTHOR_NAME} TEXT," +
                    "${QuranTafsirInfoEntry.COL_IS_DOWNLOADED} INTEGER," +
                    "${QuranTafsirInfoEntry.COL_LAST_UPDATED} LONG)"
        )
    }


    private fun createMainTable(DB: SQLiteDatabase) {
        DB.execSQL(
            "CREATE TABLE IF NOT EXISTS ${QuranTafsirEntry.TABLE_NAME} (" +
                    "${QuranTafsirEntry._ID} TEXT PRIMARY KEY," +
                    "${QuranTafsirEntry.COL_TAFSIR_KEY} TEXT," +
                    "${QuranTafsirEntry.COL_CHAPTER_NO} TEXT," +
                    "${QuranTafsirEntry.COL_FROM_VERSE_NO} INTEGER," +
                    "${QuranTafsirEntry.COL_TO_VERSE_NO} INTEGER," +
                    "${QuranTafsirEntry.COL_TEXT} TEXT," +
                    "${QuranTafsirEntry.COL_VERSION} TEXT," +
                    "${QuranTafsirEntry.COL_LAST_UPDATED} LONG)"
        )
    }

    private fun makeVerseKey(chapterNo: Int, verseNo: Int): String {
        return "$chapterNo:$verseNo"
    }

    private fun makeTafsirVerseId(tafsirKey: String, verseKey: String): String {
        return "$tafsirKey:$verseKey"
    }


    fun storeTafsirInfo(bookInfo: TafsirInfoModel) {
        val lastUpdated = Date().time

        val values = ContentValues().apply {
            put(QuranTafsirInfoEntry.COL_TAFSIR_KEY, bookInfo.key)
            put(QuranTafsirInfoEntry.COL_LANG_CODE, bookInfo.langCode)
            put(QuranTafsirInfoEntry.COL_LANG_NAME, bookInfo.langName)
            put(QuranTafsirInfoEntry.COL_BOOK_NAME, bookInfo.name)
            put(QuranTafsirInfoEntry.COL_AUTHOR_NAME, bookInfo.author)
            put(QuranTafsirInfoEntry.COL_IS_DOWNLOADED, bookInfo.isDownloaded)
            put(QuranTafsirInfoEntry.COL_LAST_UPDATED, lastUpdated)
        }

        writableDatabase.insertWithOnConflict(
            QuranTafsirInfoEntry.TABLE_NAME,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun storeTafsirs(tafsirs: List<TafsirModel>, version: String, timestamp: Long) {
        val db = writableDatabase

        for (i in 0 until tafsirs.size) {
            val tafsir = tafsirs[i]

            val chapterNo = tafsir.verseKey.split(":").first().toIntOrNull() ?: -1
            val verses = tafsir.verses.map {
                it.split(":").last().toIntOrNull() ?: -1
            }.sorted()

            val fromVerse = verses.firstOrNull() ?: -1
            val toVerse = verses.lastOrNull() ?: -1

            val values = ContentValues().apply {
                put(QuranTafsirEntry._ID, makeTafsirVerseId(tafsir.key, tafsir.verseKey))
                put(QuranTafsirEntry.COL_TAFSIR_KEY, tafsir.key)
                put(QuranTafsirEntry.COL_CHAPTER_NO, chapterNo)
                put(QuranTafsirEntry.COL_FROM_VERSE_NO, fromVerse);
                put(QuranTafsirEntry.COL_TO_VERSE_NO, toVerse)
                put(QuranTafsirEntry.COL_TEXT, tafsir.text)
                put(QuranTafsirEntry.COL_VERSION, version)
                put(QuranTafsirEntry.COL_LAST_UPDATED, timestamp)
            }

            db.insertWithOnConflict(
                QuranTafsirEntry.TABLE_NAME,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
            )
        }
    }

    fun getTafsirByVerse(tafsirKey: String, chapterNo: Int, verseNo: Int): TafsirModel? {
        val db = readableDatabase
        val verseKey = makeVerseKey(chapterNo, verseNo)
        val tafsirVerseId = makeTafsirVerseId(tafsirKey, verseKey)

        val selection = """
            ${QuranTafsirEntry.COL_TAFSIR_KEY} = ? AND (
                ${QuranTafsirEntry._ID} = ? OR (
                    ${QuranTafsirEntry.COL_CHAPTER_NO} = ? AND 
                    ${QuranTafsirEntry.COL_FROM_VERSE_NO} <= ? AND 
                    ${QuranTafsirEntry.COL_TO_VERSE_NO} >= ?
                )
            )
        """.trimIndent()

        val cursor = db.query(
            QuranTafsirEntry.TABLE_NAME,
            null,
            selection,
            arrayOf(
                tafsirKey,
                tafsirVerseId,
                chapterNo.toString(),
                verseNo.toString(),
                verseNo.toString()
            ),
            null,
            null,
            null,
            "1"
        )

        return cursor.use {
            if (it.moveToFirst()) {
                val text = it.getString(it.getColumnIndexOrThrow(QuranTafsirEntry.COL_TEXT))
                val fromVerse =
                    it.getInt(it.getColumnIndexOrThrow(QuranTafsirEntry.COL_FROM_VERSE_NO))
                val toVerse = it.getInt(it.getColumnIndexOrThrow(QuranTafsirEntry.COL_TO_VERSE_NO))

                val verses = if (fromVerse > 0 && toVerse > 0) {
                    (fromVerse..toVerse).map { v -> "$chapterNo:$v" }
                } else {
                    listOf(verseKey)
                }

                TafsirModel(
                    key = tafsirKey,
                    verseKey = verseKey,
                    verses = verses,
                    text = text,
                )
            } else {
                null
            }
        }
    }

    fun getDownloadedTafsirKeys(): Set<String> {
        val downloadedKeys = mutableSetOf<String>()
        val db = readableDatabase

        val cursor = db.query(
            QuranTafsirInfoEntry.TABLE_NAME,
            arrayOf(QuranTafsirInfoEntry.COL_TAFSIR_KEY),
            "${QuranTafsirInfoEntry.COL_IS_DOWNLOADED} = ?",
            arrayOf("1"),
            null,
            null,
            null
        )

        cursor.use {
            while (it.moveToNext()) {
                val keyIndex = it.getColumnIndex(QuranTafsirInfoEntry.COL_TAFSIR_KEY)
                if (keyIndex >= 0) {
                    downloadedKeys.add(it.getString(keyIndex))
                }
            }
        }

        return downloadedKeys
    }

    suspend fun deleteTafsirData(tafsirKey: String) {
        val db = writableDatabase

        db.transaction {
            db.delete(
                QuranTafsirInfoEntry.TABLE_NAME,
                "${QuranTafsirInfoEntry.COL_TAFSIR_KEY}=?",
                arrayOf(tafsirKey)
            )

            db.delete(
                QuranTafsirEntry.TABLE_NAME,
                "${QuranTafsirEntry.COL_TAFSIR_KEY}=?",
                arrayOf(tafsirKey)
            )
        }
    }
}
