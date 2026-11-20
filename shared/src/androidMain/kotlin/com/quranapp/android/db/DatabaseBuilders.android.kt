package com.quranapp.android.db

import android.content.Context
import androidx.room3.Room
import com.quranapp.android.db.searchindex.SearchIndexDatabase

fun createDatabaseBuilders(context: Context): DatabaseBuilders {
    val appContext = context.applicationContext

    fun path(name: String) =
        appContext.getDatabasePath(name).absolutePath

    return DatabaseBuilders(
        user = Room.databaseBuilder<UserDatabase>(
            context = appContext,
            name = path("user_db"),
        ),

        quran = Room.databaseBuilder<QuranDatabase>(
            context = appContext,
            name = path(QURAN_DATABASE_NAME),
        ),

        externalQuran = Room.databaseBuilder<ExternalQuranDatabase>(
            context = appContext,
            name = path("quranapp_external"),
        ),

        searchIndex = Room.databaseBuilder<SearchIndexDatabase>(
            context = appContext,
            name = path("SearchIndex.db"),
        ),

        topics = Room.databaseBuilder<TopicsDatabase>(
            context = appContext,
            name = path(TOPICS_DATABASE_NAME),
        ),
    )
}
