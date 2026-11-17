package com.quranapp.android.db

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase

object AndroidDatabaseFactory {
    inline fun <reified T : RoomDatabase> create(
        context: Context,
        name: String
    ): RoomDatabase.Builder<T> {

        val appContext = context.applicationContext
        val dbFile = appContext.getDatabasePath(name)

        return Room.databaseBuilder<T>(
            context = appContext,
            name = dbFile.absolutePath
        )
    }
}
