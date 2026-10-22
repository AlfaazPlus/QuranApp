package com.quranapp.android.db

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.quranapp.android.db.migrations.ExternalQuranDatabaseMigrations
import com.quranapp.android.db.searchindex.SearchIndexDatabase
import com.quranapp.android.db.translation.QuranTranslDBHelper
import com.quranapp.android.repository.QuranRepository
import com.quranapp.android.repository.TopicsRepository
import com.quranapp.android.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.io.FileInputStream
import java.io.IOException

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
                .addMigrations(UserDatabase.MIGRATION_1_2, UserDatabase.MIGRATION_2_3)
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
            quranDatabase ?: run {
                val appContext = context.applicationContext

                replaceQuranDatabaseIfStale(appContext)

                Room.databaseBuilder(
                    appContext,
                    QuranDatabase::class.java,
                    QURAN_DATABASE_NAME
                )
                    .createFromAsset("db/quranapp.db")
                    .setDriver(BundledSQLiteDriver())
                    .setQueryCoroutineContext(Dispatchers.IO)
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { quranDatabase = it }
            }
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
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
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

    /**
     * Deletes an older installed quranapp file, including its WAL, before Room opens it.
     */
    private fun replaceQuranDatabaseIfStale(context: Context) {
        val dbFile = context.getDatabasePath(QURAN_DATABASE_NAME)

        if (!dbFile.exists()) return
        if (readSqliteUserVersion(dbFile) == QURAN_DATABASE_VERSION) return

        deleteDatabaseFiles(context, QURAN_DATABASE_NAME)
    }

    private fun deleteDatabaseFiles(context: Context, name: String) {
        context.deleteDatabase(name)

        val dbFile = context.getDatabasePath(name)

        listOf("", "-wal", "-shm", "-journal").forEach { suffix ->
            File(dbFile.path + suffix).delete()
        }
    }

    private fun readSqliteUserVersion(file: File): Int {
        return try {
            FileInputStream(file).use { input ->
                var remaining = 60L

                while (remaining > 0) {
                    val skipped = input.skip(remaining)
                    if (skipped <= 0L) return -1
                    remaining -= skipped
                }

                val buf = ByteArray(4)
                if (input.read(buf) != 4) return -1

                ((buf[0].toInt() and 0xFF) shl 24) or
                        ((buf[1].toInt() and 0xFF) shl 16) or
                        ((buf[2].toInt() and 0xFF) shl 8) or
                        (buf[3].toInt() and 0xFF)
            }
        } catch (_: IOException) {
            -1
        }
    }
}
