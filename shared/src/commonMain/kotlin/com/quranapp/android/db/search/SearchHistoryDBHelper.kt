package com.quranapp.android.db.search

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.quranapp.android.db.openBundled
import com.quranapp.android.db.textOrNull
import com.quranapp.android.platform.PlatformServices
import com.quranapp.android.utils.miscellenous.DateUtils
import okio.FileSystem

private const val DB_NAME = "SearchHistory.db"
private const val DB_VERSION = 1

object SearchHistoryEntity {
    const val _ID: String = "_id"
    const val TABLE_NAME: String = "SearchHistory"
    const val COL_TEXT: String = "Text"
    const val COL_DATE: String = "Date"
}

data class SearchHistoryEntry(
    val id: Int,
    val text: String,
    val date: String,
)

class SearchHistoryDBHelper() {
    fun addToHistory(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        if (updateHistory(trimmed)) {
            return
        }

        use { db ->
            db.prepare(
                """
                INSERT INTO ${SearchHistoryEntity.TABLE_NAME} (
                    ${SearchHistoryEntity.COL_TEXT},
                    ${SearchHistoryEntity.COL_DATE}
                ) VALUES (?, ?)
                """.trimIndent()
            ).use { statement ->
                statement.bindText(1, trimmed)
                statement.bindText(2, currentDate())
                statement.step()
            }

            true
        }
    }

    fun removeFromHistory(historyId: Int) {
        use { db ->
            db.prepare(
                "DELETE FROM ${SearchHistoryEntity.TABLE_NAME} WHERE ${SearchHistoryEntity._ID} = ?"
            ).use { statement ->
                statement.bindLong(1, historyId.toLong())
                statement.step()
            }

            true
        }
    }

    fun isInHistory(text: String?): Boolean {
        if (text == null) return false

        return use { db ->
            db.prepare(
                """
                SELECT COUNT(*)
                FROM ${SearchHistoryEntity.TABLE_NAME}
                WHERE LOWER(${SearchHistoryEntity.COL_TEXT}) = LOWER(?)
                """.trimIndent()
            ).use { statement ->
                statement.bindText(1, text.trim())
                statement.step()
                statement.getLong(0) > 0
            }
        }
    }

    fun updateHistory(text: String): Boolean {
        return use { db ->
            db.prepare(
                """
                UPDATE ${SearchHistoryEntity.TABLE_NAME}
                SET ${SearchHistoryEntity.COL_DATE} = ?
                WHERE LOWER(${SearchHistoryEntity.COL_TEXT}) = LOWER(?)
                """.trimIndent()
            ).use { statement ->
                statement.bindText(1, currentDate())
                statement.bindText(2, text.trim())
                statement.step()
            }

            isInHistoryInternal(db, text.trim())
        }
    }

    fun getHistories(query: String?): ArrayList<SearchHistoryEntry> {
        return use { db ->
            val histories = arrayListOf<SearchHistoryEntry>()

            val sql = if (query.isNullOrEmpty()) {
                """
                SELECT ${SearchHistoryEntity._ID},
                       ${SearchHistoryEntity.COL_TEXT},
                       ${SearchHistoryEntity.COL_DATE}
                FROM ${SearchHistoryEntity.TABLE_NAME}
                ORDER BY ${SearchHistoryEntity.COL_DATE} DESC
                """.trimIndent()
            } else {
                """
                SELECT ${SearchHistoryEntity._ID},
                       ${SearchHistoryEntity.COL_TEXT},
                       ${SearchHistoryEntity.COL_DATE}
                FROM ${SearchHistoryEntity.TABLE_NAME}
                WHERE ${SearchHistoryEntity.COL_TEXT} LIKE ?
                ORDER BY ${SearchHistoryEntity.COL_DATE} DESC
                """.trimIndent()
            }

            db.prepare(sql).use { statement ->
                if (!query.isNullOrEmpty()) {
                    statement.bindText(1, "%$query%")
                }

                while (statement.step()) {
                    val id = statement.getLong(0).toInt()
                    val text = statement.textOrNull(1).orEmpty()
                    val date = statement.textOrNull(2).orEmpty()

                    histories.add(SearchHistoryEntry(id, text, date))
                }
            }

            histories
        }
    }

    fun getHistoriesCount(): Long {
        return use { db ->
            db.prepare(
                "SELECT COUNT(*) FROM ${SearchHistoryEntity.TABLE_NAME}"
            ).use { statement ->
                statement.step()
                statement.getLong(0)
            }
        }
    }

    fun clearHistories() {
        use { db ->
            db.execSQL("DELETE FROM ${SearchHistoryEntity.TABLE_NAME}")
        }
    }

    private fun isInHistoryInternal(
        db: SQLiteConnection,
        text: String,
    ): Boolean {
        return db.prepare(
            """
            SELECT COUNT(*)
            FROM ${SearchHistoryEntity.TABLE_NAME}
            WHERE LOWER(${SearchHistoryEntity.COL_TEXT}) = LOWER(?)
            """.trimIndent()
        ).use { statement ->
            statement.bindText(1, text)
            statement.step()
            statement.getLong(0) > 0
        }
    }

    private fun currentDate(): String {
        return DateUtils.format(DateUtils.now(), DateUtils.SYSTEM)
    }

    private fun <T> use(block: (SQLiteConnection) -> T): T {
        val path = PlatformServices.files.databaseDirectory / DB_NAME

        FileSystem.SYSTEM.createDirectories(path)

        val connection = openBundled(path, DB_VERSION) { db ->
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS ${SearchHistoryEntity.TABLE_NAME} (
                    ${SearchHistoryEntity._ID} INTEGER PRIMARY KEY,
                    ${SearchHistoryEntity.COL_TEXT} TEXT,
                    ${SearchHistoryEntity.COL_DATE} TEXT
                )
                """.trimIndent()
            )
        }

        return connection.use(block)
    }
}
