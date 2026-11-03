package com.quranapp.android.utils.reader.translation

import android.content.Context
import com.quranapp.android.api.JsonHelper
import com.quranapp.android.api.RetrofitInstance
import com.quranapp.android.api.models.translation.TranslationBookInfoModel
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.reader.TranslUtils
import com.quranapp.android.utils.reader.factory.QuranTranslationFactory
import com.quranapp.android.utils.univ.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

data class TranslationUpdateUiState(
    val outdatedSlugs: Set<String> = emptySet(),
) {
    val hasOutdatedDownloads: Boolean get() = outdatedSlugs.isNotEmpty()
}

class TranslationVersionManager private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val fileUtils = FileUtils.newInstance(appContext)
    private val store = TranslationVersionStore(appContext)
    private val mutex = Mutex()
    private val legacyVersionThreshold = 1_000_000L

    private val _updateUiState = MutableStateFlow(TranslationUpdateUiState())
    val updateUiState: StateFlow<TranslationUpdateUiState> = _updateUiState.asStateFlow()

    data class RemoteTranslationEntry(
        val slug: String,
        val langCode: String,
        val langName: String,
        val book: String,
        val author: String,
        val displayName: String,
        val downloadPath: String,
        val version: Long,
    ) {
        fun toBookInfoModel(): TranslationBookInfoModel {
            return TranslationBookInfoModel(slug).apply {
                this.langCode = this@RemoteTranslationEntry.langCode
                this.langName = this@RemoteTranslationEntry.langName
                this.bookName = this@RemoteTranslationEntry.book
                this.authorName = this@RemoteTranslationEntry.author
                this.displayName = this@RemoteTranslationEntry.displayName
                this.downloadPath = this@RemoteTranslationEntry.downloadPath
                this.version = this@RemoteTranslationEntry.version
                this.lastUpdated = this@RemoteTranslationEntry.version
            }
        }
    }

    suspend fun reconcileAfterManifestRefresh(forceFetchManifest: Boolean = false) = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                val remoteEntries = getRemoteEntries(forceFetchManifest)
                if (remoteEntries.isEmpty()) {
                    _updateUiState.value = TranslationUpdateUiState(emptySet())
                    return@runCatching
                }

                val remoteMap = remoteEntries.associateBy { it.slug }
                val outdated = mutableSetOf<String>()
                val stored = store.loadAll()
                var mutated = false

                QuranTranslationFactory(appContext).use { factory ->
                    val downloaded = factory.getDownloadedTranslationBooksInfo()

                    for ((slug, bookInfo) in downloaded) {
                        if (TranslUtils.isPrebuilt(slug) || bookInfo.downloadPath.isBlank()) {
                            continue
                        }

                        val remote = remoteMap[slug] ?: continue
                        val storedVersionRaw = stored[slug]?.version
                        val localVersion = normalizeLocalVersion(
                            storedVersionRaw ?: bookInfo.version,
                        )

                        // Backfill legacy/local-normalized values so future checks are stable.
                        if (storedVersionRaw == null || storedVersionRaw != localVersion) {
                            stored[slug] = TranslationVersionEntry(localVersion)
                            mutated = true
                        }

                        if (remote.version > localVersion) {
                            outdated.add(slug)
                        } else {
                            if (stored[slug]?.version != remote.version) {
                                stored[slug] = TranslationVersionEntry(remote.version)
                                mutated = true
                            }
                        }
                    }
                }

                if (mutated) {
                    store.saveAll(stored)
                }
                _updateUiState.value = TranslationUpdateUiState(outdated)
            }.onFailure {
                Log.saveError(it, "TranslationVersionManager.reconcileAfterManifestRefresh")
            }
        }
    }

    suspend fun refreshOutdatedState() = reconcileAfterManifestRefresh(forceFetchManifest = false)

    suspend fun markTranslationUpdated(slug: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                val remote = getRemoteEntries(false).associateBy { it.slug }[slug] ?: return@runCatching
                val stored = store.loadAll()
                stored[slug] = TranslationVersionEntry(remote.version)
                store.saveAll(stored)
                _updateUiState.value = _updateUiState.value.copy(
                    outdatedSlugs = _updateUiState.value.outdatedSlugs - slug,
                )
            }.onFailure {
                Log.saveError(it, "TranslationVersionManager.markTranslationUpdated")
            }
        }
    }

    suspend fun getRemoteBookInfo(slug: String): TranslationBookInfoModel? = withContext(Dispatchers.IO) {
        return@withContext runCatching {
            getRemoteEntries(false).firstOrNull { it.slug == slug }?.toBookInfoModel()
        }.getOrNull()
    }

    fun needsUpdate(slug: String): Boolean {
        return _updateUiState.value.outdatedSlugs.contains(slug)
    }

    private fun normalizeLocalVersion(raw: Long): Long {
        if (raw <= 0L) return 1L
        // Old builds stored epoch timestamps in the same DB column.
        if (raw > legacyVersionThreshold) return 1L
        return raw
    }

    private suspend fun getRemoteEntries(forceFetchManifest: Boolean): List<RemoteTranslationEntry> {
        var shouldFetchManifest = forceFetchManifest
        if (!shouldFetchManifest) {
            val localManifest = fileUtils.translsManifestFile
            shouldFetchManifest = !localManifest.exists() || localManifest.length() == 0L
        }

        if (shouldFetchManifest) {
            runCatching {
                val responseBody = RetrofitInstance.github.getAvailableTranslations()
                val data = responseBody.string()
                val manifestFile = fileUtils.translsManifestFile
                fileUtils.createFile(manifestFile)
                manifestFile.writeText(data, Charsets.UTF_8)
            }.onFailure {
                Log.saveError(it, "TranslationVersionManager.getRemoteEntries.fetchManifest")
            }
        }

        val manifestFile = fileUtils.translsManifestFile
        if (!manifestFile.exists() || manifestFile.length() == 0L) return emptyList()

        return runCatching {
            val root = JsonHelper.json.parseToJsonElement(
                manifestFile.readText(Charsets.UTF_8),
            ).jsonObject

            val translations = root["translations"]?.jsonObject ?: return@runCatching emptyList()
            val output = mutableListOf<RemoteTranslationEntry>()

            for ((langCode, langNode) in translations) {
                val langObj = langNode.jsonObject
                for ((slug, translNode) in langObj) {
                    val obj = translNode.jsonObject
                    val version = obj["version"]?.jsonPrimitive?.longOrNull ?: 1L
                    output.add(
                        RemoteTranslationEntry(
                            slug = slug,
                            langCode = obj["langCode"]?.jsonPrimitive?.contentOrNull ?: langCode,
                            langName = obj["langName"]?.jsonPrimitive?.contentOrNull ?: "",
                            book = obj["book"]?.jsonPrimitive?.contentOrNull ?: "",
                            author = obj["author"]?.jsonPrimitive?.contentOrNull ?: "",
                            displayName = obj["displayName"]?.jsonPrimitive?.contentOrNull ?: "",
                            downloadPath = obj["downloadPath"]?.jsonPrimitive?.contentOrNull ?: "",
                            version = version,
                        ),
                    )
                }
            }

            output
        }.onFailure {
            Log.saveError(it, "TranslationVersionManager.getRemoteEntries.parseManifest")
        }.getOrElse { emptyList() }
    }

    companion object {
        @Volatile
        private var instance: TranslationVersionManager? = null

        fun get(context: Context): TranslationVersionManager {
            return instance ?: synchronized(this) {
                instance ?: TranslationVersionManager(context).also { instance = it }
            }
        }
    }
}
