package com.quranapp.android.utils.webview

import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.noto_nastaliq_urdu_regular
import com.quranapp.android.generated.resources.scheherazadenew_regular
import com.quranapp.android.generated.resources.uthmanic_hafs
import com.quranapp.android.html.AppWebViewClient
import com.quranapp.android.locale.appPlatformLocale
import com.quranapp.android.platform.PlatformServices
import com.quranapp.android.platform.toInputStream
import com.quranapp.android.resources.ComposeResourceFiles
import com.quranapp.android.theme.ThemeUtils
import com.quranapp.android.theme.isDarkTheme
import com.quranapp.android.utils.app.AppDestinations
import com.quranapp.android.utils.reader.FontResolver
import com.quranapp.android.utils.reader.factory.ReaderFactory
import com.quranapp.android.utils.tafsir.TafsirUtils
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URLConnection
import java.util.concurrent.ConcurrentHashMap

/**
 * WebViewClient for Tafsir content that handles:
 * - Asset file loading (CSS, JS)
 * - Font loading (Uthmani, content fonts based on language)
 * - Optional verse header: Quran Arabic page fonts and Atlas ayah PNGs
 * - quranapp.verse.ref / quranapp.verse.reader links
 */
class TafsirWebViewClient(
    private var tafsirKey: String,
    private var quranArabicScriptCode: String,
    private var atlasAyahImageCache: ConcurrentHashMap<String, ByteArray>? = null,
    private var onPageReady: (() -> Unit)? = null,
    private var onOpenReference: (chapterNo: Int, verses: String) -> Unit = { _, _ -> },
) : AppWebViewClient() {

    /**
     * Keep one client for the lifetime of the WebView. Replacing it while a load is in
     * progress drops the page-finished callback, which leaves the reader spinner up.
     */
    fun update(
        tafsirKey: String,
        quranArabicScriptCode: String,
        atlasAyahImageCache: ConcurrentHashMap<String, ByteArray>?,
        onPageReady: (() -> Unit)?,
        onOpenReference: (chapterNo: Int, verses: String) -> Unit,
    ) {
        this.tafsirKey = tafsirKey
        this.quranArabicScriptCode = quranArabicScriptCode
        this.atlasAyahImageCache = atlasAyahImageCache
        this.onPageReady = onPageReady
        this.onOpenReference = onOpenReference
    }

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        try {
            val webResourceResponse = handleRequest(view, request)
            if (webResourceResponse != null) {
                return webResourceResponse
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return super.shouldInterceptRequest(view, request)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        onPageReady?.invoke()
    }

    override fun onPageCommitVisible(view: WebView, url: String) {
        super.onPageCommitVisible(view, url)
        onPageReady?.invoke()
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val uri = request.url
        val host = uri.host ?: return false
        val pathSegments = uri.pathSegments

        if (pathSegments.size != 2 || !host.contains("quranapp.verse")) {
            return false
        }

        val chapterNo = pathSegments[0].toIntOrNull() ?: return true
        val verses = pathSegments[1]
        if (verses.isEmpty()) return true

        if (host == "quranapp.verse.ref") {
            onOpenReference(chapterNo, verses)
        } else if (host == "quranapp.verse.reader") {
            val parts = verses.split("-")
            val fromVerse = parts[0].toIntOrNull() ?: return true
            val toVerse = if (parts.size > 1) {
                parts[1].toIntOrNull() ?: return true
            } else {
                fromVerse
            }
            view.context.startActivity(
                ReaderFactory.prepareVerseRangeIntent(
                    chapterNo,
                    fromVerse,
                    toVerse,
                ).apply {
                    setClassName(view.context, AppDestinations.READER)
                }
            )
        }

        return true
    }

    @Throws(IOException::class)
    private fun handleRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        val uri = request.url
        val host = uri.host ?: return null
        val ctx = view.context
        var data: InputStream? = null
        val uriStr = uri.toString().lowercase(appPlatformLocale())

        when (host) {
            "assets-file" -> {
                data = runBlocking {
                    PlatformServices.assets.readAssetBytes(
                        uri.toString().substring("https://assets-file/".length),
                    ).inputStream()
                }
            }

            "assets-atlas" -> {
                val cache = atlasAyahImageCache ?: return null

                val key = uri.toString()

                val bytes = cache[key] ?: return null

                val headers = request.requestHeaders.toMutableMap()

                headers["Access-Control-Allow-Origin"] = "*"

                return WebResourceResponse(
                    "image/png",
                    null,
                    200,
                    "OK",
                    headers,
                    ByteArrayInputStream(bytes),
                )
            }

            "assets-font" -> {
                if (uriStr.contains("quran-arabic")) {
                    val headers = request.requestHeaders.toMutableMap()
                    val pageNo = extractQuranArabicPageNo(uri)
                    val fontStream: InputStream? = FontResolver.getInstance(ctx.applicationContext)
                        .openQuranArabicFont(
                            quranArabicScriptCode,
                            pageNo,
                            ThemeUtils.isDarkTheme(ctx),
                        )
                        ?.toInputStream()
                        ?: return null
                    headers["Access-Control-Allow-Origin"] = "*"
                    return WebResourceResponse(
                        URLConnection.guessContentTypeFromName(uri.path),
                        "utf-8",
                        200,
                        "OK",
                        headers,
                        fontStream,
                    )
                }
                if (uriStr.contains("uthmani")) {
                    data =
                        ComposeResourceFiles.fontStream(Res.font.uthmanic_hafs, "uthmanic_hafs.ttf")
                } else if (uriStr.contains("content")) {
                    data = when {
                        TafsirUtils.isUrdu(tafsirKey) -> {
                            ComposeResourceFiles.fontStream(
                                Res.font.noto_nastaliq_urdu_regular,
                                "noto_nastaliq_urdu_regular.ttf",
                            )
                        }

                        TafsirUtils.isArabic(tafsirKey) -> {
                            ComposeResourceFiles.fontStream(
                                Res.font.scheherazadenew_regular,
                                "scheherazadenew_regular.ttf",
                            )
                        }

                        else -> null
                    }
                }
            }
        }

        if (data == null) {
            return null
        }

        val headers = request.requestHeaders.toMutableMap()
        headers["Access-Control-Allow-Origin"] = "*"

        return WebResourceResponse(
            URLConnection.guessContentTypeFromName(uri.path),
            "utf-8",
            200,
            "OK",
            headers,
            data,
        )
    }
}

private fun extractQuranArabicPageNo(uri: Uri): Int {
    val last = uri.lastPathSegment ?: return 1
    return if (last.startsWith("page_")) {
        last.removePrefix("page_").toIntOrNull() ?: 1
    } else {
        1
    }
}
