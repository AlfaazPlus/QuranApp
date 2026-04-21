/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 6/6/2022.
 * All rights reserved.
 */

package com.quranapp.android.utils.reader.factory

import android.content.Context
import android.database.Cursor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.core.database.sqlite.transaction
import com.quranapp.android.api.models.translation.TranslationBookInfoModel
import com.quranapp.android.components.quran.subcomponents.Footnote
import com.quranapp.android.components.quran.subcomponents.Translation
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.db.translation.QuranTranslContract.QuranTranslEntry.COL_CHAPTER_NO
import com.quranapp.android.db.translation.QuranTranslContract.QuranTranslEntry.COL_FOOTNOTES
import com.quranapp.android.db.translation.QuranTranslContract.QuranTranslEntry.COL_TEXT
import com.quranapp.android.db.translation.QuranTranslContract.QuranTranslEntry.COL_VERSE_NO
import com.quranapp.android.db.translation.QuranTranslDBHelper
import com.quranapp.android.db.translation.QuranTranslInfoContract.QuranTranslInfoEntry
import com.quranapp.android.utils.quran.QuranConstants
import com.quranapp.android.utils.reader.TranslUtils
import org.json.JSONArray
import java.io.Closeable
import java.util.Collections

/**
 * This factory prepares contents of translations for the requesters.
 * The content may be [TranslationBookInfoModel] or the actual translation contents.
 * */
class QuranTranslationFactory(private val context: Context) : Closeable {
    companion object {
        @Composable
        fun remember(context: Context): QuranTranslationFactory {
            val factory = remember(context) {
                QuranTranslationFactory(context)
            }

            DisposableEffect(Unit) {
                onDispose {
                    factory.close()
                }
            }

            return factory
        }
    }

    val dbHelper = QuranTranslDBHelper(context)

    override fun close() {
        dbHelper.close()
    }

    fun deleteTranslation(translSlug: String) {
        val db = dbHelper.writableDatabase
        db.transaction {
            try {
                execSQL("DROP TABLE IF EXISTS '$translSlug'")
                delete(
                    QuranTranslInfoEntry.TABLE_NAME,
                    "${QuranTranslInfoEntry.COL_SLUG}=?",
                    arrayOf(translSlug)
                )
            } finally {
            }
        }
    }

    /**
     * Check if translation table for the given slug exists.
     * */
    fun isTranslationDownloaded(slug: String): Boolean {
        val query = "SELECT DISTINCT tbl_name FROM sqlite_master WHERE tbl_name = '$slug'"
        dbHelper.readableDatabase.rawQuery(query, null).use { cursor ->
            if (cursor.count > 0) {
                return true
            }
            return false
        }
    }

    /**
     * Gets and prepare an instance of [TranslationBookInfoModel] from the database.
     * If no book is found in the database, an empty instance is returned.
     * @param slug The slug of the book.
     * */
    fun getTranslationBookInfo(slug: String): TranslationBookInfoModel {
        return getTranslationBooksInfo(Collections.singleton(slug))[slug]
            ?: TranslationBookInfoModel("")
    }

    /**
     * Gets a map of [TranslationBookInfoModel] in [getAvailableTranslationBooksInfo] excluding the built-in translations.
     * Only info for translations which are downloaded by the user are returned.
     */
    fun getDownloadedTranslationBooksInfo(): Map<String, TranslationBookInfoModel> {
        return getTranslationBooksInfoValidated().filterKeys { !TranslUtils.isPrebuilt(it) }
    }

    /**
     * Gets and prepare instances of [TranslationBookInfoModel] from the database for all `available` slugs.
     * Here the meaning of `available` is - all books stored in the database.
     * When a book is downloaded from the server, then its information along with its content is stored in the database.
     * Then the information is included in `available` slugs.
     * @return The returned value is a [Map] where the key is the corresponding slug.
     * */
    fun getAvailableTranslationBooksInfo(): Map<String, TranslationBookInfoModel> {
        return getTranslationBooksInfoValidated()
    }

    /**
     * Gets and prepare an instances of [TranslationBookInfoModel] from the database for the given slugs and also validating the premiership.
     * @param slugs If it is empty then empty map is returned. If null is passed as the slugs, all valid books are returned.
     * @return The returned value is a [Map] where the key is the corresponding slug.
     * */
    fun getTranslationBooksInfoValidated(slugs: Set<String>? = null): Map<String, TranslationBookInfoModel> {
        if (slugs?.isEmpty() == true) return HashMap()

        return getTranslationBooksInfo(slugs)
    }

    /**
     * Gets and prepare instances of [TranslationBookInfoModel] from the database for the given slugs.
     * @return The returned value is a [Map] where the key is the corresponding slug.
     * */

    private fun getTranslationBooksInfo(slugs: Set<String>? = null): Map<String, TranslationBookInfoModel> {
        val selection = if (slugs != null) {
            List(slugs.size) { "${QuranTranslInfoEntry.COL_SLUG}=?" }.joinToString(" OR ")
        } else {
            null
        }
        val selectionArgs = slugs?.toTypedArray()

        val cursor = dbHelper.readableDatabase.query(
            true,
            QuranTranslInfoEntry.TABLE_NAME,
            null, selection, selectionArgs,
            null, null, "${QuranTranslInfoEntry.COL_SLUG} ASC", null
        )

        try {
            return getTranslationBookInfoFromCursor(cursor)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor.close()
        }

        return HashMap()
    }

    /**
     * Prepare instances of [TranslationBookInfoModel] from the db cursor.
     * Each iteration has information for a single book.
     * @return The returned value is a [Map] where the key is the corresponding slug.
     * */
    @Throws(java.lang.Exception::class)
    private fun getTranslationBookInfoFromCursor(cursor: Cursor): HashMap<String, TranslationBookInfoModel> {
        val bookInfos = HashMap<String, TranslationBookInfoModel>()
        while (cursor.moveToNext()) {
            val bookInfo = TranslationBookInfoModel(
                cursor.getString(cursor.getColumnIndexOrThrow(QuranTranslInfoEntry.COL_SLUG))
            ).apply {
                bookName = cursor.getString(
                    cursor.getColumnIndexOrThrow(QuranTranslInfoEntry.COL_BOOK_NAME)
                )
                authorName = cursor.getString(
                    cursor.getColumnIndexOrThrow(QuranTranslInfoEntry.COL_AUTHOR_NAME)
                )
                displayName = cursor.getString(
                    cursor.getColumnIndexOrThrow(QuranTranslInfoEntry.COL_DISPLAY_NAME)
                )
                langName = cursor.getString(
                    cursor.getColumnIndexOrThrow(QuranTranslInfoEntry.COL_LANG_NAME)
                )
                langCode = cursor.getString(
                    cursor.getColumnIndexOrThrow(QuranTranslInfoEntry.COL_LANG_CODE)
                )
                lastUpdated = cursor.getLong(
                    cursor.getColumnIndexOrThrow(QuranTranslInfoEntry.COL_LAST_UPDATED)
                )
                downloadPath = cursor.getString(
                    cursor.getColumnIndexOrThrow(QuranTranslInfoEntry.COL_DOWNLOAD_PATH)
                )
            }
            bookInfos[bookInfo.slug] = bookInfo
        }
        return bookInfos
    }

    fun getTranslationsSingleVerse(chapNo: Int, verseNo: Int): List<Translation> {
        return getTranslationsSingleVerse(ReaderPreferences.getTranslations(), chapNo, verseNo)
    }

    fun getTranslationsSingleSlugVerse(slug: String, chapNo: Int, verseNo: Int): Translation {
        return getTranslationsSingleVerse(Collections.singleton(slug), chapNo, verseNo)[0]
    }

    /**
     *      example:
     *      [<Transl-of-Slug1>, <Transl-of-Slug2>, <Transl-of-Slug3>] -> verse 1:1
     * */
    fun getTranslationsSingleVerse(
        slugs: Set<String>,
        chapNo: Int,
        verseNo: Int
    ): List<Translation> {
        val nSlugs = sortTranslationSlugs(validatePremierShip(slugs))

        val transls = ArrayList<Translation>()

        val selection = "$COL_CHAPTER_NO=? AND $COL_VERSE_NO=?"
        val selectionArgs = arrayOf(chapNo.toString(), verseNo.toString())

        for ((slugIndex, slug) in nSlugs.withIndex()) {
            val translations = getTranslationsFromQuery(slug, selection, selectionArgs)
            if (!translations.isNullOrEmpty()) {
                transls.add(slugIndex, translations[0])
            }
        }

        return transls
    }

    fun getTranslationsVerseRange(
        chapNo: Int,
        fromVerse: Int,
        toVerse: Int
    ): List<List<Translation>> {
        return getTranslationsVerseRange(
            ReaderPreferences.getTranslations(),
            chapNo,
            fromVerse,
            toVerse
        )
    }

    /**
     *       example:
     *       [
     *            [<Transl-of-Slug1>, <Transl-of-Slug2>, <Transl-of-Slug3>] -> verse 1:1
     *            [<Transl-of-Slug1>, <Transl-of-Slug2>, <Transl-of-Slug3>] -> verse 1:2
     *            [<Transl-of-Slug1>, <Transl-of-Slug2>, <Transl-of-Slug3>] -> verse 1:3
     *            [<Transl-of-Slug1>, <Transl-of-Slug2>, <Transl-of-Slug3>] -> verse 1:4
     *       ]
     * */
    fun getTranslationsVerseRange(
        slugs: Set<String>?,
        chapNo: Int,
        fromVerse: Int,
        toVerse: Int
    ): List<List<Translation>> {
        val transls = List(toVerse - fromVerse + 1) { ArrayList<Translation>() }.toMutableList()

        if (slugs.isNullOrEmpty()) {
            return transls
        }

        val nSlugs = sortTranslationSlugs(validatePremierShip(slugs))

        val selection = "$COL_CHAPTER_NO=? AND $COL_VERSE_NO>=? AND $COL_VERSE_NO<=?"
        val selectionArgs = arrayOf(chapNo.toString(), fromVerse.toString(), toVerse.toString())

        // This loop creates list of translations packed with list of slugs as shown in the example.
        for ((slugIndex, slug) in nSlugs.withIndex()) {
            val translations = getTranslationsFromQuery(slug, selection, selectionArgs)
            if (!translations.isNullOrEmpty()) {
                for ((translIndex, transl) in translations.withIndex()) {
                    transls[translIndex].add(slugIndex, transl)
                }
            }
        }

        return transls
    }

    /*
    * The returned verses will be sorted by verse number regardless of order of the passed verse numbers..
    * */
    fun getTranslationsDistinctVerses(chapNo: Int, vararg verses: Int): List<List<Translation>> {
        return getTranslationsDistinctVerses(
            ReaderPreferences.getTranslations(),
            chapNo,
            *verses
        )
    }

    /*
    * The returned verses will be sorted by verse number regardless of order of the passed verse numbers..
    * */
    fun getTranslationsDistinctVerses(
        slugs: Set<String>,
        chapNo: Int,
        vararg verses: Int
    ): List<List<Translation>> {
        val translationGroups = List(verses.size) {
            ArrayList<Translation>()
        }.toMutableList()

        if (slugs.isEmpty()) {
            return translationGroups
        }

        val nSlugs = sortTranslationSlugs(validatePremierShip(slugs))

        val versesSize = verses.size

        val selection = StringBuilder("$COL_CHAPTER_NO=? AND (")
        for (i in 0 until versesSize) {
            selection.append("$COL_VERSE_NO=?")
            if (i < versesSize - 1) {
                selection.append(" OR ")
            }
        }
        selection.append(")")

        val selectionArgs = Array(versesSize + 1) { "" }
        selectionArgs[0] = chapNo.toString()
        for (i in verses.indices) {
            selectionArgs[i + 1] = verses[i].toString()
        }

        for ((slugIndex, slug) in nSlugs.withIndex()) {
            val translations = getTranslationsFromQuery(slug, selection.toString(), selectionArgs)
            if (!translations.isNullOrEmpty()) {
                for ((translIndex, transl) in translations.withIndex()) {
                    translationGroups[translIndex].add(slugIndex, transl)
                }
            }
        }

        return translationGroups
    }

    private fun sortTranslationSlugs(slugs: Set<String>): Set<String> {
        val transliterations = ArrayList<String>()
        val nonTransliterations = ArrayList<String>()

        slugs.forEach { slug ->
            if (TranslUtils.isTransliteration(slug)) {
                transliterations.add(slug)
            } else {
                nonTransliterations.add(slug)
            }
        }

        return (transliterations + nonTransliterations).toSet()
    }

    private fun validatePremierShip(translSlugs: Set<String>): Set<String> {
        if (translSlugs.isEmpty()) return HashSet()
        return getTranslationBooksInfoValidated(translSlugs).keys
    }

    private fun getTranslationsFromQuery(
        translSlug: String,
        selection: String,
        selectionArgs: Array<String>
    ): List<Translation>? {
        return try {
            val cols = arrayOf(COL_CHAPTER_NO, COL_VERSE_NO, COL_TEXT, COL_FOOTNOTES)
            val cursor = dbHelper.readableDatabase.query(
                true,
                QuranTranslDBHelper.escapeTableName(translSlug),
                cols, selection, selectionArgs,
                null, null, QuranTranslDBHelper.translationsOrderBy(), null
            )
            getTranslationsFromCursor(translSlug, cursor)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getTranslationsFromCursor(translSlug: String, cursor: Cursor): List<Translation> {
        cursor.use { cursor1 ->
            val transls = ArrayList<Translation>()
            while (cursor1.moveToNext()) {
                val transl = Translation().apply {
                    chapterNo = cursor1.getInt(cursor1.getColumnIndexOrThrow(COL_CHAPTER_NO))
                    verseNo = cursor1.getInt(cursor1.getColumnIndexOrThrow(COL_VERSE_NO))
                    text = cursor1.getString(cursor1.getColumnIndexOrThrow(COL_TEXT))
                    bookSlug = translSlug
                    isUrdu = TranslUtils.isUrdu(translSlug)

                    try {
                        footnotes = readFootnotes(
                            translSlug,
                            this.chapterNo,
                            this.verseNo,
                            cursor1.getString(cursor1.getColumnIndexOrThrow(COL_FOOTNOTES))
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                transls.add(transl)
            }
            return transls
        }
    }

    /**
     * [footnoteString] is the string of JsonArray.
     */
    @Throws(Exception::class)
    private fun readFootnotes(
        translSlug: String,
        chapterNo: Int,
        verseNo: Int,
        footnoteString: String
    ): HashMap<Int, Footnote> {
        val footnotesMap = HashMap<Int, Footnote>()
        val footnotes = JSONArray(footnoteString)
        for (i in 0 until footnotes.length()) {
            val footnoteObj = footnotes.optJSONObject(i) ?: continue
            val footnote = Footnote().apply {
                this.chapterNo = chapterNo
                this.verseNo = verseNo
                number = footnoteObj.optInt(QuranConstants.KEY_NUMBER, -1)
                text = footnoteObj.optString(QuranConstants.KEY_FOOTNOTE_SINGLE_TEXT, "")
                bookSlug = translSlug
            }
            footnotesMap[footnote.number] = footnote
        }
        return footnotesMap
    }

    fun getFootnotesSingleVerse(translSlug: String, chapNo: Int, verseNo: Int): Map<Int, Footnote> {
        val cols = arrayOf(COL_FOOTNOTES)
        val selection = "$COL_CHAPTER_NO=? AND $COL_VERSE_NO=?"
        val selectionArgs = arrayOf(chapNo.toString(), verseNo.toString())

        val cursor = dbHelper.readableDatabase.query(
            true,
            QuranTranslDBHelper.escapeTableName(translSlug),
            cols, selection, selectionArgs,
            null, null, QuranTranslDBHelper.translationsOrderBy(), null
        )

        return if (cursor.moveToNext()) {
            val footnotes = readFootnotes(
                translSlug,
                chapNo,
                verseNo,
                cursor.getString(cursor.getColumnIndexOrThrow(COL_FOOTNOTES))
            )
            cursor.close()
            footnotes
        } else {
            emptyMap()
        }
    }

    fun getTranslationsBulkForSearch(
        slugs: Set<String>,
        verseKeys: List<Pair<Int, Int>>
    ): Map<String, Map<Pair<Int, Int>, Translation>> {

        if (slugs.isEmpty() || verseKeys.isEmpty()) return emptyMap()

        val ids = verseKeys.map { "${it.first}:${it.second}" }

        val sql = buildString {
            slugs.forEachIndexed { index, slug ->

                if (index > 0) append(" UNION ALL ")

                append(
                    """
                SELECT
                    '$slug' AS slug,
                    chapterNo,
                    verseNo,
                    text,
                    footnotes
                FROM `${slug}`
                WHERE _ID IN (${ids.joinToString(",") { "?" }})
                """.trimIndent()
                )
            }

            append(" ORDER BY chapterNo, verseNo")
        }

        val args = Array(ids.size * slugs.size) { i ->
            ids[i % ids.size]
        }

        val result =
            mutableMapOf<String, MutableMap<Pair<Int, Int>, Translation>>()

        dbHelper.readableDatabase.rawQuery(sql, args).use { cursor ->

            while (cursor.moveToNext()) {

                val slug = cursor.getString(0)
                val surahNo = cursor.getInt(1)
                val ayahNo = cursor.getInt(2)
                val text = cursor.getString(3)

                val map = result.getOrPut(slug) { mutableMapOf() }

                map[surahNo to ayahNo] = Translation().apply {
                    chapterNo = surahNo
                    verseNo = ayahNo
                    this.text = text
                    bookSlug = slug
                }
            }
        }

        return result
    }
}
