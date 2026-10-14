package com.quranapp.android.utils.tafsir

import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.webkit.WebViewClientCompat
import com.quranapp.android.R
import com.quranapp.android.activities.ActivityReader
import com.quranapp.android.compose.utils.ThemeUtils
import com.quranapp.android.compose.utils.appPlatformLocale
import com.quranapp.android.utils.reader.FontResolver
import com.quranapp.android.utils.reader.factory.ReaderFactory
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
    private val tafsirKey: String,
    private val quranArabicScriptCode: String,
    private val atlasAyahImageCache: ConcurrentHashMap<String, ByteArray>? = null,
    private val onPageFinished: (() -> Unit)? = null,
    private val onOpenReference: (chapterNo: Int, verses: String) -> Unit = { _, _ -> },
) : WebViewClientCompat() {

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
        onPageFinished?.invoke()
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
                    setClass(view.context, ActivityReader::class.java)
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
                data = ctx.assets.open(uri.toString().substring("https://assets-file/".length))
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
                        .openQuranArabicFontInputStream(
                            quranArabicScriptCode,
                            pageNo,
                            ThemeUtils.isDarkTheme(ctx),
                        )
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
                    data = ctx.resources.openRawResource(R.font.uthmanic_hafs)
                } else if (uriStr.contains("content")) {
                    data = when {
                        TafsirUtils.isUrdu(tafsirKey) -> {
                            ctx.resources.openRawResource(R.font.noto_nastaliq_urdu_regular)
                        }

                        TafsirUtils.isArabic(tafsirKey) -> {
                            ctx.resources.openRawResource(R.font.scheherazadenew_regular)
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
