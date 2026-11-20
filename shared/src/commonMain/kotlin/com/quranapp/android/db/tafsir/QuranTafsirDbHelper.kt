package com.quranapp.android.db.tafsir

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.quranapp.android.api.models.tafsir.TafsirInfoModel
import com.quranapp.android.api.models.tafsir.TafsirModel
import com.quranapp.android.db.bindTextOrNull
import com.quranapp.android.db.openBundled
import com.quranapp.android.db.textOrNull
import com.quranapp.android.platform.Platform
import okio.FileSystem

private const val DB_NAME = "QuranTafsir.db"
private const val DB_VERSION = 1

object QuranTafsirEntry {
    const val TABLE_NAME: String = "Tafsir"

    /**
     * <chapter_no>:<verse_no>
     */
    const val _ID: String = "id"
    const val COL_TAFSIR_KEY: String = "tafsir_key"
    const val COL_CHAPTER_NO: String = "chapter_no"
    const val COL_FROM_VERSE_NO: String = "from_verse_no"
    const val COL_TO_VERSE_NO: String = "to_verse_no"
    const val COL_TEXT: String = "text"
    const val COL_VERSION: String = "version"
    const val COL_LAST_UPDATED: String = "last_updated"
}

object QuranTafsirInfoEntry {
    const val TABLE_NAME: String = "QuranTafsirBookInfo"
    const val COL_TAFSIR_KEY: String = "tafsir_key"
    const val COL_LANG_CODE: String = "lang_code"
    const val COL_LANG_NAME: String = "lang_name"
    const val COL_BOOK_NAME: String = "book_name"
    const val COL_AUTHOR_NAME: String = "author_name"
    const val COL_IS_DOWNLOADED: String = "is_downloaded"
    const val COL_LAST_UPDATED: String = "last_updated"
}

class QuranTafsirDBHelper() {
    fun close() = Unit

    fun storeTafsirInfo(bookInfo: TafsirInfoModel) {
        use { db ->
            db.prepare(
                """
                INSERT OR REPLACE INTO ${QuranTafsirInfoEntry.TABLE_NAME} (
                    ${QuranTafsirInfoEntry.COL_TAFSIR_KEY},
                    ${QuranTafsirInfoEntry.COL_LANG_CODE},
                    ${QuranTafsirInfoEntry.COL_LANG_NAME},
                    ${QuranTafsirInfoEntry.COL_BOOK_NAME},
                    ${QuranTafsirInfoEntry.COL_AUTHOR_NAME},
                    ${QuranTafsirInfoEntry.COL_IS_DOWNLOADED},
                    ${QuranTafsirInfoEntry.COL_LAST_UPDATED}
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            ).use { statement ->
                statement.bindText(1, bookInfo.key)
                statement.bindTextOrNull(2, bookInfo.langCode)
                statement.bindTextOrNull(3, bookInfo.langName)
                statement.bindTextOrNull(4, bookInfo.name)
                statement.bindTextOrNull(5, bookInfo.author)
                statement.bindLong(6, if (bookInfo.isDownloaded) 1 else 0)
                statement.bindLong(7, System.currentTimeMillis())
                statement.step()
            }
        }
    }

    fun storeTafsirs(tafsirs: List<TafsirModel>, version: String, timestamp: Long) {
        use { db ->
            val sql = """
                INSERT OR REPLACE INTO ${QuranTafsirEntry.TABLE_NAME} (
                    ${QuranTafsirEntry._ID},
                    ${QuranTafsirEntry.COL_TAFSIR_KEY},
                    ${QuranTafsirEntry.COL_CHAPTER_NO},
                    ${QuranTafsirEntry.COL_FROM_VERSE_NO},
                    ${QuranTafsirEntry.COL_TO_VERSE_NO},
                    ${QuranTafsirEntry.COL_TEXT},
                    ${QuranTafsirEntry.COL_VERSION},
                    ${QuranTafsirEntry.COL_LAST_UPDATED}
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            db.prepare(sql).use { statement ->
                for (tafsir in tafsirs) {
                    val chapterNo = tafsir.verseKey.split(":").first().toIntOrNull() ?: -1
                    val verses = tafsir.verses.map {
                        it.split(":").last().toIntOrNull() ?: -1
                    }.sorted()
                    val fromVerse = verses.firstOrNull() ?: -1
                    val toVerse = verses.lastOrNull() ?: -1
                    statement.bindText(1, makeTafsirVerseId(tafsir.key, tafsir.verseKey))
                    statement.bindText(2, tafsir.key)
                    statement.bindText(3, chapterNo.toString())
                    statement.bindLong(4, fromVerse.toLong())
                    statement.bindLong(5, toVerse.toLong())
                    statement.bindTextOrNull(6, tafsir.text)
                    statement.bindText(7, version)
                    statement.bindLong(8, timestamp)
                    statement.step()
                    statement.reset()
                    statement.clearBindings()
                }
            }
        }
    }

    fun getTafsirByVerse(tafsirKey: String, chapterNo: Int, verseNo: Int): TafsirModel? {
        val verseKey = makeVerseKey(chapterNo, verseNo)
        val tafsirVerseId = makeTafsirVerseId(tafsirKey, verseKey)
        return use { db ->
            db.prepare(
                """
                SELECT ${QuranTafsirEntry.COL_TEXT},
                       ${QuranTafsirEntry.COL_FROM_VERSE_NO},
                       ${QuranTafsirEntry.COL_TO_VERSE_NO}
                FROM ${QuranTafsirEntry.TABLE_NAME}
                WHERE ${QuranTafsirEntry.COL_TAFSIR_KEY} = ? AND (
                    ${QuranTafsirEntry._ID} = ? OR (
                        ${QuranTafsirEntry.COL_CHAPTER_NO} = ? AND
                        ${QuranTafsirEntry.COL_FROM_VERSE_NO} <= ? AND
                        ${QuranTafsirEntry.COL_TO_VERSE_NO} >= ?
                    )
                )
                LIMIT 1
                """.trimIndent()
            ).use { statement ->
                statement.bindText(1, tafsirKey)
                statement.bindText(2, tafsirVerseId)
                statement.bindText(3, chapterNo.toString())
                statement.bindLong(4, verseNo.toLong())
                statement.bindLong(5, verseNo.toLong())
                if (!statement.step()) return@use null
                val text = statement.textOrNull(0).orEmpty()
                val fromVerse = statement.getLong(1).toInt()
                val toVerse = statement.getLong(2).toInt()
                val verses = if (fromVerse > 0 && toVerse > 0) {
                    (fromVerse..toVerse).map { "$chapterNo:$it" }
                } else {
                    listOf(verseKey)
                }
                TafsirModel(
                    key = tafsirKey,
                    verseKey = verseKey,
                    verses = verses,
                    text = text,
                )
            }
        }
    }

    fun getDownloadedTafsirKeys(): Set<String> {
        return use { db ->
            val keys = mutableSetOf<String>()
            db.prepare(
                """
                SELECT ${QuranTafsirInfoEntry.COL_TAFSIR_KEY}
                FROM ${QuranTafsirInfoEntry.TABLE_NAME}
                WHERE ${QuranTafsirInfoEntry.COL_IS_DOWNLOADED} = 1
                """.trimIndent()
            ).use { statement ->
                while (statement.step()) {
                    statement.textOrNull(0)?.let(keys::add)
                }
            }
            keys
        }
    }

    fun getStoredTafsirInfos(): List<TafsirInfoModel> {
        return use { db ->
            val infos = mutableListOf<TafsirInfoModel>()
            db.prepare(
                """
                SELECT ${QuranTafsirInfoEntry.COL_TAFSIR_KEY},
                       ${QuranTafsirInfoEntry.COL_BOOK_NAME},
                       ${QuranTafsirInfoEntry.COL_AUTHOR_NAME},
                       ${QuranTafsirInfoEntry.COL_LANG_CODE},
                       ${QuranTafsirInfoEntry.COL_LANG_NAME},
                       ${QuranTafsirInfoEntry.COL_IS_DOWNLOADED}
                FROM ${QuranTafsirInfoEntry.TABLE_NAME}
                """.trimIndent()
            ).use { statement ->
                while (statement.step()) {
                    val key = statement.textOrNull(0)
                    if (key.isNullOrBlank()) continue
                    infos += TafsirInfoModel(
                        key = key,
                        name = statement.textOrNull(1).orEmpty(),
                        author = statement.textOrNull(2).orEmpty(),
                        langCode = statement.textOrNull(3).orEmpty(),
                        langName = statement.textOrNull(4).orEmpty(),
                    ).apply {
                        isDownloaded = statement.getLong(5) == 1L
                    }
                }
            }
            infos
        }
    }

    fun deleteTafsirData(tafsirKey: String) {
        use { db ->
            db.execSQL("BEGIN IMMEDIATE")
            try {
                db.prepare(
                    "DELETE FROM ${QuranTafsirInfoEntry.TABLE_NAME} WHERE ${QuranTafsirInfoEntry.COL_TAFSIR_KEY} = ?"
                ).use { statement ->
                    statement.bindText(1, tafsirKey)
                    statement.step()
                }
                db.prepare(
                    "DELETE FROM ${QuranTafsirEntry.TABLE_NAME} WHERE ${QuranTafsirEntry.COL_TAFSIR_KEY} = ?"
                ).use { statement ->
                    statement.bindText(1, tafsirKey)
                    statement.step()
                }
                db.execSQL("COMMIT")
            } catch (error: Throwable) {
                db.execSQL("ROLLBACK")
                throw error
            }
        }
    }

    private fun <T> use(block: (SQLiteConnection) -> T): T {
        val file = Platform.files.databaseDirectory / DB_NAME

        FileSystem.SYSTEM.createDirectories(file)

        val connection = openBundled(file, DB_VERSION) { db ->
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS ${QuranTafsirInfoEntry.TABLE_NAME} (" +
                        "${QuranTafsirInfoEntry.COL_TAFSIR_KEY} TEXT PRIMARY KEY," +
                        "${QuranTafsirInfoEntry.COL_LANG_CODE} TEXT," +
                        "${QuranTafsirInfoEntry.COL_LANG_NAME} TEXT," +
                        "${QuranTafsirInfoEntry.COL_BOOK_NAME} TEXT," +
                        "${QuranTafsirInfoEntry.COL_AUTHOR_NAME} TEXT," +
                        "${QuranTafsirInfoEntry.COL_IS_DOWNLOADED} INTEGER," +
                        "${QuranTafsirInfoEntry.COL_LAST_UPDATED} INTEGER)"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS ${QuranTafsirEntry.TABLE_NAME} (" +
                        "${QuranTafsirEntry._ID} TEXT PRIMARY KEY," +
                        "${QuranTafsirEntry.COL_TAFSIR_KEY} TEXT," +
                        "${QuranTafsirEntry.COL_CHAPTER_NO} TEXT," +
                        "${QuranTafsirEntry.COL_FROM_VERSE_NO} INTEGER," +
                        "${QuranTafsirEntry.COL_TO_VERSE_NO} INTEGER," +
                        "${QuranTafsirEntry.COL_TEXT} TEXT," +
                        "${QuranTafsirEntry.COL_VERSION} TEXT," +
                        "${QuranTafsirEntry.COL_LAST_UPDATED} INTEGER)"
            )
        }

        return connection.use(block)
    }

    private fun makeVerseKey(chapterNo: Int, verseNo: Int) = "$chapterNo:$verseNo"

    private fun makeTafsirVerseId(tafsirKey: String, verseKey: String) = "$tafsirKey:$verseKey"
}
