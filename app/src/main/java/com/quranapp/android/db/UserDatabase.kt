package com.quranapp.android.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.quranapp.android.db.converters.DbConverters
import com.quranapp.android.db.dao.BookmarkDao
import com.quranapp.android.db.entities.BookmarkEntity

@Database(
    entities = [BookmarkEntity::class],
    version = 1,
)
@TypeConverters(DbConverters::class)
abstract class UserDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
}