package com.quranapp.android.db

import androidx.room3.RoomDatabase
import com.quranapp.android.db.searchindex.SearchIndexDatabase

data class DatabaseBuilders(
    val user: RoomDatabase.Builder<UserDatabase>,
    val quran: RoomDatabase.Builder<QuranDatabase>,
    val externalQuran: RoomDatabase.Builder<ExternalQuranDatabase>,
    val searchIndex: RoomDatabase.Builder<SearchIndexDatabase>,
    val topics: RoomDatabase.Builder<TopicsDatabase>,
)
