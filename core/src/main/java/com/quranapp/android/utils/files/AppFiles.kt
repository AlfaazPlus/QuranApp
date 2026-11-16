package com.quranapp.android.utils.files

import com.quranapp.android.platform.PlatformServices
import com.quranapp.android.utils.app.AppUtils
import com.quranapp.android.utils.chapterInfo.ChapterInfoUtils
import com.quranapp.android.utils.reader.QuranScriptUtils
import com.quranapp.android.utils.reader.TranslUtils
import com.quranapp.android.utils.tafsir.TafsirUtils
import java.io.File

fun createPath(vararg parts: String): String {
    return parts.filter { it.isNotEmpty() }.joinToString("/")
}

object AppFiles {
    fun createPath(vararg parts: String): String {
        return com.quranapp.android.utils.files.createPath(*parts)
    }

    private val files get() = PlatformServices.files

    val filesDirectory: File
        get() = files.filesDirectory

    fun resourceDir(relativePath: String): File = files.resourceDirectory(relativePath)

    fun createFile(file: File): Boolean = files.createFile(file)

    fun readText(file: File): String = files.readText(file)

    fun writeText(file: File, text: String) {
        files.writeText(file, text)
    }

    fun deleteRecursively(file: File) {
        files.deleteRecursively(file)
    }

    fun deleteRecursively(path: String) {
        val file = File(path)
        if (!file.exists()) return
        files.deleteRecursively(file)
    }

    val translsManifestFile: File
        get() = File(
            resourceDir(TranslUtils.DIR_NAME_4_AVAILABLE_DOWNLOADS),
            TranslUtils.TRANSL_AVAILABLE_DOWNLOADS_FILE_NAME,
        )

    val resourcesVersionsFile: File
        get() = File(resourceDir(AppUtils.APP_OTHER_DIR), "resources_versions.json")

    val appUpdatesFile: File
        get() = File(resourceDir(AppUtils.APP_OTHER_DIR), "app_updates.json")

    val tafsirsManifestFile: File
        get() = File(resourceDir(TafsirUtils.DIR_NAME), TafsirUtils.AVAILABLE_TAFSIRS_FILENAME)

    val scriptFontDir: File
        get() = resourceDir(QuranScriptUtils.FONTS_DIR_NAME)

    fun getKFQPCScriptFontDir(scriptSlug: String): File {
        return File(scriptFontDir, scriptSlug)
    }

    fun getScriptFile(scriptKey: String): File {
        return File(resourceDir(QuranScriptUtils.SCRIPT_DIR_NAME), "$scriptKey.json")
    }

    fun getChapterInfoFile(langCode: String, chapterNo: Int): File {
        return File(
            resourceDir(ChapterInfoUtils.DIR_NAME),
            ChapterInfoUtils.prepareChapterInfoFilePath(langCode, chapterNo),
        )
    }

    fun getSingleTranslationFile(translId: Int, langCode: String, translSlug: String): File {
        return File(
            resourceDir(TranslUtils.DIR_NAME),
            TranslUtils.prepareTranslPathForSpecificLangNSlug(translId, langCode, translSlug),
        )
    }
}
