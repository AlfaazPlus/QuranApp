package com.quranapp.android.screens.science

import android.content.Context
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
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
import com.quranapp.android.components.common.AppBar
import com.quranapp.android.components.reader.dialogs.QuickReference
import com.quranapp.android.components.reader.dialogs.QuickReferenceData
import com.quranapp.android.components.reader.dialogs.QuickReferenceVerses
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.quran_and_science
import com.quranapp.android.locale.AppLocale
import com.quranapp.android.locale.LocalAppLocale
import com.quranapp.android.locale.appFallbackLanguageCodes
import com.quranapp.android.models.AndroidHtmlContentBindings
import com.quranapp.android.models.HtmlContentHost
import com.quranapp.android.models.HtmlPage
import com.quranapp.android.models.QuranScienceItem
import com.quranapp.android.models.htmlContentHandle
import com.quranapp.android.navigation.AppRoute
import com.quranapp.android.platform.Platform
import com.quranapp.android.preferences.ReaderPreferences
import com.quranapp.android.theme.ThemeUtils
import com.quranapp.android.theme.toCssVariables
import com.quranapp.android.utils.app.Logger
import com.quranapp.android.utils.quranScience.QuranScienceWebViewClient
import com.quranapp.android.utils.reader.QuranVerseWebItem
import com.quranapp.android.utils.reader.QuranVerseWebRequest
import com.quranapp.android.utils.reader.buildQuranVerseWebAssets
import com.quranapp.android.utils.reader.factory.ReaderFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import java.util.concurrent.ConcurrentHashMap

@Composable
fun ScienceContentScreen(
    item: QuranScienceItem?,
    modifier: Modifier = Modifier,
) {
    var quickRefData by remember { mutableStateOf<QuickReferenceData?>(null) }
    val translationSlugs = ReaderPreferences.observeTranslations()

    Scaffold(
        modifier = modifier,
        topBar = {
            AppBar(title = stringResource(Res.string.quran_and_science))
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
                        slugs = translationSlugs,
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

            Platform.navigator.navigate(
                AppRoute.Reader(
                    ReaderFactory.verseRange(chapterNo, range.first, range.last)
                )
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
    var renderedHtml by remember(item) { mutableStateOf<String?>(null) }
    val handle = remember(item) { htmlContentHandle() }
    val webClient = remember {
        QuranScienceWebViewClient(
            atlasPngCache = atlasAyahImageCache,
            quranArabicScriptCode = "",
            onOpenReference = { _, _, _ -> },
        )
    }
    val chromeClient = remember {
        object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                val msg = "[${consoleMessage.lineNumber()}] ${consoleMessage.message()}"
                Logger.d(msg)
                return true
            }
        }
    }

    val locale = LocalAppLocale.current
    val colors = MaterialTheme.colorScheme
    val scriptCode = ReaderPreferences.observeQuranScript()
    val arabicEnabled = ReaderPreferences.observeArabicTextEnabled()

    LaunchedEffect(item, locale, colors, scriptCode, arabicEnabled) {
        renderedHtml = withContext(Dispatchers.IO) {
            item?.let {
                renderScienceDocument(
                    context = context,
                    locale = locale,
                    item = it,
                    atlasAyahImageCache = atlasAyahImageCache,
                    colorScheme = colors,
                    scriptCode = scriptCode,
                    arabicEnabled = arabicEnabled,
                )
            }
        }
    }

    val page = renderedHtml?.takeIf { it.isNotEmpty() }?.let { html ->
        HtmlPage(
            html = html,
            reloadKey = html,
        )
    }

    HtmlContentHost(
        page = page,
        bindings = AndroidHtmlContentBindings(
            client = webClient,
            chromeClient = chromeClient,
            javaScriptEnabled = true,
            onUpdate = {
                webClient.update(
                    quranArabicScriptCode = scriptCode,
                    onOpenReference = onOpenReference,
                )
            },
        ),
        handle = handle,
        modifier = Modifier.fillMaxSize(),
    )
}

private suspend fun renderScienceDocument(
    context: Context,
    item: QuranScienceItem,
    atlasAyahImageCache: ConcurrentHashMap<String, ByteArray>,
    colorScheme: ColorScheme,
    scriptCode: String,
    arabicEnabled: Boolean,
    locale: AppLocale,
): String {
    val repository = DatabaseProvider.getQuranRepository()

    val rootVarsInner =
        colorScheme.toCssVariables() + "--tafsir-ar-size-mult:1;--tafsir-tr-size-mult:1;"

    val base = Platform.assets.readAssetText("science/base.html")
        .replace("{{THEME}}", if (ThemeUtils.isDarkTheme()) "dark" else "light")
        .replace("{{ROOT_VARS}}", rootVarsInner)

    val topicHtml = appFallbackLanguageCodes().firstNotNullOfOrNull { code ->
        Platform.assets.readAssetTextOrNull("science/topics/$code/${item.path}")
    }.orEmpty()

    atlasAyahImageCache.clear()

    var document = topicHtml
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
            locale = locale,
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
