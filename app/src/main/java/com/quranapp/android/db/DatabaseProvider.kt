package com.quranapp.android.db

import android.content.Context
import androidx.room.Room

object DatabaseProvider {

    @Volatile
    private var userDatabase: UserDatabase? = null

    @Volatile
    private var quranDatabase: QuranDatabase? = null

    @Volatile
    private var userRepository: UserRepository? = null

    @Volatile
    private var quranRepository: QuranRepository? = null

    fun getUserDatabase(context: Context): UserDatabase {
        return userDatabase ?: synchronized(this) {
            userDatabase ?: Room.databaseBuilder(
                context.applicationContext,
                UserDatabase::class.java,
                "user_db"
            )
                .fallbackToDestructiveMigration(false)
                .build()
                .also { userDatabase = it }
        }
    }

    fun getUserRepository(context: Context): UserRepository {
        return userRepository ?: synchronized(this) {
            userRepository ?: UserRepository(
                context.applicationContext,
                getUserDatabase(context)
            ).also { userRepository = it }
        }
    }

    fun getQuranDatabase(context: Context): QuranDatabase {
        return quranDatabase ?: synchronized(this) {
            quranDatabase ?: Room.databaseBuilder(
                context.applicationContext,
                QuranDatabase::class.java,
                "quranapp"
            )
                .createFromAsset("db/quranapp.db")
                .fallbackToDestructiveMigration(true)
                .build()
                .also { quranDatabase = it }
        }
    }

    fun getQuranRepository(context: Context): QuranRepository {
        return quranRepository ?: synchronized(this) {
            quranRepository ?: QuranRepository(
                context.applicationContext,
                getQuranDatabase(context)
            ).also { quranRepository = it }
        }
    }
}