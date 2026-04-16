package com.quranapp.android.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.quranapp.android.db.converters.QuranConverters
import com.quranapp.android.db.dao.WbwDao

@Database(
    entities = [
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(QuranConverters::class)
abstract class ExternalQuranDatabase : RoomDatabase() {
    abstract fun wbwDao(): WbwDao
}