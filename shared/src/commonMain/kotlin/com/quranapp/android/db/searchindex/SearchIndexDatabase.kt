package com.quranapp.android.db.searchindex

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor

const val SEARCH_INDEX_DB_VERSION = 3
const val SEARCH_INDEX_CONTENT_VERSION = 4

@Database(
    entities = [
        TranslationSearchContentEntity::class,
        TranslationSearchFtsEntity::class,
        TranslationIndexMetaEntity::class,
    ],
    version = SEARCH_INDEX_DB_VERSION,
    exportSchema = false,
)
@ConstructedBy(SearchIndexDatabaseConstructor::class)
abstract class SearchIndexDatabase : RoomDatabase() {
    abstract fun searchIndexDao(): SearchIndexDao
}

@Suppress("KotlinNoActualForExpect")
expect object SearchIndexDatabaseConstructor : RoomDatabaseConstructor<SearchIndexDatabase>
