package com.quranapp.android.db.bookmark

import android.content.Context
import androidx.room.Room

object BookmarkDatabaseProvider {

    @Volatile
    private var instance: BookmarkDatabase? = null

    @Volatile
    private var repository: BookmarkRepository? = null

    fun getDatabase(context: Context): BookmarkDatabase {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                BookmarkDatabase::class.java,
                BookmarkDbHelper.DB_NAME
            )
                .fallbackToDestructiveMigration(false)
                .build()
                .also { instance = it }
        }
    }

    fun getRepository(context: Context): BookmarkRepository {
        val database = getDatabase(context)
        return repository ?: BookmarkRepository(
            context,
            database
        ).also { repository = it }
    }
}