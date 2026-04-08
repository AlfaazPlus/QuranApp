package com.quranapp.android.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.quranapp.android.db.converters.DbConverters
import com.quranapp.android.db.dao.BookmarkDao
import com.quranapp.android.db.dao.ReadHistoryDao
import com.quranapp.android.db.entities.BookmarkEntity
import com.quranapp.android.db.entities.ReadHistoryEntity

@Database(
    entities = [BookmarkEntity::class, ReadHistoryEntity::class],
    version = 2,
)
@TypeConverters(DbConverters::class)
abstract class UserDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun readHistoryDao(): ReadHistoryDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `read_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `read_type` TEXT NOT NULL,
                        `reader_mode` TEXT NOT NULL,
                        `division_no` INTEGER NOT NULL DEFAULT 0,
                        `chapter_no` INTEGER NOT NULL DEFAULT 0,
                        `from_verse_no` INTEGER NOT NULL DEFAULT 0,
                        `to_verse_no` INTEGER NOT NULL DEFAULT 0,
                        `mushaf_id` INTEGER NOT NULL DEFAULT 0,
                        `page_no` INTEGER,
                        `datetime` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }
    }
}