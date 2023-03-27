package com.quranapp.android.utils.tafsir

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.core.graphics.drawable.DrawableCompat
import androidx.webkit.WebViewClientCompat
import com.peacedesign.android.utils.WindowUtils
import com.quranapp.android.R
import com.quranapp.android.activities.ActivityTafsir
import com.quranapp.android.utils.extensions.drawable
import com.quranapp.android.utils.univ.ResUtils.getBitmapInputStream
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.net.URLConnection
import java.util.*


open class TafsirWebViewClient(private val activity: ActivityTafsir) : WebViewClientCompat() {
    private val isDarkTheme = WindowUtils.isNightMode(activity)

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
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
        val uriStr = uri.toString().lowercase(Locale.getDefault())
        when (host) {
            "assets-file" -> data = ctx.assets.open(uri.toString().substring("https://assets-file/".length))
            "assets-font" -> {
                if (uriStr.contains("uthmani")) {
                    data = ctx.resources.openRawResource(+R.font.uthmanic_hafs)
                } else if (uriStr.contains("indopak")) {
                    data = ctx.resources.openRawResource(+R.font.indopak)
                } else if (uriStr.contains("urdu")) {
                    data = view.context.resources.openRawResource(+R.font.font_urdu)
                }
            }
            "assets-image" -> {
                if (uriStr.contains("left-arrow")) {
                    data = createArrowDrawableStream(activity, 0f)
                } else if (uriStr.contains("right-arrow")) {
                    data = createArrowDrawableStream(activity, 180f)
                } else if (uriStr.contains("top-arrow")) {
                    data = createArrowDrawableStream(activity, 90f)
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

    private fun createArrowDrawableStream(context: Context, rotate: Float): InputStream {
        val drawable = DrawableCompat.wrap(context.drawable(R.drawable.dr_icon_arrow_left)).mutate()
        drawable.setTint(if (isDarkTheme) Color.parseColor("#BBBBBB") else Color.BLACK)

        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth, drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)

        if (rotate != 0f) {
            canvas.rotate(rotate, (drawable.intrinsicWidth shr 1).toFloat(), (drawable.intrinsicHeight shr 1).toFloat())
        }

        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return getBitmapInputStream(bitmap)
    }


    override fun onPageFinished(view: WebView, url: String) {
        try {
            installContents(view)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    @Throws(JSONException::class)
    private fun installContents(webView: WebView) {
        val tafsirSlug = activity.tafsirKey!!
        val chapterNo = activity.chapterNo
        val verseNo = activity.verseNo
        val chapter = activity.mQuranRef.get().getChapter(chapterNo)

        val contentJson = JSONObject().apply {
            put("tafsir-title", TafsirUtils.getTafsirName(tafsirSlug))
            put(
                "verse-info-title",
                activity.str(R.string.strLabelVerseWithChapNameWithBar, chapter.name, verseNo)
            )
            put("verse-preview", chapter.getVerse(verseNo).arabicText)
            put("previous-tafsir-title", preparePrevVerseTitle(verseNo))
            put("next-tafsir-title", prepareNextVerseTitle(verseNo, chapter.verseCount))
        }.toString()

        webView.loadUrl("javascript:installContents('$contentJson')")
    }

    private fun preparePrevVerseTitle(verseNo: Int): String {
        return if (verseNo == 1) "" else activity.str(R.string.strLabelVerseNo, verseNo - 1)
    }

    private fun prepareNextVerseTitle(verseNo: Int, totalVerseCount: Int): String {
        return if (verseNo == totalVerseCount) "" else activity.str(R.string.strLabelVerseNo, verseNo + 1)
    }
}