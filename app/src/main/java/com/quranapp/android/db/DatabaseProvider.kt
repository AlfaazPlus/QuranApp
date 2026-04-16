package com.quranapp.android.db

import android.content.Context
import androidx.room.Room
import com.quranapp.android.repository.QuranRepository
import com.quranapp.android.repository.UserRepository

object DatabaseProvider {

    @Volatile
    private var userDatabase: UserDatabase? = null

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
                .addMigrations(UserDatabase.MIGRATION_1_2)
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

    private fun getQuranDatabase(context: Context): QuranDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            QuranDatabase::class.java,
            "quranapp"
        )
            .createFromAsset("db/quranapp.db")
            .fallbackToDestructiveMigration(true)
            .build()
    }

    private fun getExternalQuranDatabase(context: Context): ExternalQuranDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            ExternalQuranDatabase::class.java,
            "quranapp_external"
        )
            .fallbackToDestructiveMigration(false)
            .build()
    }

    fun getQuranRepository(context: Context): QuranRepository {
        return quranRepository ?: synchronized(this) {
            quranRepository ?: QuranRepository(
                getQuranDatabase(context),
                getExternalQuranDatabase(context)
            ).also { quranRepository = it }
        }
    }
}