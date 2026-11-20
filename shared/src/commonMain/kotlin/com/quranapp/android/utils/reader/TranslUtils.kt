package com.quranapp.android.utils.reader

import com.quranapp.android.api.models.translation.TranslationBookInfoModel
import com.quranapp.android.api.safeInt
import com.quranapp.android.api.safeLong
import com.quranapp.android.api.safeString
import com.quranapp.android.models.transls.TranslModel
import com.quranapp.android.platform.Platform
import com.quranapp.android.utils.app.AppFiles
import com.quranapp.android.utils.app.Logger
import com.quranapp.android.utils.miscellenous.toFormatted
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import okio.FileSystem
import okio.Path

object TranslUtils {
    const val DIR_NAME = "translations"
    const val DIR_NAME_4_AVAILABLE_DOWNLOADS = "available_translation_downloads"

    private val prebuiltJson = Json {
        ignoreUnknownKeys = true
    }

    const val TRANSL_INFO_FILE_NAME = "manifest.json"
    const val KEY_TRANSLATIONS = "key.translations"
    const val KEY_NEW_TRANSLATIONS = "key.translations_new"
    const val TRANSL_FILE_NAME_FORMAT = "translation_%d_%s_%s.json"
    const val TRANSL_AVAILABLE_DOWNLOADS_FILE_NAME = "available_downloads.json"

    const val TRANSL_SLUG_EN_SAHIH_INTERNATIONAL = "en_101_sahih-international"
    const val TRANSL_SLUG_EN_THE_CLEAR_QURAN = "en_102_the-clear-quran"
    const val TRANSL_SLUG_UR_JUNAGARHI = "ur_201_junagarhi"

    private const val TRANSL_TRANSLITERATION_SLUG_PART = "transliteration"

    const val TRANSL_SLUG_DEFAULT = TRANSL_SLUG_EN_SAHIH_INTERNATIONAL
    const val TRANSL_MAX_SELECTION_LIMIT = 6

    private data class PrebuiltSpec(
        val slug: String,
        val manifestPath: String,
        val defaultLangCode: String,
        val defaultLangName: String,
        val defaultDownloadPath: String,
    )

    private val prebuiltSpecs = listOf(
        PrebuiltSpec(
            slug = TRANSL_SLUG_EN_SAHIH_INTERNATIONAL,
            manifestPath = "prebuilt_translations/en_saheeh_v1_1_1/manifest.json",
            defaultLangCode = "en",
            defaultLangName = "English",
            defaultDownloadPath = "prebuilt_translations/en_saheeh_v1_1_1/en_saheeh_v1_1_1.json",
        ),
        PrebuiltSpec(
            slug = TRANSL_SLUG_EN_THE_CLEAR_QURAN,
            manifestPath = "prebuilt_translations/en_the_clear_quran/manifest.json",
            defaultLangCode = "en",
            defaultLangName = "English",
            defaultDownloadPath = "prebuilt_translations/en_the_clear_quran/en_the_clear_quran.json",
        ),
        PrebuiltSpec(
            slug = TRANSL_SLUG_UR_JUNAGARHI,
            manifestPath = "prebuilt_translations/ur_junagarhi/manifest.json",
            defaultLangCode = "ur",
            defaultLangName = "Urdu",
            defaultDownloadPath = "prebuilt_translations/ur_junagarhi/ur_junagarhi.json",
        ),
    )

    fun availableDownloadsFile(): Path {
        return AppFiles.savedFile(
            DIR_NAME_4_AVAILABLE_DOWNLOADS,
            TRANSL_AVAILABLE_DOWNLOADS_FILE_NAME
        )
    }

    fun translationFile(translId: Int, langCode: String, translSlug: String): Path {
        val filename = "translation_${translId}_${langCode}_$translSlug.json"
        return AppFiles.savedFile(DIR_NAME, langCode, translSlug, filename)
    }

    fun defaultTranslationSlugs(): HashSet<String> = hashSetOf(TRANSL_SLUG_DEFAULT)

    suspend fun preBuiltTranslBooksInfo(): List<TranslationBookInfoModel> {
        return prebuiltSpecs.map { createPrebuiltTranslBookInfo(it) }
    }

    private suspend fun createPrebuiltTranslBookInfo(
        spec: PrebuiltSpec,
    ): TranslationBookInfoModel {
        val model = TranslationBookInfoModel(spec.slug)
        val manifest = readPrebuiltManifest(spec.manifestPath)

        model.langCode = manifest.safeString("langCode")
            .orEmpty()
            .ifBlank { spec.defaultLangCode }

        model.langName = manifest.safeString("langName")
            .orEmpty()
            .ifBlank { spec.defaultLangName }

        model.bookName = manifest.safeString("book").orEmpty()
        model.authorName = manifest.safeString("author").orEmpty()

        model.displayName = manifest.safeString("displayName").orEmpty()
            .ifBlank { model.bookName }

        model.downloadPath = manifest.safeString("downloadPath").orEmpty()
            .ifBlank { spec.defaultDownloadPath }

        model.version = manifest.safeLong("version") ?: 1L
        model.lastUpdated = model.version

        return model
    }

    private suspend fun readPrebuiltManifest(path: String): JsonObject? {
        return runCatching {
            prebuiltJson.parseToJsonElement(
                Platform.assets.readAssetText(path)
            ).jsonObject
        }.onFailure {
            Logger.saveError(it, "TranslUtils.readPrebuiltManifest")
        }.getOrNull()
    }

    fun getPrebuiltTranslInfoPath(slug: String): String? {
        return prebuiltSpecs.firstOrNull { it.slug == slug }?.manifestPath
    }

    fun getPrebuiltTranslPath(slug: String): String? {
        return prebuiltSpecs.firstOrNull { it.slug == slug }?.defaultDownloadPath
    }

    fun isPrebuilt(slug: String): Boolean {
        return prebuiltSpecs.any { it.slug == slug }
    }

    fun isUrdu(slug: String): Boolean {
        val parts = slug.split('_', '-')
        return parts.firstOrNull() == "ur"
    }

    fun isTransliteration(slug: String): Boolean {
        return slug.contains(TRANSL_TRANSLITERATION_SLUG_PART)
    }

    private fun prepareTranslDirPathForSpecificLangNSlug(
        langCode: String,
        translSlug: String,
    ): String {
        return "$langCode/$translSlug"
    }

    @JvmStatic
    fun prepareTranslInfoPathForSpecificLangNSlug(langCode: String, translSlug: String): String {
        val path2TranslDir = prepareTranslDirPathForSpecificLangNSlug(langCode, translSlug)
        return "$path2TranslDir/$TRANSL_INFO_FILE_NAME"
    }

    @JvmStatic
    fun prepareTranslPathForSpecificLangNSlug(
        translId: Int,
        langCode: String,
        translSlug: String,
    ): String {
        val path2TranslDir = prepareTranslDirPathForSpecificLangNSlug(langCode, translSlug)
        val filename =
            TRANSL_FILE_NAME_FORMAT.toFormatted(translId, langCode, translSlug)
        return "$path2TranslDir/$filename"
    }

    @Throws(Exception::class)
    fun getTranslInfosAndFilesForMigration(
        translDir: Path,
    ): List<Pair<TranslationBookInfoModel, Path>>? {
        val fs = FileSystem.SYSTEM
        val dirsOfLangCodes = fs.list(translDir)
        if (dirsOfLangCodes.isNullOrEmpty()) {
            Logger.d("Nothing was found, deleting root translation directory: ${translDir.name}")
            AppFiles.deleteRecursively(translDir)
            return null
        }

        val translInfosAndFiles = ArrayList<Pair<TranslationBookInfoModel, Path>>()

        for (langCodeDir in dirsOfLangCodes) {
            val translFiles = fs.list(langCodeDir)
            if (translFiles.isNullOrEmpty()) {
                Logger.d("Deleting language directory with its contents: ${langCodeDir.name}")
                AppFiles.deleteRecursively(langCodeDir)
                continue
            }



            for (singleTranslDir in translFiles) {
                val metadata = fs.metadataOrNull(singleTranslDir) ?: continue
                if (!metadata.isDirectory) {
                    AppFiles.deleteRecursively(singleTranslDir)
                    continue
                }

                try {
                    val infoJSONFile = singleTranslDir / TRANSL_INFO_FILE_NAME
                    val pair = readTranslInfoFromJSONFile(infoJSONFile)
                    if (pair == null) {
                        Logger.print(
                            "Deleting translation directory with its manifest and data files: ${singleTranslDir.name}",
                        )
                        AppFiles.deleteRecursively(singleTranslDir)
                        continue
                    }

                    translInfosAndFiles.add(pair)
                } catch (e: Exception) {
                    Logger.print(
                        "Error occurred, deleting translation directory with its manifest and data files dire: ${singleTranslDir.name}",
                    )
                    AppFiles.deleteRecursively(singleTranslDir)
                    e.printStackTrace()
                }
            }
        }

        return translInfosAndFiles
    }

    private fun readTranslInfoFromJSONFile(
        infoJSONFile: Path,
    ): Pair<TranslationBookInfoModel, Path>? {
        val metadata = AppFiles.metadata(infoJSONFile) ?: return null

        if (!metadata.isRegularFile) return null

        val json = AppFiles.readText(infoJSONFile)
        val jsonObject = prebuiltJson.parseToJsonElement(json).jsonObject

        val slug = jsonObject.safeString("slug").orEmpty()
        if (isPrebuilt(slug)) return null

        val id = jsonObject.safeInt("id") ?: -1
        val langCode = jsonObject.safeString("langCode").orEmpty()
            .ifBlank { jsonObject.safeString("lang-code").orEmpty() }

        val translFile = translationFile(id, langCode, slug)

        if (AppFiles.size(translFile) == 0L) {
            Logger.d("Translation file does not exist or is empty.")
            return null
        }

        val bookInfo = TranslationBookInfoModel(slug)
        bookInfo.langCode = langCode
        bookInfo.bookName = jsonObject.safeString("book").orEmpty()
        bookInfo.authorName = jsonObject.safeString("author").orEmpty()
        bookInfo.langName = jsonObject.safeString("langName").orEmpty()
            .ifBlank { jsonObject.safeString("lang-name").orEmpty() }

        bookInfo.displayName = jsonObject.safeString("displayName").orEmpty()
            .ifBlank { jsonObject.safeString("display-name").orEmpty() }

        bookInfo.version = jsonObject.safeLong("version") ?: 1L
        bookInfo.lastUpdated = bookInfo.version
        bookInfo.downloadPath = jsonObject.safeString("downloadPath").orEmpty()

        return bookInfo to translFile
    }

    fun resolveSelectionChange(
        slugSet: MutableSet<String>,
        model: TranslModel,
        isSelected: Boolean,
    ): Boolean {
        val slug = model.bookInfo.slug
        if (isSelected) {
            if (slugSet.size >= TRANSL_MAX_SELECTION_LIMIT) {
                return false
            }
            slugSet.add(slug)
        } else {
            slugSet.remove(slug)
        }
        return true
    }
}
