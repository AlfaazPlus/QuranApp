package com.quranapp.android.db

import androidx.room3.DaoReturnTypeConverters
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.ColumnTypeConverters
import androidx.room3.migration.Migration
import androidx.room3.paging.PagingSourceDaoReturnTypeConverter
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.quranapp.android.db.converters.DbConverters
import com.quranapp.android.db.dao.BookmarkDao
import com.quranapp.android.db.dao.CollectionDao
import com.quranapp.android.db.dao.ReadHistoryDao
import com.quranapp.android.db.entities.user.BookmarkEntity
import com.quranapp.android.db.entities.user.CollectionEntity
import com.quranapp.android.db.entities.user.CollectionItemEntity
import com.quranapp.android.db.entities.user.ReadHistoryEntity

@Database(
    entities = [
        BookmarkEntity::class,
        ReadHistoryEntity::class,
        CollectionEntity::class,
        CollectionItemEntity::class,
    ],
    version = 3,
)
@ColumnTypeConverters(DbConverters::class)
@DaoReturnTypeConverters(PagingSourceDaoReturnTypeConverter::class)
abstract class UserDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun readHistoryDao(): ReadHistoryDao
    abstract fun collectionDao(): CollectionDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `user_collections` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT,
                        `icon_key` TEXT,
                        `color` TEXT,
                        `sort_index` INTEGER,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `user_collection_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `collection_id` INTEGER NOT NULL,
                        `chapter_no` INTEGER NOT NULL,
                        `verse_no` INTEGER NOT NULL,
                        `note` TEXT,
                        `sort_index` INTEGER NOT NULL,
                        `added_at` INTEGER NOT NULL,
                        FOREIGN KEY(`collection_id`) REFERENCES `user_collections`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                connection.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS `index_user_collection_items_collection_id_chapter_no_verse_no`
                    ON `user_collection_items` (`collection_id`, `chapter_no`, `verse_no`)
                    """.trimIndent()
                )
                connection.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_user_collection_items_collection_id`
                    ON `user_collection_items` (`collection_id`)
                    """.trimIndent()
                )
            }
        }
    }
}
