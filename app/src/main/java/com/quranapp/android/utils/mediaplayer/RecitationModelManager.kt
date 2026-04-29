package com.quranapp.android.utils.mediaplayer

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.quranapp.android.api.JsonHelper
import com.quranapp.android.api.RetrofitInstance
import com.quranapp.android.api.models.recitation2.AvailableRecitationTranslationsModel
import com.quranapp.android.api.models.recitation2.AvailableRecitationsModel
import com.quranapp.android.api.models.recitation2.RecitationModelBase
import com.quranapp.android.api.models.recitation2.RecitationQuranModel
import com.quranapp.android.api.models.recitation2.RecitationTranslationModel
import com.quranapp.android.compose.components.player.dialogs.AudioOption
import com.quranapp.android.compose.utils.appFallbackLanguageCodes
import com.quranapp.android.compose.utils.preferences.RecitationPreferences
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.app.AppUtils
import com.quranapp.android.utils.univ.FileUtils
import com.quranapp.android.utils.univ.StringUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import java.io.File
import java.util.Locale

class RecitationModelManager private constructor(
    context: Context
) {
    private val appContext = context.applicationContext

    private val DIR_NAME_LEGACY: String = FileUtils.createPath(
        AppUtils.BASE_APP_DOWNLOADED_SAVED_DATA_DIR, "recitations"
    )
    private val DIR_NAME: String = FileUtils.createPath(
        AppUtils.BASE_APP_DOWNLOADED_SAVED_DATA_DIR, "recitations_v2"
    )

    private val QURAN_MANIFEST_FILENAME = "available_recitations.json"
    private val TRANSLATION_MANIFEST_FILENAME = "available_recitation_translations.json"
    private val RECITATION_AUDIO_FILENAME_FORMAT_LOCAL: String = "%03d.mp3"

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
            // There is nothing to migrate as the new implementation is completely different
            // and does not rely on the old data structure. We can simply delete the old data
            // to free up space.
            val legacyDir = File(DIR_NAME_LEGACY)
            if (legacyDir.exists()) {
                legacyDir.deleteRecursively()
            }
        }
    }

    suspend fun resolveModels(settings: PlayerSettings): Pair<RecitationQuranModel?, RecitationTranslationModel?> {
        val audioOption = settings.audioOption
        val reciterId = settings.reciter
        val translationReciterId = settings.translationReciter
        val resolveQuran = audioOption != AudioOption.ONLY_TRANSLATION
        val resolveTranslation = audioOption != AudioOption.ONLY_QURAN

        return Pair(
            if (resolveQuran) (if (reciterId.isNullOrBlank()) getSelectedQuranModel() else getQuranModel(
                reciterId
            )) else null,
            if (resolveTranslation) (if (translationReciterId.isNullOrBlank()) getSelectedTranslationModel() else getTranslationModel(
                translationReciterId
            )) else null
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

    suspend fun getQuranModel(
        id: String?
    ): RecitationQuranModel? {
        return getAllQuranModel()?.reciters?.selectById(id)
    }

    suspend fun getTranslationModel(
        id: String?
    ): RecitationTranslationModel? {
        return getAllTranslationModel()?.reciters?.selectById(id)
    }

    suspend fun getAllQuranModel(
        forceRefresh: Boolean = forceRefreshQuran
    ): AvailableRecitationsModel? {
        val inMemory = cachedQuran

        if (!forceRefresh && inMemory != null) {
            return inMemory
        }

        return quranLoadLock.withLock {
            val recheck = cachedQuran

            if (!forceRefresh && recheck != null) {
                return@withLock recheck
            }

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
        forceRefresh: Boolean = forceRefreshTranslation
    ): AvailableRecitationTranslationsModel? {
        val inMemory = cachedTranslation

        if (!forceRefresh && inMemory != null) {
            return inMemory
        }

        return translationLoadLock.withLock {
            val recheck = cachedTranslation

            if (!forceRefresh && recheck != null) {
                return@withLock recheck
            }

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
        val audioAudio = RecitationPreferences.getAudioOption()

        val isBoth = audioAudio == AudioOption.BOTH
        val isOnlyTransl = audioAudio == AudioOption.ONLY_TRANSLATION

        val quranReciterName =
            if (!isOnlyTransl) getSelectedQuranModel()?.getReciterName() else null

        val translationReciterName =
            if (isBoth || isOnlyTransl) getSelectedTranslationModel()?.getReciterName() else null

        val reciterName = if (
            isBoth &&
            !quranReciterName.isNullOrEmpty() &&
            !translationReciterName.isNullOrEmpty()
        ) {
            "$quranReciterName & $translationReciterName"
        } else {
            quranReciterName ?: translationReciterName ?: ""
        }

        return reciterName
    }

    @Composable
    fun rememberCurrentReciterNameForAudioOption(): String {
        val audioOption = RecitationPreferences.observeAudioOption()

        val reciterId = RecitationPreferences.observeReciterId()
        val translationReciterId = RecitationPreferences.observeTranslationReciterId()

        val reciterName by produceState(
            initialValue = "",
            audioOption,
            reciterId,
            translationReciterId
        ) {
            value = getCurrentReciterNameForAudioOption()
        }

        return reciterName
    }


    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun loadQuranFromLocal(): AvailableRecitationsModel? =
        withContext(Dispatchers.IO) {
            val file = getQuranManifestFile()

            if (!file.exists() || file.length() == 0L) {
                return@withContext null
            }

            try {
                JsonHelper.json.decodeFromString<AvailableRecitationsModel>(
                    file.readText(Charsets.UTF_8)
                )
            } catch (e: Exception) {
                Log.saveError(e, "RecitationManager.loadQuranFromLocal")
                null
            }
        }

    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun loadTranslationFromLocal(): AvailableRecitationTranslationsModel? =
        withContext(Dispatchers.IO) {
            val file = getTranslationManifestFile()
            if (!file.exists() || file.length() == 0L) {
                return@withContext null
            }

            try {
                JsonHelper.json.decodeFromString<AvailableRecitationTranslationsModel>(
                    file.readText(Charsets.UTF_8)
                )
            } catch (e: Exception) {
                Log.saveError(e, "RecitationManager.loadTranslationFromLocal")
                null
            }
        }

    private suspend fun loadQuranFromNetwork(): AvailableRecitationsModel? =
        withContext(Dispatchers.IO) {
            try {
                downloadManifest(
                    getQuranManifestFile(),
                    RetrofitInstance.github.getAvailableRecitations(),
                )
                loadQuranFromLocal()
            } catch (e: Exception) {
                Log.saveError(e, "RecitationManager.loadQuranFromNetwork")
                null
            }
        }

    private suspend fun loadTranslationFromNetwork(): AvailableRecitationTranslationsModel? =
        withContext(Dispatchers.IO) {
            try {
                downloadManifest(
                    getTranslationManifestFile(),
                    RetrofitInstance.github.getAvailableRecitationTranslations(),
                )
                loadTranslationFromLocal()
            } catch (e: Exception) {
                Log.saveError(e, "RecitationManager.loadTranslationFromNetwork")
                null
            }
        }

    private fun getRecitationsDir() = FileUtils.makeAndGetAppResourceDir(DIR_NAME)

    /**
     * Counts non-empty `.mp3` files under per-reciter dirs and how many reciter dirs have at least one
     * (excludes `timing_metadata` and manifest JSON files).
     */
    fun getDownloadedAudioStats(): Pair<Int, Int> {
        val root = getRecitationsDir()
        var mp3Count = 0
        var recitersWithAudio = 0
        root.listFiles()?.filter { it.isDirectory && it.name != "timing_metadata" }
            ?.forEach { dir ->
                var hasMp3 = false
                dir.walkTopDown().forEach { f ->
                    if (f.isFile && f.length() > 0L && f.name.endsWith(".mp3", ignoreCase = true)) {
                        mp3Count++
                        hasMp3 = true
                    }
                }
                if (hasMp3) recitersWithAudio++
            }
        return mp3Count to recitersWithAudio
    }

    /** Removes all downloaded chapter audio (and any other files) for this reciter id. */
    fun deleteReciterAudioDirectory(reciterId: String) {
        val dir = File(getRecitationsDir(), reciterId)
        if (dir.exists()) {
            dir.deleteRecursively()
        }
        val timing = getRecitationTimingFile(reciterId)
        if (timing.exists()) {
            timing.delete()
        }
    }

    fun getRecitationAudioFile(reciterId: String, chapterNo: Int): File {
        val filename = StringUtils.formatInvariant(
            RECITATION_AUDIO_FILENAME_FORMAT_LOCAL,
            chapterNo,
        )

        return File(
            getRecitationsDir(),
            FileUtils.createPath(reciterId, filename),
        )
    }

    fun getRecitationTimingFile(reciterId: String): File {
        return File(
            getRecitationsDir(),
            FileUtils.createPath("timing_metadata", "$reciterId.json"),
        )
    }

    private fun getQuranManifestFile() = File(getRecitationsDir(), QURAN_MANIFEST_FILENAME)

    private fun getTranslationManifestFile() =
        File(getRecitationsDir(), TRANSLATION_MANIFEST_FILENAME)

    private fun downloadManifest(file: File, body: okhttp3.ResponseBody) {
        val fileUtils = FileUtils.newInstance(appContext)

        val tempFile = File(file.parentFile, "${file.name}.tmp")
        if (fileUtils.createFile(tempFile)) {
            try {
                body.byteStream().use { input ->
                    tempFile.outputStream().buffered().use { output ->
                        input.copyTo(output)
                        output.flush()
                    }
                }
                if (file.exists()) {
                    file.delete()
                }
                tempFile.renameTo(file)
            } finally {
                if (tempFile.exists() && tempFile.name != file.name) {
                    tempFile.delete()
                }
            }
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
            .map { it.lowercase(Locale.ROOT) }
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

    companion object {
        @Volatile
        private var instance: RecitationModelManager? = null

        fun get(context: Context): RecitationModelManager {
            return instance ?: synchronized(this) {
                instance ?: RecitationModelManager(context).also { instance = it }
            }
        }


        fun emptyModel(
            id: String = "",
            reciter: String = "",
            style: String? = null,
            urlTemplate: String = "",
        ): RecitationQuranModel {
            return RecitationQuranModel(
                style = style,
            ).apply {
                this.id = id
                this.reciter = reciter
                this.urlTemplate = urlTemplate
            }
        }
    }
}
