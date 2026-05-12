package com.quranapp.android.db

import android.content.Context
import androidx.room.Room
import com.quranapp.android.db.migrations.ExternalQuranDatabaseMigrations
import com.quranapp.android.db.searchindex.SearchIndexDatabase
import com.quranapp.android.db.translation.QuranTranslDBHelper
import com.quranapp.android.repository.QuranRepository
import com.quranapp.android.repository.TopicsRepository
import com.quranapp.android.repository.UserRepository

object DatabaseProvider {

    @Volatile
    private var userDatabase: UserDatabase? = null

    @Volatile
    private var userRepository: UserRepository? = null

    @Volatile
    private var quranDatabase: QuranDatabase? = null

    @Volatile
    private var quranRepository: QuranRepository? = null

    @Volatile
    private var externalQuranDatabase: ExternalQuranDatabase? = null

    @Volatile
    private var searchIndexDatabase: SearchIndexDatabase? = null

    @Volatile
    private var topicsDatabase: TopicsDatabase? = null

    @Volatile
    private var topicsRepository: TopicsRepository? = null

    @Volatile
    private var quranTranslDbHelper: QuranTranslDBHelper? = null

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

    fun getExternalQuranDatabase(context: Context): ExternalQuranDatabase {
        return externalQuranDatabase ?: synchronized(this) {
            externalQuranDatabase ?: Room.databaseBuilder(
                context.applicationContext,
                ExternalQuranDatabase::class.java,
                "quranapp_external"
            )
                .addMigrations(
                    ExternalQuranDatabaseMigrations.MIGRATION_1_2,
                    ExternalQuranDatabaseMigrations.MIGRATION_2_3,
                    ExternalQuranDatabaseMigrations.MIGRATION_3_4,
                )
                .fallbackToDestructiveMigration(false)
                .build()
                .also { externalQuranDatabase = it }
        }
    }

    fun getQuranRepository(context: Context): QuranRepository {
        return quranRepository ?: synchronized(this) {
            quranRepository ?: QuranRepository(
                getQuranDatabase(context),
                getExternalQuranDatabase(context)
            ).also { quranRepository = it }
        }
    }

    fun getSearchIndexDatabase(context: Context): SearchIndexDatabase {
        return searchIndexDatabase ?: synchronized(this) {
            searchIndexDatabase ?: Room.databaseBuilder(
                context.applicationContext,
                SearchIndexDatabase::class.java,
                "SearchIndex.db",
            )
                .fallbackToDestructiveMigration(true)
                .build()
                .also { searchIndexDatabase = it }
        }
    }

    private fun getTopicsDatabase(context: Context): TopicsDatabase {
        return topicsDatabase ?: synchronized(this) {
            topicsDatabase ?: Room.databaseBuilder(
                context.applicationContext,
                TopicsDatabase::class.java,
                "topics"
            )
                .createFromAsset("db/topics.db")
                .fallbackToDestructiveMigration(true)
                .build()
                .also { topicsDatabase = it }
        }
    }

    fun getTopicsRepository(context: Context): TopicsRepository {
        return topicsRepository ?: synchronized(this) {
            topicsRepository ?: TopicsRepository(
                context.applicationContext,
                getTopicsDatabase(context),
            ).also { topicsRepository = it }
        }
    }

    fun getQuranTranslDBHelper(context: Context): QuranTranslDBHelper {
        return quranTranslDbHelper ?: synchronized(this) {
            quranTranslDbHelper ?: QuranTranslDBHelper(context.applicationContext).also {
                quranTranslDbHelper = it
            }
        }
    }

    fun closeAll() {
        synchronized(this) {
            userDatabase?.close(); userDatabase = null
            quranDatabase?.close(); quranDatabase = null
            externalQuranDatabase?.close(); externalQuranDatabase = null
            searchIndexDatabase?.close(); searchIndexDatabase = null
            topicsDatabase?.close(); topicsDatabase = null
            userRepository = null
            quranRepository = null
            topicsRepository = null
            quranTranslDbHelper = null
        }
    }
}
