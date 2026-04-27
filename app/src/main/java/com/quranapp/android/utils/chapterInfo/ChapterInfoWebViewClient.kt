package com.quranapp.android.utils.chapterInfo

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.quranapp.android.R
import com.quranapp.android.compose.screens.chapterInfo.ChapterInfoContentData
import com.quranapp.android.db.entities.quran.RevelationType
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.quran.QuranGlyphs
import com.quranapp.android.utils.univ.StringUtils
import org.json.JSONObject
import java.net.URLConnection

class ChapterInfoWebViewClient(
    private val data: ChapterInfoContentData,
    private val onPageFinished: (() -> Unit)? = null,
) : WebViewClient() {

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest,
    ): WebResourceResponse? {
        return try {
            handleRequest(view, request) ?: super.shouldInterceptRequest(view, request)
        } catch (e: Exception) {
            e.printStackTrace()
            super.shouldInterceptRequest(view, request)
        }
    }

    private fun handleRequest(
        view: WebView,
        request: WebResourceRequest,
    ): WebResourceResponse? {
        val uri = request.url
        val host = uri.host ?: return super.shouldInterceptRequest(view, request)
        val uriStr = uri.toString().lowercase()
        val ctx = view.context

        val inputStream = when (host) {
            "assets-file" -> {
                ctx.assets.open(uri.toString().removePrefix("https://assets-file/"))
            }

            "assets-font" -> when {
                uriStr.contains("surah-icon") ->
                    ctx.resources.openRawResource(R.font.suracon)

                data.language == "ur" && uriStr.contains("content") ->
                    ctx.resources.openRawResource(R.font.noto_nastaliq_urdu_variable)

                else -> null
            }

            "assets-image" -> when {
                uriStr.contains("revelation") -> {
                    val resId = if (data.revelationType == RevelationType.meccan) {
                        R.drawable.dr_makkah_old
                    } else {
                        R.drawable.dr_madina_old
                    }
                    ctx.resources.openRawResource(resId)
                }

                else -> null
            }

            else -> null
        } ?: return null

        val headers = request.requestHeaders.toMutableMap()
        headers["Access-Control-Allow-Origin"] = "*"

        return WebResourceResponse(
            URLConnection.guessContentTypeFromName(uri.path),
            "utf-8",
            200,
            "OK",
            headers,
            inputStream,
        )
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        return true
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        onPageFinished?.invoke()
    }
}
