package com.quranapp.android.platform

import com.quranapp.android.api.models.tafsir.TafsirInfoModel
import com.quranapp.android.api.models.translation.TranslationBookInfoModel
import com.quranapp.android.platform.notification.NotificationManager
import com.quranapp.android.platform.shortcut.ShortcutManager
import com.quranapp.android.task.TaskManager
import com.quranapp.platform.AppImageLoader
import kotlinx.coroutines.flow.StateFlow
import okio.Path
import okio.Source

interface AppAssets {
    /**
     * Reads an asset as bytes.
     */
    suspend fun readAssetBytes(relativePath: String): ByteArray

    /**
     * Reads an asset as text.
     */
    suspend fun readAssetText(relativePath: String): String
    suspend fun readAssetTextOrNull(relativePath: String): String?

    suspend fun assetUri(relativePath: String): String
}


interface AppZipReader {
    fun open(path: Path): AppZipArchive
}

interface AppZipArchive : AutoCloseable {
    fun entries(): List<String>

    fun readEntry(path: String): ByteArray

    fun openEntry(path: String): Source

    fun copyEntry(path: String, destination: Path)
}


interface AppFileSystem {
    val zipReader: AppZipReader
    val imageLoader: AppImageLoader
    val filesDirectory: Path
    val cacheDirectory: Path
    val databaseDirectory: Path
    fun resourceDirectory(relativePath: String): Path
}

interface AppLinks {
    fun browse(url: String)
    fun openStore(packageName: String? = null)
    fun storeWebLink(packageName: String? = null): String
    fun shareText(text: String, chooserTitle: String? = null)
    fun shareImage(uri: String, chooserTitle: String? = null)
}

interface FontLoader {
    fun openQuranArabicFont(
        script: String,
        pageNo: Int,
        isDark: Boolean,
    ): Source?
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

interface NetworkMonitor {
    val isConnectedFlow: StateFlow<Boolean>
    val isConnected: Boolean
}

interface SystemServices {
    val taskManager: TaskManager
    val notificationManager: NotificationManager
    val shortcutManager: ShortcutManager
}

object PlatformServices {
    lateinit var assets: AppAssets
        private set
    lateinit var files: AppFileSystem
        private set
    lateinit var fonts: FontLoader
        private set
    lateinit var audio: AudioPlayer
        private set
    lateinit var downloads: DownloadScheduler
        private set
    lateinit var links: AppLinks
        private set
    lateinit var messages: AppMessages
        private set
    lateinit var networkMonitor: NetworkMonitor
        private set
    lateinit var system: SystemServices
        private set

    fun installFiles(files: AppFileSystem) {
        this.files = files
    }

    fun install(
        assets: AppAssets,
        files: AppFileSystem,
        fonts: FontLoader,
        audio: AudioPlayer,
        downloads: DownloadScheduler,
        links: AppLinks,
        messages: AppMessages,
        networkMonitor: NetworkMonitor,
        system: SystemServices,
    ) {
        this.assets = assets
        this.files = files
        this.fonts = fonts
        this.audio = audio
        this.downloads = downloads
        this.links = links
        this.messages = messages
        this.networkMonitor = networkMonitor
        this.system = system
    }
}
