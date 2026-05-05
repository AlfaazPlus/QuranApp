package com.quranapp.android.utils.quranScience

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.webkit.WebViewClientCompat
import com.quranapp.android.activities.ActivityReader
import com.quranapp.android.compose.utils.ThemeUtils
import com.quranapp.android.compose.utils.appPlatformLocale
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.utils.reader.FontResolver
import com.quranapp.android.utils.reader.factory.ReaderFactory
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URLConnection
import java.util.concurrent.ConcurrentHashMap

open class QuranScienceWebViewClient(
    private val atlasPngCache: ConcurrentHashMap<String, ByteArray> = ConcurrentHashMap(),
    private val onOpenReference: (chapterNo: Int, fromVerse: Int, toVerse: Int) -> Unit,
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


    @Throws(IOException::class)
    private fun handleRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        val uri = request.url
        val host = uri.host ?: return null
        val ctx = view.context
        val uriStr = uri.toString().lowercase(appPlatformLocale())

        when (host) {
            "assets-file" -> {
                val headers = request.requestHeaders.toMutableMap()
                headers["Access-Control-Allow-Origin"] = "*"

                val data = ctx.assets.open(uri.toString().substring("https://assets-file/".length))

                return WebResourceResponse(
                    URLConnection.guessContentTypeFromName(uri.path),
                    "utf-8",
                    200,
                    "OK",
                    headers,
                    data
                )
            }

            "assets-atlas" -> {
                val headers = request.requestHeaders.toMutableMap()
                val key = uri.toString()
                val bytes = atlasPngCache[key] ?: return null

                headers["Access-Control-Allow-Origin"] = "*"

                return WebResourceResponse(
                    "image/png",
                    null,
                    200,
                    "OK",
                    headers,
                    ByteArrayInputStream(bytes)
                )
            }

            "assets-font" -> {
                if (!uriStr.contains("quran-arabic")) return null

                val headers = request.requestHeaders.toMutableMap()
                val savedScript = ReaderPreferences.getQuranScript()
                val pageNo = extractQuranArabicPageNo(uri)

                val data: InputStream? = FontResolver.getInstance(ctx.applicationContext)
                    .openQuranArabicFontInputStream(
                        savedScript,
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
                    data
                )
            }
        }

        return null
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val uri = request.url
        val host = uri.host ?: return true

        val pathSegments = uri.pathSegments

        if (pathSegments.size == 2 && host.contains("quranapp.verse")) {
            val chapterNo = pathSegments[0].toInt()
            val verses = pathSegments[1].split("-")
            val fromVerse = verses[0].toInt()
            var toVerse = fromVerse
            if (verses.size > 1) {
                toVerse = verses[1].toInt()
            }

            if (host == "quranapp.verse.ref") {
                onOpenReference(chapterNo, fromVerse, toVerse)
            } else if (host == "quranapp.verse.reader") {
                view.context.startActivity(
                    ReaderFactory.prepareVerseRangeIntent(
                        chapterNo,
                        fromVerse,
                        toVerse
                    ).apply {
                        setClass(view.context, ActivityReader::class.java)
                    }
                )
            }
        }

        return true
    }
}

private fun extractQuranArabicPageNo(uri: android.net.Uri): Int {
    val last = uri.lastPathSegment ?: return 1

    return if (last.startsWith("page_")) {
        last.removePrefix("page_").toIntOrNull() ?: 1
    } else {
        1
    }
}
