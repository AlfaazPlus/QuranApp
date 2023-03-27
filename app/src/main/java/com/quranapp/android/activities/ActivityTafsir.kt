package com.quranapp.android.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.quranapp.android.R
import com.quranapp.android.databinding.ActivityChapterInfoBinding
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.tafsir.TafsirJsInterface
import com.quranapp.android.utils.tafsir.TafsirUtils
import com.quranapp.android.utils.tafsir.TafsirWebViewClient
import com.quranapp.android.utils.univ.FileUtils
import com.quranapp.android.utils.univ.Keys
import com.quranapp.android.widgets.PageAlert
import java.io.File

class ActivityTafsir : ReaderPossessingActivity() {
    private lateinit var binding: ActivityChapterInfoBinding
    private lateinit var fileUtils: FileUtils
    private lateinit var pageAlert: PageAlert

    var tafsirKey: String? = null
    var chapterNo = 0
    var verseNo = 0

    override fun getLayoutResource(): Int {
        return R.layout.activity_chapter_info
    }

    override fun shouldInflateAsynchronously(): Boolean {
        return false
    }

    override fun preReaderReady(activityView: View, intent: Intent, savedInstanceState: Bundle?) {
        fileUtils = FileUtils.newInstance(this)
        binding = ActivityChapterInfoBinding.bind(activityView)
        pageAlert = PageAlert(this)
        initThis()
    }

    private fun initThis() {
        binding.let {
            it.title.setText(R.string.strTitleTafsir)
            it.back.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
            it.loader.visibility = View.VISIBLE
        }
    }

    override fun onReaderReady(intent: Intent, savedInstanceState: Bundle?) {
        initWebView()
        initContent(intent)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        binding.webView.let {
            it.setBackgroundColor(Color.TRANSPARENT)
            it.settings.apply {
                javaScriptEnabled = true
            }
            it.addJavascriptInterface(TafsirJsInterface(this), "TafsirJSInterface")
            it.overScrollMode = View.OVER_SCROLL_NEVER
            it.webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                    Log.d("[" + consoleMessage.lineNumber() + "]" + consoleMessage.message())
                    return true
                }
            }
            it.webViewClient = object : TafsirWebViewClient(this) {
                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    binding.loader.visibility = View.GONE
                }
            }
        }
    }


    private fun initContent(intent: Intent) {
        var key = intent.getStringExtra(TafsirUtils.KEY_TAFSIR)
        val chapterNo = intent.getIntExtra(Keys.READER_KEY_CHAPTER_NO, -1)
        val verseNo = intent.getIntExtra(Keys.READER_KEY_VERSE_NO, -1)

        if (chapterNo < 1 || verseNo < 1) {
            fail("Invalid params", false)
            return
        }

        if (key == null) {
            // key = TafsirUtils.TAFSIR_SLUG_TAFSIR_IBN_KATHIR_EN // fixme: get default key
        }

        this.tafsirKey = key
        this.chapterNo = chapterNo
        this.verseNo = verseNo

        loadContent()
    }

    private fun loadContent() {
        val tafsirFile: File = fileUtils.getTafsirFileSingleVerse(tafsirKey, chapterNo, verseNo)
        val urlStr: String? = TafsirUtils.prepareTafsirUrlSingleVerse(tafsirKey, chapterNo, verseNo)
    }


    private fun loadFailed(addMsg: String) {
        var msg = "Failed to load tafsir."
        if (!TextUtils.isEmpty(addMsg)) {
            msg += " $addMsg"
        }
        fail(msg, true)
    }

    private fun fail(msg: String, showRetry: Boolean) {
        binding.loader.visibility = View.GONE

        pageAlert.let {
            it.setMessage(msg, null)
            if (showRetry) {
                it.setActionButton(R.string.strLabelRetry) { loadContent() }
            } else {
                it.setActionButton(null, null)
            }
            it.show(binding.container)
        }

        deleteSavedFileIfExists()
    }

    private fun noInternet() {
        pageAlert.let {
            it.setupForNoInternet { loadContent() }
            it.show(binding.container)
        }
    }

    private fun deleteSavedFileIfExists() {
        if (tafsirKey == null) {
            return
        }

        val tafsirFile = fileUtils.getTafsirFileSingleVerse(tafsirKey, chapterNo, verseNo)

        tafsirFile.delete()
    }

}