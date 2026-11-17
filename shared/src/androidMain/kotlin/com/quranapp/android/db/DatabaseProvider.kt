package com.quranapp.android.db

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.quranapp.android.db.DatabaseProvider.replaceDatabaseIfStale
import com.quranapp.android.db.migrations.ExternalQuranDatabaseMigrations
import com.quranapp.android.db.searchindex.SearchIndexDatabase
import com.quranapp.android.platform.PlatformServices
import com.quranapp.android.repository.QuranRepository
import com.quranapp.android.repository.TopicsRepository
import com.quranapp.android.repository.TranslationRepository
import com.quranapp.android.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileInputStream
import java.io.IOException

object DatabaseProvider {
    private lateinit var applicationContext: Context

    fun install(context: Context) {
        applicationContext = context.applicationContext
    }

    fun appContext(): Context = applicationContext

    private class LazySingleton<T>(private val factory: (Context) -> T) {
        @Volatile
        private var instance: T? = null

        fun get(context: Context): T {
            return instance ?: synchronized(this) {
                instance ?: factory(context.applicationContext).also { instance = it }
            }
        }
    }

    private val userDatabase = LazySingleton { appContext ->
        Room.databaseBuilder<UserDatabase>(
            context = appContext,
            name = appContext.getDatabasePath("user_db").absolutePath,
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .addMigrations(
                UserDatabase.MIGRATION_1_2,
                UserDatabase.MIGRATION_2_3,
                UserDatabase.MIGRATION_3_4,
                UserDatabase.MIGRATION_4_5,
            )
            .fallbackToDestructiveMigration(false)
            .build()
    }

    private val userRepository = LazySingleton { appContext ->
        UserRepository(getUserDatabase(appContext))
    }

    private val quranDatabase = LazySingleton { appContext ->
        replaceDatabaseIfStale(
            appContext,
            QURAN_DATABASE_NAME,
            QURAN_DATABASE_VERSION,
        )

        installBundledDatabase(appContext, QURAN_DATABASE_NAME, "db/quranapp.db")

        Room.databaseBuilder<QuranDatabase>(
            context = appContext,
            name = appContext.getDatabasePath(QURAN_DATABASE_NAME).absolutePath,
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .fallbackToDestructiveMigration(true)
            .build()
    }

    private val externalQuranDatabase = LazySingleton { appContext ->
        Room.databaseBuilder<ExternalQuranDatabase>(
            context = appContext,
            name = appContext.getDatabasePath("quranapp_external").absolutePath,
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .addMigrations(
                ExternalQuranDatabaseMigrations.MIGRATION_1_2,
                ExternalQuranDatabaseMigrations.MIGRATION_2_3,
                ExternalQuranDatabaseMigrations.MIGRATION_3_4,
                ExternalQuranDatabaseMigrations.MIGRATION_4_5,
            )
            .fallbackToDestructiveMigration(false)
            .build()
    }

    private val translationRepository = LazySingleton { appContext ->
        TranslationRepository(appContext, getExternalQuranDatabase(appContext))
    }

    private val quranRepository = LazySingleton { appContext ->
        QuranRepository(
            getQuranDatabase(appContext),
            getExternalQuranDatabase(appContext)
        )
    }

    private val searchIndexDatabase = LazySingleton { appContext ->
        Room.databaseBuilder<SearchIndexDatabase>(
            context = appContext,
            name = appContext.getDatabasePath("SearchIndex.db").absolutePath,
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .fallbackToDestructiveMigration(true)
            .build()
    }

    private val topicsDatabase = LazySingleton { appContext ->
        replaceDatabaseIfStale(
            appContext,
            TOPICS_DATABASE_NAME,
            TOPICS_DATABASE_VERSION,
        )

        installBundledDatabase(appContext, TOPICS_DATABASE_NAME, "db/topics.db")

        Room.databaseBuilder<TopicsDatabase>(
            context = appContext,
            name = appContext.getDatabasePath(TOPICS_DATABASE_NAME).absolutePath,
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .fallbackToDestructiveMigration(true)
            .build()
    }

    private val topicsRepository = LazySingleton { appContext ->
        TopicsRepository(getTopicsDatabase(appContext)) {
            getTranslationRepository(appContext)
        }
    }

    fun getUserDatabase(context: Context): UserDatabase = userDatabase.get(context)

    fun getUserRepository(context: Context): UserRepository = userRepository.get(context)

    private fun getQuranDatabase(context: Context): QuranDatabase = quranDatabase.get(context)

    fun getExternalQuranDatabase(context: Context): ExternalQuranDatabase =
        externalQuranDatabase.get(context)

    fun getTranslationRepository(context: Context): TranslationRepository =
        translationRepository.get(context)

    fun getQuranRepository(context: Context): QuranRepository = quranRepository.get(context)

    fun getSearchIndexDatabase(context: Context): SearchIndexDatabase =
        searchIndexDatabase.get(context)

    private fun getTopicsDatabase(context: Context): TopicsDatabase = topicsDatabase.get(context)

    fun getTopicsRepository(context: Context): TopicsRepository = topicsRepository.get(context)

    /**
     * Copies a composeResources database into the app database directory once.
     * [replaceDatabaseIfStale] removes an older copy first so the next open installs the new file.
     */
    private fun installBundledDatabase(context: Context, dbName: String, relativePath: String) {
        val dbFile = context.getDatabasePath(dbName)
        if (dbFile.exists()) return

        dbFile.parentFile?.mkdirs()

        val bytes = runBlocking { PlatformServices.assets.readAssetBytes(relativePath) }
        val tmp = File(dbFile.parentFile, "${dbFile.name}.tmp")

        tmp.writeBytes(bytes)

        if (!tmp.renameTo(dbFile)) {
            dbFile.writeBytes(bytes)
            tmp.delete()
        }
    }

    /**
     * Deletes an older destructible db file, including its WAL, before Room opens it.
     */
    private fun replaceDatabaseIfStale(context: Context, dbName: String, newVersion: Int) {
        val dbFile = context.getDatabasePath(dbName)
        val dbFilePath = dbFile.path

        if (!dbFile.exists()) return
        if (readSqliteUserVersion(dbFile) == newVersion) return

        context.deleteDatabase(dbName)

        listOf("", "-wal", "-shm", "-journal").forEach { suffix ->
            File(dbFilePath + suffix).delete()
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

internal fun openBundled(
    path: String,
    version: Int,
    create: (SQLiteConnection) -> Unit,
): SQLiteConnection {
    val connection = BundledSQLiteDriver().open(path)
    val current = connection.prepare("PRAGMA user_version").use { statement ->
        statement.step()
        statement.getLong(0).toInt()
    }
    if (current == 0) {
        create(connection)
        connection.execSQL("PRAGMA user_version = $version")
    }
    return connection
}

internal fun SQLiteStatement.bindTextOrNull(index: Int, value: String?) {
    if (value == null) bindNull(index) else bindText(index, value)
}

internal fun SQLiteStatement.textOrNull(index: Int): String? {
    return if (isNull(index)) null else getText(index)
}
