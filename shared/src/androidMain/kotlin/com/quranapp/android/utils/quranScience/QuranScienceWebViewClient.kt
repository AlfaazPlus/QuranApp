package com.quranapp.android.utils.quranScience

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import com.quranapp.android.html.AppWebViewClient
import com.quranapp.android.locale.appPlatformLocale
import com.quranapp.android.platform.PlatformServices
import com.quranapp.android.platform.toInputStream
import com.quranapp.android.theme.ThemeUtils
import com.quranapp.android.theme.isDarkTheme
import com.quranapp.android.utils.app.AppDestinations
import com.quranapp.android.utils.reader.FontResolver
import com.quranapp.android.utils.reader.factory.ReaderFactory
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URLConnection
import java.util.concurrent.ConcurrentHashMap

open class QuranScienceWebViewClient(
    private val atlasPngCache: ConcurrentHashMap<String, ByteArray> = ConcurrentHashMap(),
    private var quranArabicScriptCode: String,
    private var onOpenReference: (chapterNo: Int, fromVerse: Int, toVerse: Int) -> Unit,
) : AppWebViewClient() {

    fun update(
        quranArabicScriptCode: String,
        onOpenReference: (chapterNo: Int, fromVerse: Int, toVerse: Int) -> Unit,
    ) {
        this.quranArabicScriptCode = quranArabicScriptCode
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

                val data = runBlocking {
                    PlatformServices.assets.readAssetBytes(
                        uri.toString().substring("https://assets-file/".length),
                    ).inputStream()
                }

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
                val pageNo = extractQuranArabicPageNo(uri)

                val data: InputStream? = FontResolver.getInstance(ctx.applicationContext)
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
                        setClassName(view.context, AppDestinations.READER)
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
