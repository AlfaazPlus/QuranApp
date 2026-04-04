package com.quranapp.android.db.bookmark

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [BookmarkEntity::class],
    version = BookmarkDbHelper.DB_VERSION,
    exportSchema = false
)

abstract class BookmarkDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
}