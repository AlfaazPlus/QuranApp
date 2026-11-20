package com.quranapp.android.utils.mediaplayer

import com.quranapp.android.api.ApiClient
import com.quranapp.android.api.JsonHelper
import com.quranapp.android.api.models.recitation2.AvailableRecitationTranslationsModel
import com.quranapp.android.api.models.recitation2.AvailableRecitationsModel
import com.quranapp.android.api.models.recitation2.RecitationModelBase
import com.quranapp.android.api.models.recitation2.RecitationQuranModel
import com.quranapp.android.api.models.recitation2.RecitationTranslationModel
import com.quranapp.android.locale.appFallbackLanguageCodes
import com.quranapp.android.preferences.RecitationPreferences
import com.quranapp.android.utils.app.AppFiles
import com.quranapp.android.utils.app.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import okio.FileSystem
import okio.Path

object RecitationModelManager {
    private const val LEGACY_DIR_NAME = "recitations"
    private const val DIR_NAME = "recitations_v2"
    private const val QURAN_MANIFEST_FILENAME = "available_recitations.json"
    private const val TRANSLATION_MANIFEST_FILENAME = "available_recitation_translations.json"

    @Volatile
    private var cachedQuran: AvailableRecitationsModel? = null

    @Volatile
    private var cachedTranslation: AvailableRecitationTranslationsModel? = null

    var forceRefreshQuran = false
    var forceRefreshTranslation = false

    private val quranLoadLock = Mutex()
    private val translationLoadLock = Mutex()

    fun migrateLegacyData() {
        CoroutineScope(Dispatchers.IO).launch {
            AppFiles.deleteRecursively(AppFiles.savedDir(LEGACY_DIR_NAME))
        }
    }

    suspend fun resolveModels(
        settings: PlayerSettings,
    ): Pair<RecitationQuranModel?, RecitationTranslationModel?> {
        val audioOption = settings.audioOption
        val resolveQuran = audioOption != AudioOption.ONLY_TRANSLATION
        val resolveTranslation = audioOption != AudioOption.ONLY_QURAN

        return Pair(
            if (resolveQuran) {
                if (settings.reciter.isNullOrBlank()) getSelectedQuranModel()
                else getQuranModel(settings.reciter)
            } else {
                null
            },
            if (resolveTranslation) {
                if (settings.translationReciter.isNullOrBlank()) getSelectedTranslationModel()
                else getTranslationModel(settings.translationReciter)
            } else {
                null
            },
        )
    }

    suspend fun getSelectedQuranModel(): RecitationQuranModel? {
        val id = RecitationPreferences.getReciterId()
        if (id.isNullOrBlank()) {
            val reciters = getAllQuranModel()?.reciters
            if (reciters.isNullOrEmpty()) return null

            val chosen = reciters.firstOrNull { it.isDefault } ?: reciters.first()
            RecitationPreferences.setReciterId(chosen.id)
            return chosen
        }
        return getQuranModel(id)
    }

    suspend fun getSelectedTranslationModel(): RecitationTranslationModel? {
        val id = RecitationPreferences.getTranslationReciterId()
        if (id.isNullOrBlank()) {
            val reciters = getAllTranslationModel()?.reciters ?: return null
            val chosen = reciters.selectTranslationByLocaleWithFallback() ?: return null
            RecitationPreferences.setTranslationReciterId(chosen.id)
            return chosen
        }
        return getTranslationModel(id)
    }

    suspend fun getQuranModel(id: String?): RecitationQuranModel? {
        return getAllQuranModel()?.reciters?.selectById(id)
    }

    suspend fun getTranslationModel(id: String?): RecitationTranslationModel? {
        return getAllTranslationModel()?.reciters?.selectById(id)
    }

    suspend fun getAllQuranModel(
        forceRefresh: Boolean = forceRefreshQuran,
    ): AvailableRecitationsModel? {
        val inMemory = cachedQuran
        if (!forceRefresh && inMemory != null) return inMemory

        return quranLoadLock.withLock {
            val recheck = cachedQuran
            if (!forceRefresh && recheck != null) return@withLock recheck

            if (!forceRefresh) {
                loadQuranFromLocal()?.let { localModel ->
                    cachedQuran = localModel
                    return@withLock localModel
                }
            }

            val networkModel = loadQuranFromNetwork() ?: return@withLock null
            cachedQuran = networkModel
            forceRefreshQuran = false
            networkModel
        }
    }

    suspend fun getAllTranslationModel(
        forceRefresh: Boolean = forceRefreshTranslation,
    ): AvailableRecitationTranslationsModel? {
        val inMemory = cachedTranslation
        if (!forceRefresh && inMemory != null) return inMemory

        return translationLoadLock.withLock {
            val recheck = cachedTranslation
            if (!forceRefresh && recheck != null) return@withLock recheck

            if (!forceRefresh) {
                loadTranslationFromLocal()?.let { localModel ->
                    cachedTranslation = localModel
                    return@withLock localModel
                }
            }

            val networkModel = loadTranslationFromNetwork() ?: return@withLock null
            cachedTranslation = networkModel
            forceRefreshTranslation = false
            networkModel
        }
    }

    suspend fun refreshManifests() {
        loadQuranFromNetwork()
        loadTranslationFromNetwork()
    }

    suspend fun getCurrentReciterNameForAudioOption(): String {
        val audioOption = RecitationPreferences.getAudioOption()
        val isBoth = audioOption == AudioOption.BOTH
        val isOnlyTranslation = audioOption == AudioOption.ONLY_TRANSLATION

        val quranReciterName =
            if (!isOnlyTranslation) getSelectedQuranModel()?.getReciterName() else null
        val translationReciterName =
            if (isBoth || isOnlyTranslation) getSelectedTranslationModel()?.getReciterName() else null

        return if (
            isBoth &&
            !quranReciterName.isNullOrEmpty() &&
            !translationReciterName.isNullOrEmpty()
        ) {
            "$quranReciterName & $translationReciterName"
        } else {
            quranReciterName ?: translationReciterName ?: ""
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun loadQuranFromLocal(): AvailableRecitationsModel? =
        withContext(Dispatchers.IO) {
            val file = quranManifestFile()
            if (!AppFiles.exists(file) || AppFiles.size(file) == 0L) return@withContext null

            try {
                val model = JsonHelper.json.decodeFromString<AvailableRecitationsModel>(
                    AppFiles.readText(file)
                )
                AvailableRecitationsModel(reciters = model.reciters.sortedBy { it.reciter })
            } catch (e: Exception) {
                Logger.saveError(e, "RecitationManager.loadQuranFromLocal")
                null
            }
        }

    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun loadTranslationFromLocal(): AvailableRecitationTranslationsModel? =
        withContext(Dispatchers.IO) {
            val file = translationManifestFile()
            if (!AppFiles.exists(file) || AppFiles.size(file) == 0L) return@withContext null

            try {
                val model = JsonHelper.json.decodeFromString<AvailableRecitationTranslationsModel>(
                    AppFiles.readText(file),
                )
                AvailableRecitationTranslationsModel(reciters = model.reciters.sortedBy { it.reciter })
            } catch (e: Exception) {
                Logger.saveError(e, "RecitationManager.loadTranslationFromLocal")
                null
            }
        }

    private suspend fun loadQuranFromNetwork(): AvailableRecitationsModel? =
        withContext(Dispatchers.IO) {
            try {
                downloadManifest(quranManifestFile(), ApiClient.getAvailableRecitations())
                loadQuranFromLocal()
            } catch (e: Exception) {
                Logger.saveError(e, "RecitationManager.loadQuranFromNetwork")
                null
            }
        }

    private suspend fun loadTranslationFromNetwork(): AvailableRecitationTranslationsModel? =
        withContext(Dispatchers.IO) {
            try {
                downloadManifest(
                    translationManifestFile(),
                    ApiClient.getAvailableRecitationTranslations(),
                )
                loadTranslationFromLocal()
            } catch (e: Exception) {
                Logger.saveError(e, "RecitationManager.loadTranslationFromNetwork")
                null
            }
        }

    fun getDownloadedAudioStats(): Pair<Int, Int> {
        val root = getRecitationsDir()
        if (!AppFiles.exists(root)) return 0 to 0

        var mp3Count = 0
        var recitersWithAudio = 0
        FileSystem.SYSTEM.list(root)
            .filter { it.name != "timing_metadata" && FileSystem.SYSTEM.metadataOrNull(it)?.isDirectory == true }
            .forEach { dir ->
                var hasMp3 = false
                FileSystem.SYSTEM.listRecursively(dir).forEach { path ->
                    val metadata = FileSystem.SYSTEM.metadataOrNull(path) ?: return@forEach
                    val isMp3 = metadata.isRegularFile &&
                            (metadata.size ?: 0L) > 0L &&
                            path.name.endsWith(".mp3", ignoreCase = true)
                    if (isMp3) {
                        mp3Count++
                        hasMp3 = true
                    }
                }
                if (hasMp3) recitersWithAudio++
            }
        return mp3Count to recitersWithAudio
    }

    fun deleteReciterAudioDirectory(reciterId: String) {
        AppFiles.deleteRecursively(AppFiles.savedDir(DIR_NAME, reciterId))
        val timing = getRecitationTimingFile(reciterId)
        if (AppFiles.exists(timing)) FileSystem.SYSTEM.delete(timing)
    }

    fun getRecitationAudioFile(reciterId: String, chapterNo: Int): Path {
        val filename = chapterNo.toString().padStart(3, '0') + ".mp3"
        return AppFiles.savedFile(DIR_NAME, reciterId, filename)
    }

    fun getRecitationTimingFile(reciterId: String): Path {
        return AppFiles.savedFile(DIR_NAME, "timing_metadata", "$reciterId.json")
    }

    fun getRecitationsDir(): Path = AppFiles.savedDir(DIR_NAME)

    private fun quranManifestFile(): Path = AppFiles.savedFile(DIR_NAME, QURAN_MANIFEST_FILENAME)

    private fun translationManifestFile(): Path =
        AppFiles.savedFile(DIR_NAME, TRANSLATION_MANIFEST_FILENAME)

    private fun downloadManifest(file: Path, text: String) {
        val temp = file.parent!! / "${file.name}.tmp"
        if (!AppFiles.createFile(temp)) return

        try {
            AppFiles.writeText(temp, text)
            if (AppFiles.exists(file)) FileSystem.SYSTEM.delete(file)
            FileSystem.SYSTEM.atomicMove(temp, file)
        } finally {
            if (AppFiles.exists(temp)) FileSystem.SYSTEM.delete(temp)
        }
    }

    private fun <T : RecitationModelBase> List<T>.selectById(id: String?): T? {
        if (isEmpty()) return null
        if (id.isNullOrBlank()) return firstOrNull()
        return firstOrNull { it.id == id } ?: firstOrNull()
    }

    private fun List<RecitationTranslationModel>.selectTranslationByLocaleWithFallback(): RecitationTranslationModel? {
        if (isEmpty()) return null

        val candidates = appFallbackLanguageCodes()
            .map { it.lowercase() }
            .flatMap { sequenceOf(it, it.substringBefore('-')) }
            .distinct()

        return candidates
            .mapNotNull { candidate ->
                firstOrNull { it.langCode.equals(candidate, ignoreCase = true) }
            }
            .firstOrNull()
            ?: firstOrNull { it.langCode.equals("en", ignoreCase = true) }
            ?: firstOrNull()
    }

    fun emptyModel(
        id: String = "",
        reciter: String = "",
        style: String? = null,
        urlTemplate: String = "",
    ): RecitationQuranModel {
        return RecitationQuranModel(style = style).apply {
            this.id = id
            this.reciter = reciter
            this.urlTemplate = urlTemplate
        }
    }
}
