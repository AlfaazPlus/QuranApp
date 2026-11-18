package com.quranapp.android.db

import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.quranapp.android.db.migrations.ExternalQuranDatabaseMigrations
import com.quranapp.android.db.searchindex.SearchIndexDatabase
import com.quranapp.android.platform.PlatformServices
import com.quranapp.android.repository.QuranRepository
import com.quranapp.android.repository.TopicsRepository
import com.quranapp.android.repository.TranslationRepository
import com.quranapp.android.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.buffer

object DatabaseProvider {
    private const val USER_DB = "user_db"
    private const val EXTERNAL_DB = "quranapp_external"
    private const val SEARCH_DB = "SearchIndex.db"

    private val initLock = Mutex()
    private val fs = FileSystem.SYSTEM

    private data class Instances(
        val userDatabase: UserDatabase,
        val quranDatabase: QuranDatabase,
        val externalDatabase: ExternalQuranDatabase,
        val searchDatabase: SearchIndexDatabase,
        val topicsDatabase: TopicsDatabase,

        val userRepository: UserRepository,
        val translationRepository: TranslationRepository,
        val quranRepository: QuranRepository,
        val topicsRepository: TopicsRepository,
    )

    private var instances: Instances? = null

    /**
     * Called exactly once during application startup.
     * Safe to call more than once.
     */
    suspend fun initialize(builders: DatabaseBuilders) {
        initLock.withLock {
            if (instances != null) return

            val newInstances = withContext(Dispatchers.IO) {
                installBundledDatabase(
                    QURAN_DATABASE_NAME,
                    "db/quranapp.db",
                    QURAN_DATABASE_VERSION,
                )

                installBundledDatabase(
                    TOPICS_DATABASE_NAME,
                    "db/topics.db",
                    TOPICS_DATABASE_VERSION,
                )

                val opened = mutableListOf<RoomDatabase>()

                try {
                    val userDb = builders.user
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
                        .also { opened.add(it) }

                    val quranDb = builders.quran
                        .setDriver(BundledSQLiteDriver())
                        .setQueryCoroutineContext(Dispatchers.IO)
                        .fallbackToDestructiveMigration(true)
                        .build()
                        .also { opened.add(it) }

                    val externalDb = builders.externalQuran
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
                        .also { opened.add(it) }

                    val searchDb = builders.searchIndex
                        .setDriver(BundledSQLiteDriver())
                        .setQueryCoroutineContext(Dispatchers.IO)
                        .fallbackToDestructiveMigration(true)
                        .build()
                        .also { opened.add(it) }

                    val topicsDb = builders.topics
                        .setDriver(BundledSQLiteDriver())
                        .setQueryCoroutineContext(Dispatchers.IO)
                        .fallbackToDestructiveMigration(true)
                        .build()
                        .also { opened.add(it) }

                    val translationRepository =
                        TranslationRepository(externalDb)

                    Instances(
                        userDatabase = userDb,
                        quranDatabase = quranDb,
                        externalDatabase = externalDb,
                        searchDatabase = searchDb,
                        topicsDatabase = topicsDb,

                        userRepository = UserRepository(userDb),
                        translationRepository = translationRepository,
                        quranRepository = QuranRepository(
                            quranDb,
                            externalDb,
                        ),
                        topicsRepository = TopicsRepository(topicsDb) {
                            translationRepository
                        },
                    )
                } catch (error: Throwable) {
                    opened.asReversed().forEach {
                        runCatching { it.close() }
                    }
                    throw error
                }
            }

            // Publish everything together only after successful initialization.
            instances = newInstances
        }
    }

    private fun requireInstances(): Instances =
        checkNotNull(instances) {
            "DatabaseProvider.initialize() must be called before accessing repositories."
        }

    // Synchronous accessors

    fun getUserDatabase() = requireInstances().userDatabase

    fun getUserRepository() = requireInstances().userRepository

    fun getQuranDatabase() = requireInstances().quranDatabase

    fun getExternalQuranDatabase() = requireInstances().externalDatabase

    fun getSearchIndexDatabase() = requireInstances().searchDatabase

    fun getTopicsDatabase() = requireInstances().topicsDatabase

    fun getTranslationRepository() = requireInstances().translationRepository

    fun getQuranRepository() = requireInstances().quranRepository

    fun getTopicsRepository() = requireInstances().topicsRepository

    // Bundled database management

    private suspend fun installBundledDatabase(
        name: String,
        assetPath: String,
        expectedVersion: Int,
    ) {
        val directory = PlatformServices.files.databaseDirectory
        fs.createDirectories(directory)

        val destination = directory / name

        if (fs.exists(destination)) {
            val currentVersion = readSqliteUserVersion(destination)

            if (currentVersion == expectedVersion) return

            deleteDatabaseFiles(name)
        }

        val temp = directory / "$name.installing"

        try {
            val bytes = PlatformServices.assets.readAssetBytes(assetPath)

            fs.sink(temp).buffer().use {
                it.write(bytes)
            }

            fs.atomicMove(temp, destination)
        } finally {
            if (fs.exists(temp)) fs.delete(temp)
        }
    }

    private fun readSqliteUserVersion(path: Path): Int? =
        runCatching {
            fs.source(path).buffer().use {
                it.skip(60L)
                it.readInt()
            }
        }.getOrNull()

    private fun deleteDatabaseFiles(name: String) {
        val directory = PlatformServices.files.databaseDirectory

        listOf(
            name,
            "$name-wal",
            "$name-shm",
            "$name-journal",
        ).forEach { filename ->
            val path = directory / filename
            if (fs.exists(path)) fs.delete(path)
        }
    }
}
