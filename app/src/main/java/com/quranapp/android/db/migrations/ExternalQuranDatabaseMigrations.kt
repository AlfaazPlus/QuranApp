package com.quranapp.android.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object ExternalQuranDatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                    CREATE TABLE IF NOT EXISTS `atlas_bundles` (
                        `bundle_key` TEXT NOT NULL,
                        `meta_json` TEXT NOT NULL,
                        `layer_json` TEXT NOT NULL,
                        `image_png` BLOB NOT NULL,
                        PRIMARY KEY(`bundle_key`)
                    )
                    """.trimIndent()
            )

            db.execSQL(
                """
                    CREATE TABLE IF NOT EXISTS `atlas_word_shapes` (
                        `bundle_key` TEXT NOT NULL,
                        `word` TEXT NOT NULL,
                        `placements_json` TEXT NOT NULL,
                        PRIMARY KEY(`bundle_key`, `word`)
                    )
                    """.trimIndent()
            )
            db.execSQL(
                """
                    CREATE INDEX IF NOT EXISTS `idx_atlas_word_shapes_bundle`
                    ON `atlas_word_shapes` (`bundle_key`)
                    """.trimIndent()
            )
        }
    }
}
