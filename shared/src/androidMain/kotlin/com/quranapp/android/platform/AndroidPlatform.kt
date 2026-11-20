package com.quranapp.android.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.widget.Toast
import androidx.core.net.toUri
import com.quranapp.android.db.QURAN_DATABASE_NAME
import com.quranapp.android.models.ChapterVersePair
import com.quranapp.android.navigation.AndroidAppNavigator
import com.quranapp.android.platform.clipboard.AndroidClipboardManager
import com.quranapp.android.platform.notification.AndroidNotificationManager
import com.quranapp.android.platform.shortcut.AndroidShortcutManager
import com.quranapp.android.resources.ComposeResourceFiles
import com.quranapp.android.task.AndroidTaskManager
import com.quranapp.android.utils.mediaplayer.RecitationController
import com.quranapp.android.utils.reader.FontResolver
import com.quranapp.platform.AppImageLoader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toOkioPath
import okio.Path.Companion.toPath
import okio.Source
import okio.buffer
import okio.source
import java.io.File
import java.lang.ref.WeakReference
import java.util.zip.ZipFile

fun installAndroidFileSystem(context: Context) {
    // Called from Application.attachBaseContext, where applicationContext is still null.
    ComposeResourceFiles.install(context)
    PlatformServices.installFiles(AndroidAppFileSystem(context))
}

fun installAndroidPlatform(context: Context) {
    val app = context.applicationContext
    ComposeResourceFiles.install(app)

    PlatformServices.install(
        assets = DefaultAppAssets(),
        files = AndroidAppFileSystem(app),
        fonts = FontResolver.getInstance(app),
        audio = AndroidAudioPlayer(app),
        links = AndroidAppLinks(app),
        messages = AndroidAppMessages(app),
        networkMonitor = AndroidNetworkMonitor(app),
        system = AndroidSystemServices(app),
        navigator = AndroidAppNavigator(app),
    )
}

private class AndroidAppMessages(
    private val context: Context,
) : AppMessages {
    private var toast: WeakReference<Toast>? = null

    override fun show(text: String, long: Boolean) {
        showToast(text, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT)
    }

    override fun showClipboard(text: String) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            show(text)
        }
    }

    private fun showToast(text: String, duration: Int) {
        try {
            toast?.get()?.cancel()
        } catch (_: Exception) {
        }

        val next = Toast.makeText(context, text, duration)
        toast = WeakReference(next)
        next.show()
    }
}

class AndroidZipReader : AppZipReader {

    override fun open(path: Path): AppZipArchive {
        return AndroidZipArchive(path)
    }
}

private class AndroidZipArchive(path: Path) : AppZipArchive {
    private val zipFile = ZipFile(path.toFile())

    private val entries = zipFile.entries().asSequence()
        .filterNot { it.isDirectory }
        .associateBy { normalizeZipPath(it.name) }

    override fun entries(): List<String> = entries.keys.toList()

    override fun readEntry(path: String): ByteArray {
        val entry = findEntry(path)

        return zipFile.getInputStream(entry).use { input ->
            input.readBytes()
        }
    }

    override fun openEntry(path: String): Source {
        val entry = findEntry(path)

        return zipFile.getInputStream(entry).source()
    }

    override fun copyEntry(path: String, destination: Path) {
        openEntry(path).use { source ->
            FileSystem.SYSTEM.sink(destination).buffer().use { sink ->
                sink.writeAll(source)
            }
        }
    }

    private fun findEntry(path: String) =
        entries[normalizeZipPath(path)]
            ?: error("ZIP entry not found: $path")

    override fun close() {
        zipFile.close()
    }

    private fun normalizeZipPath(path: String): String {
        return path.trimStart('/').replace('\\', '/')
    }
}

private class AndroidAppFileSystem(
    context: Context,
) : AppFileSystem {
    private val appContext = context.applicationContext ?: context

    override val zipReader: AppZipReader
        get() = AndroidZipReader()

    override val imageLoader: AppImageLoader
        get() = createAppImageLoader(appContext)

    override val filesDirectory: Path
        get() = appContext.filesDir.toOkioPath()

    override val cacheDirectory: Path
        get() = appContext.cacheDir.toOkioPath()

    override val databaseDirectory: Path =
        appContext.getDatabasePath(QURAN_DATABASE_NAME)
            .parentFile!!
            .absolutePath
            .toPath()

    override fun resourceDirectory(relativePath: String): Path {
        val directory = File(appContext.filesDir, relativePath)

        if (!directory.exists()) {
            directory.mkdirs()
        }

        return directory.toOkioPath()
    }
}

private class AndroidAppLinks(
    private val context: Context,
) : AppLinks {
    override fun browse(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        start(intent)
    }

    override fun openStore(packageName: String?) {
        val market =
            Intent(Intent.ACTION_VIEW, storeLink(packageName, market = true).toUri()).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

        try {
            context.startActivity(market)
        } catch (_: ActivityNotFoundException) {
            browse(storeLink(packageName, market = false))
        }
    }

    override fun storeWebLink(packageName: String?): String {
        return storeLink(packageName, market = false)
    }

    override fun shareText(text: String, chooserTitle: String?) {
        share(mimeType = "text/plain", chooserTitle = chooserTitle) {
            putExtra(Intent.EXTRA_TEXT, text)
        }
    }

    override fun shareImage(uri: String, chooserTitle: String?) {
        share(mimeType = "image/*", chooserTitle = chooserTitle) {
            putExtra(Intent.EXTRA_STREAM, uri.toUri())
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun share(
        mimeType: String,
        chooserTitle: String?,
        fill: Intent.() -> Unit,
    ) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            fill()
        }

        val chooser = Intent.createChooser(send, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        start(chooser)
    }

    override fun openAppSettings() {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        start(intent)
    }

    private fun storeLink(packageName: String?, market: Boolean): String {
        val pkg = packageName ?: context.packageName.replace(".debug", "")
        return if (market) {
            "market://details?id=$pkg"
        } else {
            "https://play.google.com/store/apps/details?id=$pkg"
        }
    }

    private fun start(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "Failed to open.", Toast.LENGTH_SHORT).show()
        }
    }
}

private class AndroidAudioPlayer(
    context: Context,
) : AudioPlayer {
    private val controller = RecitationController.getInstance(context)

    override fun playChapterVerse(chapterNo: Int, verseNo: Int) {
        controller.playControl(ChapterVersePair(chapterNo, verseNo))
    }

    override fun pause() {
        controller.pause()
    }
}

class AndroidNetworkMonitor(
    context: Context
) : NetworkMonitor {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager

    private val _isConnected = MutableStateFlow(checkConnection())

    override val isConnectedFlow: StateFlow<Boolean> = _isConnected
    override val isConnected: Boolean get() = isConnectedFlow.value

    private fun checkConnection(): Boolean {
        val network = connectivityManager.activeNetwork
            ?: return false

        val capabilities =
            connectivityManager.getNetworkCapabilities(network)
                ?: return false

        return capabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_INTERNET
        ) && capabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_VALIDATED
        )
    }

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isConnected.value = checkConnection()
        }

        override fun onLost(network: Network) {
            _isConnected.value = checkConnection()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            capabilities: NetworkCapabilities
        ) {
            _isConnected.value = checkConnection()
        }
    }

    init {
        connectivityManager.registerDefaultNetworkCallback(callback)
    }
}

class AndroidSystemServices(private val app: Context) : SystemServices {
    override val taskManager
        get() = AndroidTaskManager(app)

    override val notificationManager
        get() = AndroidNotificationManager(app)

    override val shortcutManager
        get() = AndroidShortcutManager(app)

    override val clipboardManager
        get() = AndroidClipboardManager(app)
}
