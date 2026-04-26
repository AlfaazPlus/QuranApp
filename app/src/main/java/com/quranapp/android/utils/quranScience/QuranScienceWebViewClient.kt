package com.quranapp.android.utils.quranScience

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.webkit.WebViewClientCompat
import com.quranapp.android.activities.ActivityReader
import com.quranapp.android.activities.reference.ActivityQuranScienceContent
import com.quranapp.android.compose.utils.appLocale
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.utils.reader.factory.ReaderFactory
import com.quranapp.android.utils.reader.getQuranScriptFontHasDark
import com.quranapp.android.utils.reader.getQuranScriptFontRes
import com.quranapp.android.utils.reader.isKFQPCScript
import com.quranapp.android.utils.reader.toKFQPCFontFilename
import com.quranapp.android.utils.reader.toKFQPCFontFilenameOld
import com.quranapp.android.utils.univ.FileUtils
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URLConnection


open class QuranScienceWebViewClient(private val activity: ActivityQuranScienceContent) :
    WebViewClientCompat() {
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
        var data: InputStream? = null
        val uriStr = uri.toString().lowercase(appLocale())
        when (host) {
            "assets-file" -> data =
                ctx.assets.open(uri.toString().substring("https://assets-file/".length))

            "assets-font" -> {
                if (uriStr.contains("quran-arabic")) {
                    val savedScript = ReaderPreferences.getQuranScript()
                    val fileUtils = FileUtils.newInstance(ctx)
                    if (savedScript.isKFQPCScript()) {
                        val lastSeg = uri.lastPathSegment ?: return null

                        val pageNo =
                            if (lastSeg.startsWith("page_")) lastSeg.removePrefix("page_")
                                .toIntOrNull()
                            else null

                        if (pageNo != null) {
                            val fontsDir = fileUtils.getKFQPCScriptFontDir(savedScript)
                            val useDark =
                                ThemeUtils.isDarkTheme(ctx) && savedScript.getQuranScriptFontHasDark()
                            val primaryFile =
                                File(fontsDir, pageNo.toKFQPCFontFilename(useDark))
                            val lightFile =
                                File(fontsDir, pageNo.toKFQPCFontFilename(false))
                            val oldFile = File(fontsDir, pageNo.toKFQPCFontFilenameOld())

                            data = when {
                                primaryFile.exists() && primaryFile.length() > 0L ->
                                    primaryFile.inputStream()

                                useDark && lightFile.exists() && lightFile.length() > 0L ->
                                    lightFile.inputStream()

                                oldFile.exists() && oldFile.length() > 0L ->
                                    oldFile.inputStream()

                                else -> null
                            }
                        }
                    } else {
                        data = ctx.resources.openRawResource(
                            savedScript.getQuranScriptFontRes(ThemeUtils.isDarkTheme(ctx))
                        )
                    }
                }
            }
        }

        if (data == null) {
            return null
        }

        val headers = request.requestHeaders

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
                activity.showQuickReference(chapterNo, fromVerse, toVerse)
            } else if (host == "quranapp.verse.reader") {
                activity.startActivity(
                    ReaderFactory.prepareVerseRangeIntent(
                        chapterNo,
                        fromVerse,
                        toVerse
                    ).apply {
                        setClass(activity, ActivityReader::class.java)
                    }
                )
            }
        }

        return true
    }
}