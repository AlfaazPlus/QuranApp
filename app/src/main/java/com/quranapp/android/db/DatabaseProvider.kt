package com.quranapp.android.db

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.quranapp.android.db.migrations.ExternalQuranDatabaseMigrations
import com.quranapp.android.db.searchindex.SearchIndexDatabase
import com.quranapp.android.db.translation.QuranTranslDBHelper
import com.quranapp.android.repository.QuranRepository
import com.quranapp.android.repository.TopicsRepository
import com.quranapp.android.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import java.io.File

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
                repairPackagedFtsQuoting(appContext.getDatabasePath("quranapp"))
                Room.databaseBuilder(
                    appContext,
                    QuranDatabase::class.java,
                    "quranapp"
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

    /**
     * Room 3 stores FTS options with backtick quotes. Databases copied from the older asset used
     * single quotes, which fails schema validation. Rewrite those option strings once.
     */
    private fun repairPackagedFtsQuoting(dbFile: File) {
        val marker = File(dbFile.parentFile, "${dbFile.name}.fts5-quote-v1")
        if (marker.exists() || !dbFile.exists()) return
        try {
            BundledSQLiteDriver().open(dbFile.absolutePath).use { connection ->
                var changed = false
                for (name in listOf("surah_search_aliases_fts", "arabic_search")) {
                    val sql = connection.readMasterSql(name) ?: continue

                    val updated = sql.replace(
                        "content='surah_search_aliases'",
                        "content=`surah_search_aliases`"
                    )
                        .replace("content_rowid='id'", "content_rowid=`id`")
                        .replace("tokenize='unicode61'", "tokenize=`unicode61`")

                    if (updated == sql) continue
                    if (!changed) {
                        connection.execSQL("PRAGMA writable_schema = ON")
                        changed = true
                    }
                    connection.prepare(
                        "UPDATE sqlite_master SET sql = ? WHERE name = ?"
                    ).use { statement ->
                        statement.bindText(1, updated)
                        statement.bindText(2, name)
                        statement.step()
                    }
                }
                if (changed) {
                    val schemaVersion =
                        connection.prepare("PRAGMA schema_version").use { statement ->
                            statement.step()
                            statement.getLong(0)
                        }
                    connection.execSQL("PRAGMA writable_schema = OFF")
                    connection.execSQL("PRAGMA schema_version = ${schemaVersion + 1}")
                }
            }
            marker.parentFile?.mkdirs()
            marker.writeText("1")
        } catch (_: Exception) {
            dbFile.delete()
            File("${dbFile.path}-wal").delete()
            File("${dbFile.path}-shm").delete()
            File("${dbFile.path}-journal").delete()
        }
    }

    private fun SQLiteConnection.readMasterSql(tableName: String): String? {
        return prepare("SELECT sql FROM sqlite_master WHERE name = ?").use { statement ->
            statement.bindText(1, tableName)
            if (statement.step()) statement.getText(0) else null
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
