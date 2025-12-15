package com.quranapp.android.utils.tafsir

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.webkit.WebViewClientCompat
import com.quranapp.android.R
import java.io.IOException
import java.io.InputStream
import java.net.URLConnection
import java.util.Locale

/**
 * WebViewClient for Tafsir content that handles:
 * - Asset file loading (CSS, JS)
 * - Font loading (Uthmani, content fonts based on language)
 */
class TafsirWebViewClient(
    private val tafsirKey: String,
    private val onPageFinished: (() -> Unit)? = null
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

    @Throws(IOException::class)
    private fun handleRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        val uri = request.url
        val host = uri.host ?: return null
        val ctx = view.context
        var data: InputStream? = null
        val uriStr = uri.toString().lowercase(Locale.getDefault())

        when (host) {
            "assets-file" -> {
                data = ctx.assets.open(uri.toString().substring("https://assets-file/".length))
            }

            "assets-font" -> {
                if (uriStr.contains("uthmani")) {
                    data = ctx.resources.openRawResource(R.font.uthmanic_hafs)
                } else if (uriStr.contains("content")) {
                    data = when {
                        TafsirUtils.isUrdu(tafsirKey) -> {
                            ctx.resources.openRawResource(R.font.noto_nastaliq_urdu_variable)
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
            data
        )
    }
}
