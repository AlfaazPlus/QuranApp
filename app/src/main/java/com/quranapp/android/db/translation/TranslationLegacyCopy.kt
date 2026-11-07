package com.quranapp.android.db.translation

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.quranapp.android.db.ExternalQuranDatabase
import com.quranapp.android.db.entities.translation.TranslationBookEntity
import com.quranapp.android.db.entities.translation.TranslationFootnoteEntity
import com.quranapp.android.db.entities.translation.TranslationVerseEntity
import com.quranapp.android.utils.quran.QuranMeta
import org.json.JSONArray
import java.io.File

/**
 * Copies the old per-book SQLiteOpenHelper file into ExternalQuranDatabase, then deletes it.
 * A failed copy leaves the old file so the next launch can try again.
 */
class TranslationLegacyCopy(
    private val database: ExternalQuranDatabase,
) {
    suspend fun copyIfPresent(context: Context) {
        val file = context.getDatabasePath(OLD_DB_NAME)
        if (!file.exists()) return

        SQLiteDatabase.openDatabase(
            file.path,
            null,
            SQLiteDatabase.OPEN_READONLY,
        ).use { old ->
            val infos = readBookInfos(old)
            val tables = readSlugTables(old)

            val slugs = LinkedHashSet<String>()
            slugs.addAll(infos.keys)
            slugs.addAll(tables)

            val dao = database.translationDao()

            for (slug in slugs) {
                val book = infos[slug] ?: emptyBook(slug)
                
                val (verses, footnotes) = if (slug in tables) {
                    readVerses(old, slug)
                } else {
                    emptyList<TranslationVerseEntity>() to emptyList()
                }

                dao.replaceSlug(book, verses, footnotes)
            }
        }

        deleteOldFiles(context)
    }

    private fun readBookInfos(db: SQLiteDatabase): Map<String, TranslationBookEntity> {
        if (!tableExists(db, INFO_TABLE)) return emptyMap()

        val books = LinkedHashMap<String, TranslationBookEntity>()
        db.query(INFO_TABLE, null, null, null, null, null, null).use { cursor ->
            val slugCol = cursor.getColumnIndexOrThrow("slug")
            val langCodeCol = cursor.getColumnIndexOrThrow("langCode")
            val langNameCol = cursor.getColumnIndexOrThrow("langName")
            val bookNameCol = cursor.getColumnIndexOrThrow("bookName")
            val authorCol = cursor.getColumnIndexOrThrow("authorName")
            val displayCol = cursor.getColumnIndexOrThrow("displayName")
            val versionCol = cursor.getColumnIndexOrThrow("lastUpdated")

            while (cursor.moveToNext()) {
                val slug = cursor.getString(slugCol) ?: continue
                books[slug] = TranslationBookEntity(
                    slug = slug,
                    langCode = cursor.getString(langCodeCol).orEmpty(),
                    langName = cursor.getString(langNameCol).orEmpty(),
                    bookName = cursor.getString(bookNameCol).orEmpty(),
                    authorName = cursor.getString(authorCol).orEmpty(),
                    displayName = cursor.getString(displayCol).orEmpty(),
                    version = normalizeVersion(cursor.getLong(versionCol)),
                )
            }
        }
        return books
    }

    private fun readSlugTables(db: SQLiteDatabase): Set<String> {
        val names = LinkedHashSet<String>()
        db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type = 'table'",
            null,
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(0) ?: continue
                
                if (name == INFO_TABLE) continue
                if (name == "android_metadata") continue
                if (name.startsWith("sqlite_")) continue
                if (name.startsWith("room_")) continue

                names.add(name)
            }
        }
        return names
    }

    private fun readVerses(
        db: SQLiteDatabase,
        slug: String,
    ): Pair<List<TranslationVerseEntity>, List<TranslationFootnoteEntity>> {
        val verses = ArrayList<TranslationVerseEntity>()
        val footnotes = ArrayList<TranslationFootnoteEntity>()
        val sql = "SELECT chapterNo, verseNo, text, footnotes FROM ${quote(slug)}"

        db.rawQuery(sql, null).use { cursor ->
            while (cursor.moveToNext()) {
                val chapterNo = cursor.getInt(0)
                val verseNo = cursor.getInt(1)
                if (chapterNo <= 0 || verseNo <= 0) continue

                val ayahId = QuranMeta.getAyahId(chapterNo, verseNo)

                verses.add(
                    TranslationVerseEntity(
                        slug = slug,
                        ayahId = ayahId,
                        text = cursor.getString(2).orEmpty(),
                    )
                )

                footnotes.addAll(
                    footnotesFromJson(slug, ayahId, cursor.getString(3))
                )
            }
        }
        return verses to footnotes
    }

    private fun tableExists(db: SQLiteDatabase, name: String): Boolean {
        db.rawQuery(
            "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ? LIMIT 1",
            arrayOf(name),
        ).use { cursor ->
            return cursor.moveToFirst()
        }
    }

    private fun deleteOldFiles(context: Context) {
        context.deleteDatabase(OLD_DB_NAME)
        val dbFile = context.getDatabasePath(OLD_DB_NAME)
        listOf("", "-wal", "-shm", "-journal").forEach { suffix ->
            File(dbFile.path + suffix).delete()
        }
    }

    private fun emptyBook(slug: String): TranslationBookEntity {
        return TranslationBookEntity(
            slug = slug,
            langCode = "",
            langName = "",
            bookName = "",
            authorName = "",
            displayName = "",
            version = 1L,
        )
    }

    companion object {
        const val OLD_DB_NAME = "QuranTranslation.db"
        private const val INFO_TABLE = "QuranTranslationBookInfo"
        private const val LEGACY_VERSION_THRESHOLD = 1_000_000L

        fun normalizeVersion(raw: Long): Long {
            if (raw <= 0L) return 1L
            if (raw > LEGACY_VERSION_THRESHOLD) return 1L
            return raw
        }

        fun quote(name: String): String {
            return "`${name.replace("`", "``")}`"
        }

        fun footnotesFromJson(
            slug: String,
            ayahId: Int,
            raw: String?,
        ): List<TranslationFootnoteEntity> {
            if (raw.isNullOrBlank()) return emptyList()

            return try {
                val array = JSONArray(raw)
                val footnotes = ArrayList<TranslationFootnoteEntity>(array.length())
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val number = obj.optInt("index", -1)
                    if (number <= 0) continue
                    footnotes.add(
                        TranslationFootnoteEntity(
                            slug = slug,
                            ayahId = ayahId,
                            number = number,
                            text = obj.optString("text", ""),
                        )
                    )
                }
                footnotes
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}
