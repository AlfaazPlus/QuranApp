/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 6/6/2022.
 * All rights reserved.
 */

package com.quranapp.android.db.transl

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.quranapp.android.components.quran.subcomponents.QuranTranslBookInfo
import com.quranapp.android.db.transl.QuranTranslContract.QuranTranslEntry.*
import com.quranapp.android.db.transl.QuranTranslInfoContract.QuranTranslInfoEntry
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.quran.QuranConstants
import com.quranapp.android.utils.reader.TranslUtils
import com.quranapp.android.utils.univ.FileUtils
import com.quranapp.android.utils.univ.StringUtils
import java.io.File
import org.json.JSONObject

class QuranTranslDBHelper(private val context: Context) : SQLiteOpenHelper(
    context,
    DB_NAME,
    null,
    DB_VERSION
) {
    companion object {
        private const val DB_NAME = "QuranTranslation.db"
        const val DB_VERSION = 1

        /**
         * Escapes tables name as it may contain special characters.
         * For example, as in "en_sahih-international"
         */
        @JvmStatic
        fun escapeTableName(tableName: String): String {
            return "`$tableName`"
        }

        @JvmStatic
        fun translationsOrderBy(): String {
            return "$COL_CHAPTER_NO ASC, $COL_VERSE_NO ASC"
        }
    }

    override fun onUpgrade(DB: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        DB.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(DB)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }

    override fun onCreate(DB: SQLiteDatabase) {
        createTranslInfoTable(DB)
        DB.beginTransaction()
        try {
            for (bookInfo in TranslUtils.preBuiltTranslBooksInfo()) {
                val prebuiltTranslPath = TranslUtils.getPrebuiltTranslPath(bookInfo.slug)
                val translStrData = StringUtils.readInputStream(
                    context.assets.open(prebuiltTranslPath)
                )
                storeTranslation(bookInfo, translStrData, DB)
            }

            migrateFileBasedTranslsToDatabase(context, DB)
            DB.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            DB.endTransaction()
        }
    }

    /**
     * The DB instance is already running in a transaction in QuranTranslDBHelper for storing built-in translations,
     * so do not close or do anything else except storing translation data.
     */
    private fun migrateFileBasedTranslsToDatabase(context: Context, DB: SQLiteDatabase) {
        val fileUtils = FileUtils.newInstance(context)
        val translDir = File(fileUtils.appFilesDirectory, TranslUtils.DIR_NAME)
        if (!translDir.exists()) return

        try {
            TranslUtils.getTranslInfosAndFilesForMigration(fileUtils, translDir)?.let {
                for (pair in it) {
                    storeTranslation(pair.first, pair.second.readText(), DB)
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        } finally {
            translDir.deleteRecursively()
            Log.d(
                "Migration finished anyhow, deleting root translation directory: " + translDir.name
            )
        }
    }

    private fun createTranslInfoTable(DB: SQLiteDatabase) {
        DB.execSQL(
            "CREATE TABLE ${QuranTranslInfoEntry.TABLE_NAME} (" +
                "${QuranTranslInfoEntry.COL_SLUG} TEXT PRIMARY KEY," +
                "${QuranTranslInfoEntry.COL_LANG_CODE} TEXT," +
                "${QuranTranslInfoEntry.COL_LANG_NAME} TEXT," +
                "${QuranTranslInfoEntry.COL_BOOK_NAME} TEXT," +
                "${QuranTranslInfoEntry.COL_AUTHOR_NAME} TEXT," +
                "${QuranTranslInfoEntry.COL_DISPLAY_NAME} TEXT," +
                "${QuranTranslInfoEntry.COL_LAST_UPDATED} LONG," +
                "${QuranTranslInfoEntry.COL_DOWNLOAD_PATH} TEXT," +
                "${QuranTranslInfoEntry.COL_IS_PREMIUM} BOOLEAN)"
        )
    }

    private fun makeVerseKey(chapterNo: Int, verseNo: Int): String {
        return "$chapterNo:$verseNo"
    }

    private fun createTranslTable(DB: SQLiteDatabase, bookInfo: QuranTranslBookInfo) {
        DB.execSQL(
            "CREATE TABLE IF NOT EXISTS ${escapeTableName(bookInfo.slug)} (" +
                "$_ID TEXT PRIMARY KEY," +
                "$COL_CHAPTER_NO INTEGER," +
                "$COL_VERSE_NO INTEGER," +
                "$COL_TEXT TEXT," +
                "$COL_FOOTNOTES TEXT)"
        )
    }

    private fun readAndInsertChapters(
        DB: SQLiteDatabase,
        bookInfo: QuranTranslBookInfo,
        root: JSONObject
    ) {
        val chapters = root.optJSONArray(QuranConstants.KEY_CHAPTER_LIST) ?: return
        for (i in 0 until chapters.length()) {
            val chapterObj = chapters.optJSONObject(i) ?: continue
            readAndInsertSingleChapter(DB, bookInfo, chapterObj)
        }
    }

    private fun readAndInsertSingleChapter(
        DB: SQLiteDatabase,
        bookInfo: QuranTranslBookInfo,
        chapterObj: JSONObject
    ) {
        val chapterNo = chapterObj.optInt(QuranConstants.KEY_NUMBER, -1)
        val verses = chapterObj.optJSONArray(QuranConstants.KEY_VERSE_LIST) ?: return
        for (i in 0 until verses.length()) {
            val verseObj = verses.optJSONObject(i) ?: continue
            val footnotes = verseObj.optJSONArray(QuranConstants.KEY_FOOTNOTE_LIST)?.toString() ?: "[]"
            insertTranslationQuery(
                DB,
                bookInfo.slug,
                chapterNo,
                verseObj.optInt(QuranConstants.KEY_NUMBER, -1),
                verseObj.optString(QuranConstants.KEY_TRANSLATION_TEXT, ""),
                footnotes
            )
        }
    }

    private fun insertTranslationQuery(
        DB: SQLiteDatabase,
        tableName: String,
        chapterNo: Int,
        verseNo: Int,
        text: String,
        footnotes: String?
    ) {
        val values = ContentValues().apply {
            put(_ID, makeVerseKey(chapterNo, verseNo))
            put(COL_CHAPTER_NO, chapterNo)
            put(COL_VERSE_NO, verseNo)
            put(COL_TEXT, text)
            put(COL_FOOTNOTES, footnotes)
        }
        DB.insert(escapeTableName(tableName), null, values)
    }

    private fun storeTranslationInfo(bookInfo: QuranTranslBookInfo, DB: SQLiteDatabase) {
        val values = ContentValues().apply {
            put(QuranTranslInfoEntry.COL_SLUG, bookInfo.slug)
            put(QuranTranslInfoEntry.COL_LANG_CODE, bookInfo.langCode)
            put(QuranTranslInfoEntry.COL_LANG_NAME, bookInfo.langName)
            put(QuranTranslInfoEntry.COL_BOOK_NAME, bookInfo.bookName)
            put(QuranTranslInfoEntry.COL_AUTHOR_NAME, bookInfo.authorName)
            put(QuranTranslInfoEntry.COL_DISPLAY_NAME, bookInfo.displayName)
            put(QuranTranslInfoEntry.COL_LAST_UPDATED, bookInfo.lastUpdated)
            put(QuranTranslInfoEntry.COL_DOWNLOAD_PATH, bookInfo.downloadPath)
        }

        DB.insert(QuranTranslInfoEntry.TABLE_NAME, null, values)
    }

    fun storeTranslation(bookInfo: QuranTranslBookInfo, translData: String, DB: SQLiteDatabase?) {
        (DB ?: writableDatabase).let {
            storeTranslationInfo(bookInfo, it)
            createTranslTable(it, bookInfo)
            try {
                val root = JSONObject(translData)
                readAndInsertChapters(it, bookInfo, root)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun storeTranslation(bookInfo: QuranTranslBookInfo, translData: String) {
        storeTranslation(bookInfo, translData, writableDatabase)
    }
}
