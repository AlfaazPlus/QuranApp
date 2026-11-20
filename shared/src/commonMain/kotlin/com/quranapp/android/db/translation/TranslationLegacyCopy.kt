package com.quranapp.android.db.translation

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.driver.bundled.SQLITE_OPEN_READONLY
import com.quranapp.android.api.safeInt
import com.quranapp.android.api.safeString
import com.quranapp.android.db.ExternalQuranDatabase
import com.quranapp.android.db.entities.translation.TranslationBookEntity
import com.quranapp.android.db.entities.translation.TranslationFootnoteEntity
import com.quranapp.android.db.entities.translation.TranslationVerseEntity
import com.quranapp.android.platform.Platform
import com.quranapp.android.utils.app.AppFiles
import com.quranapp.android.utils.quran.QuranMeta
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import okio.FileSystem

/**
 * Copies the old per-book SQLite database into ExternalQuranDatabase,
 * then deletes it.
 *
 * A failed copy leaves the old file so the next launch can try again.
 */
class TranslationLegacyCopy(
    private val database: ExternalQuranDatabase,
    private val driver: BundledSQLiteDriver = BundledSQLiteDriver(),
) {

    suspend fun copyIfPresent() {
        val file = Platform.files.databaseDirectory / OLD_DB_NAME
        if (!AppFiles.exists(file)) return

        val connection = driver.open(
            fileName = file.toString(),
            flags = SQLITE_OPEN_READONLY,
        )

        try {
            val infos = readBookInfos(connection)
            val tables = readSlugTables(connection)

            val slugs = LinkedHashSet<String>()
            slugs.addAll(infos.keys)
            slugs.addAll(tables)

            val dao = database.translationDao()

            for (slug in slugs) {
                val book = infos[slug] ?: emptyBook(slug)

                val (verses, footnotes) = if (slug in tables) {
                    readVerses(connection, slug)
                } else {
                    emptyList<TranslationVerseEntity>() to emptyList()
                }

                dao.replaceSlug(book, verses, footnotes)
            }
        } finally {
            connection.close()
        }

        deleteOldFiles()
    }

    private fun readBookInfos(
        db: SQLiteConnection,
    ): Map<String, TranslationBookEntity> {
        if (!tableExists(db, INFO_TABLE)) return emptyMap()

        val books = LinkedHashMap<String, TranslationBookEntity>()

        db.prepare(
            "SELECT slug,langCode,langName,bookName,authorName,displayName,lastUpdated FROM ${
                quote(
                    INFO_TABLE
                )
            }"
        ).use { statement ->
            val slugCol = 0
            val langCodeCol = 1
            val langNameCol = 2
            val bookNameCol = 3
            val authorCol = 4
            val displayCol = 5
            val versionCol = 6

            while (statement.step()) {
                val slug = statement.getText(slugCol) ?: continue

                books[slug] = TranslationBookEntity(
                    slug = slug,
                    langCode = statement.getText(langCodeCol).orEmpty(),
                    langName = statement.getText(langNameCol).orEmpty(),
                    bookName = statement.getText(bookNameCol).orEmpty(),
                    authorName = statement.getText(authorCol).orEmpty(),
                    displayName = statement.getText(displayCol).orEmpty(),
                    version = normalizeVersion(statement.getLong(versionCol)),
                )
            }
        }

        return books
    }

    private fun readSlugTables(
        db: SQLiteConnection,
    ): Set<String> {
        val names = LinkedHashSet<String>()

        db.prepare(
            "SELECT name FROM sqlite_master WHERE type = 'table'"
        ).use { statement ->
            while (statement.step()) {
                val name = statement.getText(0) ?: continue

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
        db: SQLiteConnection,
        slug: String,
    ): Pair<List<TranslationVerseEntity>, List<TranslationFootnoteEntity>> {
        val verses = ArrayList<TranslationVerseEntity>()
        val footnotes = ArrayList<TranslationFootnoteEntity>()

        val sql = """
            SELECT chapterNo, verseNo, text, footnotes
            FROM ${quote(slug)}
        """.trimIndent()

        db.prepare(sql).use { statement ->
            while (statement.step()) {
                val chapterNo = statement.getInt(0)
                val verseNo = statement.getInt(1)

                if (chapterNo <= 0 || verseNo <= 0) continue

                val ayahId = QuranMeta.getAyahId(chapterNo, verseNo)

                verses.add(
                    TranslationVerseEntity(
                        slug = slug,
                        ayahId = ayahId,
                        text = statement.getText(2).orEmpty(),
                    )
                )

                footnotes.addAll(
                    footnotesFromJson(
                        slug = slug,
                        ayahId = ayahId,
                        raw = statement.getText(3),
                    )
                )
            }
        }

        return verses to footnotes
    }

    private fun tableExists(
        db: SQLiteConnection,
        name: String,
    ): Boolean {
        db.prepare(
            """
            SELECT 1
            FROM sqlite_master
            WHERE type = 'table' AND name = ?
            LIMIT 1
            """.trimIndent()
        ).use { statement ->
            statement.bindText(1, name)
            return statement.step()
        }
    }

    private fun deleteOldFiles() {
        val fs = FileSystem.SYSTEM
        val directory = Platform.files.databaseDirectory
        val name = OLD_DB_NAME

        listOf(
            name,
            "$name-wal",
            "$name-shm",
            "$name-journal",
        ).forEach { filename ->
            val path = directory / filename
            if (fs.exists(path)) fs.delete(path)
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
                val array = Json.parseToJsonElement(raw).jsonArray

                array.mapNotNull { element ->
                    val obj = element.jsonObject

                    val number = obj.safeInt("index") ?: -1
                    if (number <= 0) return@mapNotNull null

                    TranslationFootnoteEntity(
                        slug = slug,
                        ayahId = ayahId,
                        number = number,
                        text = obj.safeString("text").orEmpty(),
                    )
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}
