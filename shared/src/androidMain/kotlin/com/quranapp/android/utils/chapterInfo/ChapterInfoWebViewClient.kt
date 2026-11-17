package com.quranapp.android.utils.chapterInfo

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import com.quranapp.android.db.entities.quran.RevelationType
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.dr_madina_old
import com.quranapp.android.generated.resources.dr_makkah_old
import com.quranapp.android.generated.resources.noto_nastaliq_urdu_regular
import com.quranapp.android.generated.resources.suracon
import com.quranapp.android.html.AppWebViewClient
import com.quranapp.android.models.ChapterInfoContentData
import com.quranapp.android.platform.PlatformServices
import com.quranapp.android.resources.ComposeResourceFiles
import kotlinx.coroutines.runBlocking
import java.net.URLConnection

class ChapterInfoWebViewClient(
    private var data: ChapterInfoContentData,
    private var onPageFinished: (() -> Unit)? = null,
) : AppWebViewClient() {

    fun update(
        data: ChapterInfoContentData,
        onPageFinished: (() -> Unit)?,
    ) {
        this.data = data
        this.onPageFinished = onPageFinished
    }

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

        val inputStream = when (host) {
            "assets-file" -> {
                runBlocking {
                    PlatformServices.assets.readAssetBytes(
                        uri.toString().removePrefix("https://assets-file/"),
                    ).inputStream()
                }
            }

            "assets-font" -> when {
                uriStr.contains("surah-icon") ->
                    ComposeResourceFiles.fontStream(Res.font.suracon, "suracon.ttf")

                data.language == "ur" && uriStr.contains("content") ->
                    ComposeResourceFiles.fontStream(
                        Res.font.noto_nastaliq_urdu_regular,
                        "noto_nastaliq_urdu_regular.ttf",
                    )

                else -> null
            }

            "assets-image" -> when {
                uriStr.contains("revelation") -> {
                    val image = if (data.revelationType == RevelationType.meccan) {
                        Res.drawable.dr_makkah_old
                    } else {
                        Res.drawable.dr_madina_old
                    }
                    ComposeResourceFiles.drawableStream(image)
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
