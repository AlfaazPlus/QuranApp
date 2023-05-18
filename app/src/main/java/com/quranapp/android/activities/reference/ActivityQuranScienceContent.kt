package com.quranapp.android.activities.reference

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.peacedesign.android.utils.WindowUtils
import com.quranapp.android.R
import com.quranapp.android.activities.ReaderPossessingActivity
import com.quranapp.android.components.quran.QuranScienceItem
import com.quranapp.android.databinding.ActivityChapterInfoBinding
import com.quranapp.android.utils.Log.d
import com.quranapp.android.utils.Logger
import com.quranapp.android.utils.extensions.serializableExtra
import com.quranapp.android.utils.quranScience.QuranScienceWebViewClient
import com.quranapp.android.utils.reader.TranslUtils
import com.quranapp.android.utils.reader.factory.QuranTranslationFactory
import com.quranapp.android.utils.univ.StringUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ActivityQuranScienceContent : ReaderPossessingActivity() {
    private lateinit var binding: ActivityChapterInfoBinding
    private lateinit var translFactory: QuranTranslationFactory
    lateinit var slugs: Set<String>

    override fun getLayoutResource() = R.layout.activity_chapter_info

    override fun shouldInflateAsynchronously() = false

    override fun onDestroy() {
        super.onDestroy()
        translFactory.close()
        binding.webView.destroy()
    }

    override fun initCreate(savedInstanceState: Bundle?) {
        translFactory = QuranTranslationFactory(this)
        slugs = resolveTranslationSlugs()

        super.initCreate(savedInstanceState)
    }

    override fun preReaderReady(activityView: View, intent: Intent, savedInstanceState: Bundle?) {
        binding = ActivityChapterInfoBinding.bind(activityView)

        binding.back.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val title = "Quran & Science"
        binding.title.text = title

        showLoader()

        setupWebView(binding.webView)
    }

    override fun onReaderReady(intent: Intent, savedInstanceState: Bundle?) {
        CoroutineScope(Dispatchers.IO).launch {
            renderData(intent.serializableExtra<QuranScienceItem>("item")!!)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(webView: WebView) {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        webView.overScrollMode = View.OVER_SCROLL_NEVER
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                val msg = "[" + consoleMessage.lineNumber() + "] " + consoleMessage.message()
                d(msg)
                Logger.logMsg(msg)
                return true
            }
        }
        webView.webViewClient = object : QuranScienceWebViewClient(this) {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                hideLoader()
            }
        }
    }

    fun showLoader() {
        binding.loader.visibility = View.VISIBLE
    }

    fun hideLoader() {
        binding.loader.visibility = View.GONE
    }


    private fun renderData(item: QuranScienceItem) {
        val base = assets.open("science/base.html").bufferedReader().use { it.readText() }
            .replace("{{THEME}}", if (WindowUtils.isNightMode(this)) "dark" else "light")

        var document = assets.open("science/topics/${item.path}").bufferedReader().use { it.readText() }
        document = base.replace("{{CONTENT}}", document)

        val regexAr = Regex("\\{\\{REF_AR=(\\d+):(\\d+)\\}\\}")
        val regexTr = Regex("\\{\\{REF_TR=(\\d+):(\\d+)\\}\\}")
        val regexName = Regex("\\{\\{REF_NAME=(\\d+):(\\d+)\\}\\}")

        val quranMeta = mQuranMetaRef.get()!!
        val quran = mQuranRef.get()

        val isKFQPC = mVerseDecorator.isKFQPCScript()
        val fontPageNos = HashSet<Int>()

        document = regexAr.replace(document) { matchResult ->
            val chapterNo = matchResult.groupValues[1]
            val verseNo = matchResult.groupValues[2]

            val verse = quran.getVerse(chapterNo.toInt(), verseNo.toInt())
            val quranText = if (isKFQPC) verse.arabicText else TextUtils.concat(verse.arabicText, " ", verse.endText)

            if (isKFQPC) {
                fontPageNos.add(verse.pageNo)
            }

            if (isKFQPC) {
                "<p class='arabic' style='font-family:page_${verse.pageNo}'>$quranText</p>"
            } else {
                "<p class='arabic' style='font-family:quran-arabic'>$quranText</p>"
            }
        }

        var fontStyles = "";
        if (isKFQPC) {
            fontPageNos.forEach {
                fontStyles += "@font-face { font-family: page_$it; src: url('https://assets-font/quran-arabic/page_$it'); }"
            }
        } else {
            fontStyles = "@font-face { font-family: quran-arabic; src: url('https://assets-font/quran-arabic'); }"
        }
        document = document.replace("{{STYLE}}", fontStyles)

        document = regexTr.replace(document) { matchResult ->
            val chapterNo = matchResult.groupValues[1]
            val verse = matchResult.groupValues[2]

            StringUtils.removeHTML(
                translFactory.getTranslationsSingleVerse(
                    slugs,
                    chapterNo.toInt(),
                    verse.toInt()
                )[0].text, false
            )
        }

        document = regexName.replace(document) { matchResult ->
            val chapterNo = matchResult.groupValues[1]
            val verse = matchResult.groupValues[2]

            "${quranMeta.getChapterName(this, chapterNo.toInt(), "en", false)} $chapterNo:$verse"
        }

        runOnUiThread {
            binding.webView.loadDataWithBaseURL(null, document, "text/html; charset=UTF-8", "utf-8", null)
        }
    }

    private fun resolveTranslationSlugs(): Set<String> {
        val slugs = mutableSetOf<String>()
        val bookInfos = translFactory.getAvailableTranslationBooksInfo()

        for (bookInfo in bookInfos) {
            if (bookInfo.key.contains("yusuf")) {
                slugs.add(bookInfo.key)
                break
            }
        }

        if (slugs.isEmpty()) {
            slugs.add(TranslUtils.TRANSL_SLUG_EN_THE_CLEAR_QURAN)
        }

        return slugs
    }

}