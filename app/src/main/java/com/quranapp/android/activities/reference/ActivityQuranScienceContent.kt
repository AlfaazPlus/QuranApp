package com.quranapp.android.activities.reference


import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.toArgb
import com.quranapp.android.R
import com.quranapp.android.activities.base.BaseActivity
import com.quranapp.android.components.quran.QuranScienceItem
import com.quranapp.android.compose.components.QuickReferenceHost
import com.quranapp.android.compose.theme.toCssVariables
import com.quranapp.android.compose.utils.ThemeUtils
import com.quranapp.android.compose.utils.appPlatformLocale
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.databinding.ActivityChapterInfoBinding
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.utils.Log.d
import com.quranapp.android.utils.Logger
import com.quranapp.android.utils.extensions.serializableExtra
import com.quranapp.android.utils.quranScience.QuranScienceWebViewClient
import com.quranapp.android.utils.reader.QuranVerseWebItem
import com.quranapp.android.utils.reader.QuranVerseWebRequest
import com.quranapp.android.utils.reader.buildQuranVerseWebAssets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class ActivityQuranScienceContent : BaseActivity() {
    private lateinit var binding: ActivityChapterInfoBinding
    private lateinit var quickRefHost: QuickReferenceHost
    private lateinit var colorScheme: ColorScheme

    private val atlasAyahImageCache = ConcurrentHashMap<String, ByteArray>()

    override fun getLayoutResource() = R.layout.activity_chapter_info

    override fun shouldInflateAsynchronously() = false

    override fun getStatusBarBG(): Int {
        return colorScheme.surface.toArgb()
    }

    override fun getNavBarBG(): Int {
        return colorScheme.surface.toArgb()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.webView.destroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        colorScheme = ThemeUtils.colorSchemeFromPreferences(this)

        super.onCreate(savedInstanceState)
    }

    override fun onActivityInflated(activityView: View, savedInstanceState: Bundle?) {
        binding = ActivityChapterInfoBinding.bind(activityView)
        binding.back.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.title.text = getString(R.string.quran_and_science)

        quickRefHost = QuickReferenceHost(this, binding.composeQuickReference)
        binding.header.setBackgroundColor(colorScheme.surface.toArgb())

        showLoader()

        setupWebView(binding.webView)

        CoroutineScope(Dispatchers.IO).launch {
            renderData(intent.serializableExtra<QuranScienceItem>("item")!!)
        }
    }

    fun showQuickReference(chapterNo: Int, fromVerse: Int, toVerse: Int) {
        quickRefHost.show(chapterNo, fromVerse, toVerse)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(webView: WebView) {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.useWideViewPort = true
        webView.overScrollMode = View.OVER_SCROLL_NEVER
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                val msg = "[" + consoleMessage.lineNumber() + "] " + consoleMessage.message()
                d(msg)
                Logger.logMsg(msg)
                return true
            }
        }
        webView.webViewClient = object : QuranScienceWebViewClient(this, atlasAyahImageCache) {
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


    private suspend fun renderData(item: QuranScienceItem) {
        val repository = DatabaseProvider.getQuranRepository(this)

        val rootVarsInner = colorScheme.toCssVariables() +
            "--tafsir-ar-size-mult:1;--tafsir-tr-size-mult:1;"

        val base = assets.open("science/base.html").bufferedReader().use { it.readText() }
            .replace("{{THEME}}", if (ThemeUtils.isDarkTheme(this)) "dark" else "light")
            .replace("{{ROOT_VARS}}", rootVarsInner)


        val fallbackLangCode = "en"
        val locale = appPlatformLocale()
        val currentLangCode = with(locale.language) {
            if (this == "in") "id" else this // Hosted weblate uses "id" for Indonesian but Android uses "in"
        }
        val currentCountry = locale.country

        val fullPath0 = "science/topics/$currentLangCode-r$currentCountry"
        val fullPath1 = "science/topics/$currentLangCode"
        val fallbackPath = "science/topics/$fallbackLangCode"

        val inputStream = listOf(
            "$fullPath0/${item.path}",
            "$fullPath1/${item.path}",
            "$fallbackPath/${item.path}"
        ).firstNotNullOfOrNull { path ->
            runCatching { assets.open(path) }.getOrNull()
        }

        val scriptCode = ReaderPreferences.getQuranScript()
        val arabicEnabled = ReaderPreferences.getArabicTextEnabled()

        atlasAyahImageCache.clear()

        var document = inputStream?.bufferedReader().use { it?.readText() ?: "" }
        document = base.replace("{{CONTENT}}", document)

        val regexAr = Regex("\\{\\{REF_AR=(\\d+):(\\d+)\\}\\}")

        val verseMatches = regexAr.findAll(document).toList()

        val verses = verseMatches.map {
            it.groupValues[1].toInt() to it.groupValues[2].toInt()
        }.distinct()
            .mapNotNull { (chapterNo, verseNo) ->
                val vwd = repository.getVerseWithDetails(
                    chapterNo, verseNo, scriptCode, arabicEnabled
                ) ?: return@mapNotNull null

                QuranVerseWebItem(
                    details = vwd,
                )
            }

        val verseWeb = buildQuranVerseWebAssets(
            QuranVerseWebRequest(
                context = this,
                colorScheme = colorScheme,
                repository = repository,
                scriptCode = scriptCode,
                atlasAyahImageCache = atlasAyahImageCache,
                includeOpenInReaderLink = true,
                verses = verses,
            ),
        )

        document = regexAr.replace(document) { matchResult ->
            val chapterNo = matchResult.groupValues[1].toInt()
            val verseNo = matchResult.groupValues[2].toInt()

            verseWeb.verseCardHtmlByRef[chapterNo to verseNo].orEmpty()
        }

        document = document.replace("{{EXTRA_HEAD}}", "<style>${verseWeb.css}</style>")

        runOnUiThread {
            binding.webView.loadDataWithBaseURL(
                null,
                document,
                "text/html; charset=UTF-8",
                "utf-8",
                null
            )
        }
    }

}

