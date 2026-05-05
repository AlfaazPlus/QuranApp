package com.quranapp.android.compose.screens.science

import android.content.Context
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.quranapp.android.R
import com.quranapp.android.components.quran.QuranScienceItem
import com.quranapp.android.compose.components.common.AppBar
import com.quranapp.android.compose.components.reader.dialogs.QuickReference
import com.quranapp.android.compose.components.reader.dialogs.QuickReferenceData
import com.quranapp.android.compose.components.reader.dialogs.QuickReferenceVerses
import com.quranapp.android.compose.theme.toCssVariables
import com.quranapp.android.compose.utils.ThemeUtils
import com.quranapp.android.compose.utils.appFallbackLanguageCodes
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.Logger
import com.quranapp.android.utils.quranScience.QuranScienceWebViewClient
import com.quranapp.android.utils.reader.QuranVerseWebItem
import com.quranapp.android.utils.reader.QuranVerseWebRequest
import com.quranapp.android.utils.reader.buildQuranVerseWebAssets
import com.quranapp.android.utils.reader.factory.ReaderFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

@Composable
fun ScienceContentScreen(
    item: QuranScienceItem?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var quickRefData by remember { mutableStateOf<QuickReferenceData?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            AppBar(title = stringResource(R.string.quran_and_science))
        }
    ) {
        Box(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
                .background(colorScheme.background)
        ) {
            ScienceContentWebView(
                item = item,
                onOpenReference = { chapterNo, fromVerse, toVerse ->
                    quickRefData = QuickReferenceData(
                        slugs = ReaderPreferences.getTranslations(),
                        chapterNo = chapterNo,
                        parsedVerses = QuickReferenceVerses.Range(
                            chapterNo = chapterNo,
                            range = fromVerse..toVerse,
                        ),
                    )
                },
            )
        }
    }

    QuickReference(
        data = quickRefData,
        onOpenInReader = { chapterNo, range ->
            quickRefData = null

            ReaderFactory.startVerseRange(
                context = context,
                chapterNo = chapterNo,
                fromVerse = range.first,
                toVerse = range.last,
            )
        },
        onClose = { quickRefData = null },
    )
}

@Composable
private fun ScienceContentWebView(
    item: QuranScienceItem?,
    onOpenReference: (Int, Int, Int) -> Unit,
) {
    val context = LocalContext.current
    val atlasAyahImageCache = remember { ConcurrentHashMap<String, ByteArray>() }
    var webViewRef by remember(item) { mutableStateOf<WebView?>(null) }

    val colors = MaterialTheme.colorScheme
    val scriptCode = ReaderPreferences.observeQuranScript()
    val arabicEnabled = ReaderPreferences.observeArabicTextEnabled()

    LaunchedEffect(item, colors, scriptCode, arabicEnabled, webViewRef) {
        val renderedHtml = withContext(Dispatchers.IO) {
            item?.let {
                renderScienceDocument(
                    context = context,
                    item = it,
                    atlasAyahImageCache = atlasAyahImageCache,
                    colorScheme = colors,
                    scriptCode = scriptCode,
                    arabicEnabled = arabicEnabled,
                )
            }
        }

        val webView = webViewRef

        if (webView != null && !renderedHtml.isNullOrEmpty()) {
            webView.loadDataWithBaseURL(
                null,
                renderedHtml,
                "text/html; charset=UTF-8",
                "utf-8",
                null,
            )
        }
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                val settings = this.settings
                settings.javaScriptEnabled = true
                settings.useWideViewPort = true

                overScrollMode = View.OVER_SCROLL_NEVER
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                        val msg = "[${consoleMessage.lineNumber()}] ${consoleMessage.message()}"
                        Log.d(msg)
                        Logger.logMsg(msg)
                        return true
                    }
                }
            }
        },
        update = { webView ->
            webViewRef = webView
            webView.webViewClient = QuranScienceWebViewClient(
                atlasPngCache = atlasAyahImageCache,
                onOpenReference = onOpenReference,
            )
        },
        modifier = Modifier
            .fillMaxSize(),
    )
}

private suspend fun renderScienceDocument(
    context: Context,
    item: QuranScienceItem,
    atlasAyahImageCache: ConcurrentHashMap<String, ByteArray>,
    colorScheme: ColorScheme,
    scriptCode: String,
    arabicEnabled: Boolean,
): String {
    val repository = DatabaseProvider.getQuranRepository(context)

    val rootVarsInner =
        colorScheme.toCssVariables() + "--tafsir-ar-size-mult:1;--tafsir-tr-size-mult:1;"

    val base = context.assets.open("science/base.html").bufferedReader().use { it.readText() }
        .replace("{{THEME}}", if (ThemeUtils.isDarkTheme(context)) "dark" else "light")
        .replace("{{ROOT_VARS}}", rootVarsInner)

    val inputStream = appFallbackLanguageCodes().firstNotNullOfOrNull { code ->
        runCatching {
            context.assets.open("science/topics/$code/${item.path}")
        }.getOrNull()
    }

    atlasAyahImageCache.clear()

    var document = inputStream?.bufferedReader().use { it?.readText() ?: "" }
    document = base.replace("{{CONTENT}}", document)

    val regexAr = Regex("\\{\\{REF_AR=(\\d+):(\\d+)\\}\\}")
    val verseRefs = regexAr.findAll(document).map {
        it.groupValues[1].toInt() to it.groupValues[2].toInt()
    }.distinct().toList()

    val verses = buildList {
        for ((chapterNo, verseNo) in verseRefs) {
            val vwd = repository.getVerseWithDetails(
                chapterNo,
                verseNo,
                scriptCode,
                arabicEnabled,
            ) ?: continue

            add(QuranVerseWebItem(details = vwd))
        }
    }

    val verseWeb = buildQuranVerseWebAssets(
        QuranVerseWebRequest(
            context = context,
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

    return document.replace("{{EXTRA_HEAD}}", "<style>${verseWeb.css}</style>")
}
