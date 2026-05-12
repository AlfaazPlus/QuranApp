package com.quranapp.android.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.utils.reader.QuranScriptUtils
import com.quranapp.android.utils.reader.isQuranAtlasScript
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

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

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DELETE FROM `atlas_bundles`")
            db.execSQL("DROP TABLE IF EXISTS `atlas_word_shapes`")
            db.execSQL(
                """
                    CREATE TABLE IF NOT EXISTS `atlas_word_shapes` (
                        `bundle_key` TEXT NOT NULL,
                        `word` TEXT NOT NULL,
                        `page` INTEGER NOT NULL,
                        `placements_json` TEXT NOT NULL,
                        PRIMARY KEY(`bundle_key`, `word`, `page`)
                    )
                    """.trimIndent(),
            )
            db.execSQL(
                """
                    CREATE INDEX IF NOT EXISTS `idx_atlas_word_shapes_bundle`
                    ON `atlas_word_shapes` (`bundle_key`)
                    """.trimIndent(),
            )

            runBlocking {
                withContext(Dispatchers.IO) {
                    if (ReaderPreferences.getQuranScript().isQuranAtlasScript()) {
                        ReaderPreferences.setQuranScript(QuranScriptUtils.SCRIPT_DEFAULT)
                    }
                }
            }
        }
    }
}
