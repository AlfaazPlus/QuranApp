package com.quranapp.android.db

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import okio.Path

internal fun openBundled(
    path: Path,
    version: Int,
    create: (SQLiteConnection) -> Unit,
): SQLiteConnection {
    val connection = BundledSQLiteDriver().open(path.toString())

    try {
        val current = connection.prepare("PRAGMA user_version").use { statement ->
            statement.step()
            statement.getLong(0).toInt()
        }

        if (current < version) {
            connection.execSQL("BEGIN IMMEDIATE")

            try {
                create(connection)
                connection.execSQL("PRAGMA user_version = $version")
                connection.execSQL("COMMIT")
            } catch (error: Throwable) {
                connection.execSQL("ROLLBACK")
                throw error
            }
        }

        return connection
    } catch (error: Throwable) {
        connection.close()
        throw error
    }
}

internal fun SQLiteStatement.bindTextOrNull(index: Int, value: String?) {
    if (value == null) bindNull(index) else bindText(index, value)
}

internal fun SQLiteStatement.textOrNull(index: Int): String? {
    return if (isNull(index)) null else getText(index)
}
