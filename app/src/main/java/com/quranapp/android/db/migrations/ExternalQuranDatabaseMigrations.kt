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
                        PRIMARY KEY(`bundle_key`)
                    )
                    """.trimIndent(),
            )

            db.execSQL(
                """
                    CREATE TABLE IF NOT EXISTS `atlas_word_shapes` (
                        `bundle_key` TEXT NOT NULL,
                        `word` TEXT NOT NULL,
                        `placements_json` TEXT NOT NULL,
                        PRIMARY KEY(`bundle_key`, `word`)
                    )
                    """.trimIndent(),
            )
            db.execSQL(
                """
                    CREATE INDEX IF NOT EXISTS `idx_atlas_word_shapes_bundle`
                    ON `atlas_word_shapes` (`bundle_key`)
                    """.trimIndent(),
            )
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                    CREATE TABLE IF NOT EXISTS `wbw_audio_timing` (
                        `audio_id` TEXT NOT NULL,
                        `ayah_id` INTEGER NOT NULL,
                        `word_index` INTEGER NOT NULL,
                        `start_millis` INTEGER NOT NULL,
                        `end_millis` INTEGER NOT NULL,
                        PRIMARY KEY(`audio_id`, `ayah_id`, `word_index`)
                    )
                    """.trimIndent(),
            )
            db.execSQL(
                """
                    CREATE INDEX IF NOT EXISTS `idx_wbw_audio_timing_ayah_word_index`
                    ON `wbw_audio_timing` (`ayah_id`, `word_index`)
                    """.trimIndent(),
            )
        }
    }
}
