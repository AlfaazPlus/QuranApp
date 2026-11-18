package com.quranapp.android.db

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL

internal fun openBundled(
    path: String,
    version: Int,
    create: (SQLiteConnection) -> Unit,
): SQLiteConnection {
    val connection = BundledSQLiteDriver().open(path)
    val current = connection.prepare("PRAGMA user_version").use { statement ->
        statement.step()
        statement.getLong(0).toInt()
    }
    if (current == 0) {
        create(connection)
        connection.execSQL("PRAGMA user_version = $version")
    }
    return connection
}

internal fun SQLiteStatement.bindTextOrNull(index: Int, value: String?) {
    if (value == null) bindNull(index) else bindText(index, value)
}

internal fun SQLiteStatement.textOrNull(index: Int): String? {
    return if (isNull(index)) null else getText(index)
}
