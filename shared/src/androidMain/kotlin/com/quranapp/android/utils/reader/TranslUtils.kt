package com.quranapp.android.utils.reader

import android.util.Pair
import com.quranapp.android.api.models.translation.TranslationBookInfoModel
import com.quranapp.android.components.transls.TranslModel
import com.quranapp.android.platform.PlatformServices
import com.quranapp.android.utils.app.Logger
import com.quranapp.android.utils.files.AppFiles
import com.quranapp.android.utils.miscellenous.StringUtils
import okio.Path
import okio.Path.Companion.toOkioPath
import org.json.JSONObject
import java.io.File

object TranslUtils {
    const val DIR_NAME = "translations"
    const val DIR_NAME_4_AVAILABLE_DOWNLOADS = "available_translation_downloads"

    fun availableDownloadsFile(): Path {
        return AppFiles.savedFile(DIR_NAME_4_AVAILABLE_DOWNLOADS, TRANSL_AVAILABLE_DOWNLOADS_FILE_NAME)
    }

    fun translationFile(translId: Int, langCode: String, translSlug: String): Path {
        val filename = "translation_${translId}_${langCode}_$translSlug.json"
        return AppFiles.savedFile(DIR_NAME, langCode, translSlug, filename)
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

    fun defaultTranslationSlugs(): HashSet<String> = hashSetOf(TRANSL_SLUG_DEFAULT)

    suspend fun preBuiltTranslBooksInfo(): List<TranslationBookInfoModel> {
        return prebuiltSpecs.map { createPrebuiltTranslBookInfo(it) }
    }

    private suspend fun createPrebuiltTranslBookInfo(
        spec: PrebuiltSpec,
    ): TranslationBookInfoModel {
        val model = TranslationBookInfoModel(spec.slug)
        val manifest = readPrebuiltManifest(spec.manifestPath)
        model.langCode = manifest?.optString("langCode").orEmpty().ifBlank { spec.defaultLangCode }
        model.langName = manifest?.optString("langName").orEmpty().ifBlank { spec.defaultLangName }
        model.bookName = manifest?.optString("book").orEmpty()
        model.authorName = manifest?.optString("author").orEmpty()
        model.displayName = manifest?.optString("displayName").orEmpty().ifBlank { model.bookName }
        model.downloadPath =
            manifest?.optString("downloadPath").orEmpty().ifBlank { spec.defaultDownloadPath }
        model.version = manifest?.optLong("version", 1L) ?: 1L
        model.lastUpdated = model.version
        return model
    }

    private suspend fun readPrebuiltManifest(path: String): JSONObject? {
        return runCatching {
            JSONObject(PlatformServices.assets.readAssetText(path))
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
            StringUtils.formatInvariant(TRANSL_FILE_NAME_FORMAT, translId, langCode, translSlug)
        return "$path2TranslDir/$filename"
    }

    @Throws(Exception::class)
    fun getTranslInfosAndFilesForMigration(
        translDir: File,
    ): List<Pair<TranslationBookInfoModel, File>>? {
        val dirsOfLangCodes = translDir.listFiles()
        if (dirsOfLangCodes.isNullOrEmpty()) {
            Logger.d("Nothing was found, deleting root translation directory: ${translDir.name}")
            AppFiles.deleteRecursively(translDir.toOkioPath())
            return null
        }

        val translInfosAndFiles = ArrayList<Pair<TranslationBookInfoModel, File>>()

        for (langCodeDir in dirsOfLangCodes) {
            val translFiles = langCodeDir.listFiles()
            if (translFiles.isNullOrEmpty()) {
                Logger.d("Deleting language directory with its contents: ${langCodeDir.name}")
                AppFiles.deleteRecursively(langCodeDir.toOkioPath())
                continue
            }

            for (singleTranslDir in translFiles) {
                if (!singleTranslDir.isDirectory) {
                    AppFiles.deleteRecursively(singleTranslDir.toOkioPath())
                    continue
                }

                try {
                    val infoJSONFile = File(singleTranslDir, TRANSL_INFO_FILE_NAME)
                    val pair = readTranslInfoFromJSONFile(infoJSONFile)
                    if (pair == null) {
                        Logger.print(
                            "Deleting translation directory with its manifest and data files: ${singleTranslDir.name}",
                        )
                        AppFiles.deleteRecursively(singleTranslDir.toOkioPath())
                        continue
                    }

                    translInfosAndFiles.add(pair)
                } catch (e: Exception) {
                    Logger.print(
                        "Error occurred, deleting translation directory with its manifest and data files dire: ${singleTranslDir.name}",
                    )
                    AppFiles.deleteRecursively(singleTranslDir.toOkioPath())
                    e.printStackTrace()
                }
            }
        }

        return translInfosAndFiles
    }

    private fun readTranslInfoFromJSONFile(
        infoJSONFile: File,
    ): Pair<TranslationBookInfoModel, File>? {
        if (!infoJSONFile.isFile) return null

        val json = AppFiles.readText(infoJSONFile.toOkioPath())
        val jsonObject = JSONObject(json)

        val slug = jsonObject.optString("slug", "")
        if (isPrebuilt(slug)) return null

        val id = jsonObject.optInt("id")
        val langCode = jsonObject.optString("langCode", jsonObject.optString("lang-code", ""))
        val translFile = translationFile(id, langCode, slug).toFile()

        if (!translFile.exists() || translFile.length() == 0L) {
            Logger.d("Translation file does not exist or is empty.")
            return null
        }

        val bookInfo = TranslationBookInfoModel(slug)
        bookInfo.langCode = langCode
        bookInfo.bookName = jsonObject.optString("book", "")
        bookInfo.authorName = jsonObject.optString("author", "")
        bookInfo.langName = jsonObject.optString("langName", jsonObject.optString("lang-name", ""))
        bookInfo.displayName =
            jsonObject.optString("displayName", jsonObject.optString("display-name", ""))
        bookInfo.version = jsonObject.optLong("version", 1L)
        bookInfo.lastUpdated = bookInfo.version
        bookInfo.downloadPath = jsonObject.optString("downloadPath", "")
        return Pair(bookInfo, translFile)
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
