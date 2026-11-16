package com.quranapp.android.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import com.quranapp.android.api.models.tafsir.TafsirInfoModel
import com.quranapp.android.api.models.translation.TranslationBookInfoModel
import com.quranapp.android.components.reader.ChapterVersePair
import com.quranapp.android.utils.managers.ScriptFontsDownloadManager
import com.quranapp.android.utils.managers.TafsirDownloadManager
import com.quranapp.android.utils.managers.TranslationDownloadManager
import com.quranapp.android.utils.mediaplayer.RecitationController
import com.quranapp.android.utils.reader.FontResolver
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.lang.ref.WeakReference

fun installAndroidFileSystem(context: Context) {
    // Called from Application.attachBaseContext, where applicationContext is still null.
    PlatformServices.installFiles(AndroidAppFileSystem(context))
}

fun installAndroidPlatform(context: Context) {
    val app = context.applicationContext
    PlatformServices.install(
        files = AndroidAppFileSystem(app),
        fonts = FontResolver.getInstance(app),
        atlas = AndroidAtlasTextureStore(app),
        audio = AndroidAudioPlayer(app),
        downloads = AndroidDownloadScheduler(app),
        links = AndroidAppLinks(app),
        messages = AndroidAppMessages(app),
    )
}

private class AndroidAppMessages(
    private val context: Context,
) : AppMessages {
    private var toast: WeakReference<Toast>? = null

    override fun show(text: String, long: Boolean) {
        showToast(text, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT)
    }

    override fun show(messageRes: Int, long: Boolean) {
        show(context.getString(messageRes), long)
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

private class AndroidAppFileSystem(
    private val context: Context,
) : AppFileSystem {
    override val filesDirectory: File
        get() = context.filesDir

    override fun openAsset(path: String): InputStream = context.assets.open(path)

    override fun resourceDirectory(relativePath: String): File {
        val directory = File(filesDirectory, relativePath)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return directory
    }

    override fun createFile(file: File): Boolean {
        if (file.exists()) return true

        return try {
            file.parentFile?.mkdirs()
            file.createNewFile()
        } catch (_: IOException) {
            false
        }
    }

    override fun readText(file: File): String = file.readText()

    override fun writeText(file: File, text: String) {
        file.writeText(text)
    }

    override fun deleteRecursively(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteRecursively(it) }
        }

        if (file.exists() && !file.delete()) {
            throw IOException("Failed to delete: ${file.absolutePath}")
        }
    }
}

private class AndroidAppLinks(
    private val context: Context,
) : AppLinks {
    override fun browse(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        start(intent)
    }

    override fun openStore(packageName: String?) {
        val market =
            Intent(Intent.ACTION_VIEW, Uri.parse(storeLink(packageName, market = true))).apply {
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
            putExtra(Intent.EXTRA_STREAM, Uri.parse(uri))
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

private class AndroidAtlasTextureStore(
    private val context: Context,
) : AtlasTextureStore {
    override fun openPrebuiltZip(assetPath: String): InputStream =
        context.assets.open(assetPath)
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

private class AndroidDownloadScheduler(
    private val context: Context,
) : DownloadScheduler {
    override fun downloadTranslation(bookInfo: TranslationBookInfoModel) {
        TranslationDownloadManager.startDownload(context, bookInfo)
    }

    override fun downloadTafsir(bookInfo: TafsirInfoModel) {
        TafsirDownloadManager.startDownload(context, bookInfo)
    }

    override fun downloadScriptFont(script: String) {
        ScriptFontsDownloadManager.startDownload(context, script)
    }
}
