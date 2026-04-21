package com.quranapp.android.db.searchindex

import androidx.room.Database
import androidx.room.RoomDatabase

const val SEARCH_INDEX_DB_VERSION = 1

@Database(
    entities = [
        TranslationSearchContentEntity::class,
        TranslationSearchFtsEntity::class,
        TranslationIndexMetaEntity::class,
    ],
    version = SEARCH_INDEX_DB_VERSION,
    exportSchema = false,
)
abstract class SearchIndexDatabase : RoomDatabase() {
    abstract fun searchIndexDao(): SearchIndexDao
}
