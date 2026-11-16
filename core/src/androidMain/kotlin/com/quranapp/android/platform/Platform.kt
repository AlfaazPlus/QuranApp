package com.quranapp.android.platform

import com.quranapp.android.api.models.tafsir.TafsirInfoModel
import com.quranapp.android.api.models.translation.TranslationBookInfoModel
import okio.Path
import okio.Source
import java.io.InputStream

interface AppFileSystem {
    val filesDirectory: Path
    fun openAsset(path: String): Source
    fun resourceDirectory(relativePath: String): Path
    fun createFile(file: Path): Boolean
    fun readText(file: Path): String
    fun writeText(file: Path, text: String)
    fun deleteRecursively(file: Path)
}

interface AppLinks {
    fun browse(url: String)
    fun openStore(packageName: String? = null)
    fun storeWebLink(packageName: String? = null): String
    fun shareText(text: String, chooserTitle: String? = null)
    fun shareImage(uri: String, chooserTitle: String? = null)
}

interface FontLoader {
    fun openQuranArabicFontInputStream(
        script: String,
        pageNo: Int,
        isDark: Boolean,
    ): InputStream?
}

interface AtlasTextureStore {
    fun openPrebuiltZip(assetPath: String): InputStream
}

interface AudioPlayer {
    fun playChapterVerse(chapterNo: Int, verseNo: Int)
    fun pause()
}

interface DownloadScheduler {
    fun downloadTranslation(bookInfo: TranslationBookInfoModel)
    fun downloadTafsir(bookInfo: TafsirInfoModel)
    fun downloadScriptFont(script: String)
}

interface AppMessages {
    fun show(text: String, long: Boolean = false)
    fun showClipboard(text: String)
}

object PlatformServices {
    lateinit var files: AppFileSystem
        private set
    lateinit var fonts: FontLoader
        private set
    lateinit var atlas: AtlasTextureStore
        private set
    lateinit var audio: AudioPlayer
        private set
    lateinit var downloads: DownloadScheduler
        private set
    lateinit var links: AppLinks
        private set
    lateinit var messages: AppMessages
        private set

    fun installFiles(files: AppFileSystem) {
        this.files = files
    }

    fun install(
        files: AppFileSystem,
        fonts: FontLoader,
        atlas: AtlasTextureStore,
        audio: AudioPlayer,
        downloads: DownloadScheduler,
        links: AppLinks,
        messages: AppMessages,
    ) {
        this.files = files
        this.fonts = fonts
        this.atlas = atlas
        this.audio = audio
        this.downloads = downloads
        this.links = links
        this.messages = messages
    }
}
