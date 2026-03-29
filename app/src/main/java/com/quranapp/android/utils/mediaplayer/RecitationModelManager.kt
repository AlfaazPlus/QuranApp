package com.quranapp.android.utils.mediaplayer

import android.content.Context
import com.quranapp.android.api.JsonHelper
import com.quranapp.android.api.RetrofitInstance
import com.quranapp.android.api.models.recitation2.AvailableRecitationTranslationsModel
import com.quranapp.android.api.models.recitation2.AvailableRecitationsModel
import com.quranapp.android.api.models.recitation2.RecitationModelBase
import com.quranapp.android.api.models.recitation2.RecitationQuranModel
import com.quranapp.android.api.models.recitation2.RecitationTranslationModel
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.app.AppUtils
import com.quranapp.android.utils.reader.recitation.RecitationUtils
import com.quranapp.android.utils.sharedPrefs.SPReader
import com.quranapp.android.utils.sharedPrefs.SPReader.getRecitationAudioOption
import com.quranapp.android.utils.univ.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
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

    private val quranLoadLock = Mutex()
    private val translationLoadLock = Mutex()

    fun migrateLegacyData() {
        // There is nothing to migrate as the new implementation is completely different
        // and does not rely on the old data structure. We can simply delete the old data
        // to free up space.
        val legacyDir = File(DIR_NAME_LEGACY)
        if (legacyDir.exists()) {
            legacyDir.deleteRecursively()
        }
    }

    suspend fun resolveModels(): Pair<RecitationQuranModel?, RecitationTranslationModel?> {
        val audioOption = getRecitationAudioOption(appContext)

        val resolveQuran = audioOption != RecitationUtils.AUDIO_OPTION_ONLY_TRANSLATION
        val resolveTranslation = audioOption != RecitationUtils.AUDIO_OPTION_ONLY_ARABIC

        return Pair(
            if (resolveQuran) getSelectedQuranModel() else null,
            if (resolveTranslation) getSelectedTranslationModel() else null
        )
    }

    suspend fun getSelectedQuranModel(
    ): RecitationQuranModel? {
        val id = SPReader.getSavedRecitationSlug(appContext)

        return getAllQuranModel()?.reciters?.selectById(id)
    }

    suspend fun getSelectedTranslationModel(
    ): RecitationTranslationModel? {
        val id = SPReader.getSavedRecitationTranslationSlug(appContext)

        return getAllTranslationModel()?.reciters?.selectById(id)
    }

    suspend fun getAllQuranModel(
        forceRefresh: Boolean = false
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

            networkModel
        }
    }

    suspend fun getAllTranslationModel(
        forceRefresh: Boolean = false
    ): AvailableRecitationTranslationsModel? {
        val id = SPReader.getSavedRecitationTranslationSlug(appContext)
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

            networkModel
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun loadQuranFromLocal(): AvailableRecitationsModel? =
        withContext(Dispatchers.IO) {
            val file = getQuranManifestFile()

            if (!file.exists() || file.length() == 0L) {
                return@withContext null
            }

            try {
                file.inputStream().buffered().use {
                    JsonHelper.json.decodeFromStream<AvailableRecitationsModel>(it)
                }
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
                file.inputStream().buffered().use {
                    JsonHelper.json.decodeFromStream<AvailableRecitationTranslationsModel>(it)
                }
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

    fun getRecitationAudioFile(reciterId: String, chapterNo: Int): File {
        val filename = String.format(
            Locale.ENGLISH,
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

        if (fileUtils.createFile(file)) {
            body.byteStream().use { input ->
                file.outputStream().buffered().use { output ->
                    input.copyTo(output)
                    output.flush()
                }
            }
        }
    }

    private fun <T : RecitationModelBase> List<T>.selectById(id: String?): T? {
        if (isEmpty()) return null
        if (id.isNullOrBlank()) return firstOrNull()
        return firstOrNull { it.id == id } ?: firstOrNull()
    }

    companion object {
        @Volatile
        private var instance: RecitationModelManager? = null

        fun getInstance(context: Context): RecitationModelManager {
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