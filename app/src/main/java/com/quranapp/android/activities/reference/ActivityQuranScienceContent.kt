package com.quranapp.android.activities.reference


import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.annotation.ColorInt
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.toArgb
import com.quranapp.android.R
import com.quranapp.android.activities.base.BaseActivity
import com.quranapp.android.components.quran.QuranScienceItem
import com.quranapp.android.compose.components.QuickReferenceHost
import com.quranapp.android.compose.utils.ThemeUtils
import com.quranapp.android.compose.utils.appLocale
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.databinding.ActivityChapterInfoBinding
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.utils.Log.d
import com.quranapp.android.utils.Logger
import com.quranapp.android.utils.extensions.serializableExtra
import com.quranapp.android.utils.quranScience.QuranScienceWebViewClient
import com.quranapp.android.utils.reader.factory.QuranTranslationFactory
import com.quranapp.android.utils.reader.isKFQPCScript
import com.quranapp.android.utils.univ.StringUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ActivityQuranScienceContent : BaseActivity() {
    private lateinit var binding: ActivityChapterInfoBinding
    private lateinit var translFactory: QuranTranslationFactory
    private lateinit var quickRefHost: QuickReferenceHost
    private lateinit var colorScheme: ColorScheme

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
        translFactory.close()
        binding.webView.destroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        colorScheme = ThemeUtils.colorSchemeFromPreferences(this)

        super.onCreate(savedInstanceState)
    }

    override fun initCreate(savedInstanceState: Bundle?) {
        translFactory = QuranTranslationFactory(this)

        super.initCreate(savedInstanceState)
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


    private suspend fun renderData(item: QuranScienceItem) {
        val repository = DatabaseProvider.getQuranRepository(this)

        val varMap = mapOf(
            "--primary" to colorIntToCssHex(colorScheme.primary.toArgb()),
            "--on-primary" to colorIntToCssHex(colorScheme.onPrimary.toArgb()),
            "--background" to colorIntToCssHex(colorScheme.surface.toArgb()),
            "--on-background" to colorIntToCssHex(colorScheme.onSurface.toArgb()),
        )

        val base = assets.open("science/base.html").bufferedReader().use { it.readText() }
            .replace("{{THEME}}", if (ThemeUtils.isDarkTheme(this)) "dark" else "light")
            .replaceFirst(
                "{{STYLE}}",
                "<style>:root{${varMap.entries.joinToString("") { "${it.key}:${it.value};" }}}</style>",
            )


        val fallbackLangCode = "en"
        val locale = appLocale()
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
        var document = inputStream?.bufferedReader().use { it?.readText() ?: "" }
        document = base.replace("{{CONTENT}}", document)

        val regexAr = Regex("\\{\\{REF_AR=(\\d+):(\\d+)\\}\\}")
        val regexName = Regex("\\{\\{REF_NAME=(\\d+):(\\d+)\\}\\}")

        val verseMatches = regexAr.findAll(document).toList()
        val nameMatches = regexName.findAll(document).toList()


        val verseMap = verseMatches.map {
            it.groupValues[1].toInt() to it.groupValues[2].toInt()
        }.distinct().associateWith { (chapterNo, verseNo) ->
            repository.getVerseWithDetails(chapterNo, verseNo, scriptCode)
        }

        val referenceMap = nameMatches.map {
            it.groupValues[1].toInt() to it.groupValues[2].toInt()
        }.distinct().associateWith { (chapterNo, verse) ->
            String.format(
                appLocale(),
                $$"%1$s %2$d:%3$d",
                repository.getChapterName(chapterNo),
                chapterNo,
                verse
            )
        }

        val isKFQPC = scriptCode.isKFQPCScript()
        val fontPageNos = HashSet<Int>()

        document = regexAr.replace(document) { matchResult ->
            val chapterNo = matchResult.groupValues[1].toInt()
            val verseNo = matchResult.groupValues[2].toInt()

            val details = verseMap[chapterNo to verseNo] ?: return@replace ""

            val quranText = details.words.joinToString(" ") { it.text }

            if (isKFQPC) {
                fontPageNos.add(details.pageNo)
            }

            if (isKFQPC) {
                "<p class='arabic' style='font-family:page_${details.pageNo}'>$quranText</p>"
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
            fontStyles =
                "@font-face { font-family: quran-arabic; src: url('https://assets-font/quran-arabic'); }"
        }
        document = document.replace("{{STYLE}}", fontStyles)

        val regexTr = Regex("\\{\\{REF_TR=(\\d+):(\\d+)\\}\\}")
        document = regexTr.replace(document) { matchResult ->
            val chapterNo = matchResult.groupValues[1]
            val verse = matchResult.groupValues[2]

            val translationText = translFactory.getTranslationsSingleVerse(
                chapterNo.toInt(),
                verse.toInt()
            ).firstOrNull()?.text.orEmpty()

            StringUtils.removeHTML(translationText, false)
        }

        document = regexName.replace(document) { matchResult ->
            val chapterNo = matchResult.groupValues[1].toInt()
            val verse = matchResult.groupValues[2].toInt()

            referenceMap[chapterNo to verse] ?: ""
        }

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

    private fun colorIntToCssHex(@ColorInt color: Int): String =
        StringUtils.formatInvariant("#%06X", 0xFFFFFF and color)

}
