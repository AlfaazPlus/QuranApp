package com.quranapp.android.compose.screens.tafsir

import ThemeUtils
import android.content.Context
import android.content.Intent
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.peacedesign.android.utils.AppBridge
import com.quranapp.android.R
import com.quranapp.android.activities.ActivitySettings
import com.quranapp.android.api.models.tafsir.TafsirModel
import com.quranapp.android.compose.components.common.AppBar
import com.quranapp.android.compose.components.reader.navigator.ChapterVerseNavigator
import com.quranapp.android.compose.navigation.SettingRoutes
import com.quranapp.android.utils.quran.QuranMeta
import com.quranapp.android.utils.tafsir.TafsirUtils
import com.quranapp.android.utils.tafsir.TafsirWebViewClient
import com.quranapp.android.utils.univ.Keys
import com.quranapp.android.utils.univ.ResUtils
import com.quranapp.android.utils.univ.StringUtils.isRtlLanguage
import com.quranapp.android.viewModels.TafsirContentState
import com.quranapp.android.viewModels.TafsirReaderEvent
import com.quranapp.android.viewModels.TafsirReaderUiState
import com.quranapp.android.viewModels.TafsirReaderViewModel
import java.util.Locale

@Composable
fun TafsirReaderScreen(
    showFontSizeDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = viewModel<TafsirReaderViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var webViewScrollY by remember { mutableIntStateOf(0) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var showChapterVerseNavigator by remember { mutableStateOf(false) }

    val onEvent = viewModel::onEvent
    val context = LocalContext.current

    Scaffold(
        modifier = modifier,
        topBar = {
            TafsirTopBar(
                uiState = uiState,
                onOpenChapterVerseNavigator = { showChapterVerseNavigator = true },
                onShare = when (val s = uiState.contentState) {
                    is TafsirContentState.Success -> {
                        { shareTafsir(context, uiState, s.tafsir) }
                    }

                    else -> null
                },
            )
        },
        bottomBar = {
            TafsirBottomNavigation(
                uiState = uiState,
                onEvent = onEvent,
            )
        },
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnimatedVisibility(
                    visible = webViewScrollY > 400,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it }
                ) {
                    FloatingActionButton(
                        onClick = { webViewRef?.scrollTo(0, 0) },
                        containerColor = colorScheme.primaryContainer,
                        contentColor = colorScheme.onPrimaryContainer,
                        elevation = FloatingActionButtonDefaults.elevation(2.dp),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.dr_icon_chevron_left),
                            contentDescription = stringResource(R.string.strLabelTop),
                            modifier = Modifier
                                .size(24.dp)
                                .rotate(90f)
                        )
                    }
                }

                FloatingActionButton(
                    onClick = showFontSizeDialog,
                    containerColor = colorScheme.primaryContainer,
                    contentColor = colorScheme.onPrimaryContainer,
                    elevation = FloatingActionButtonDefaults.elevation(2.dp),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.icon_font_size),
                        contentDescription = stringResource(R.string.titleReaderTextSizeTafsir),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val contentState = uiState.contentState) {
                is TafsirContentState.Loading -> LoadingContent()

                is TafsirContentState.Success -> {
                    TafsirWebViewContent(
                        text = contentState.tafsir.text,
                        verses = contentState.tafsir.verses,
                        tafsirKey = uiState.tafsirKey,
                        textSizeMultiplier = uiState.textSizeMultiplier,
                        langCode = uiState.tafsirInfo?.langCode,
                        onWebViewCreated = { webViewRef = it },
                        onScrollChanged = { scrollY -> webViewScrollY = scrollY },
                    )
                }

                is TafsirContentState.Error -> {
                    ErrorContent(
                        message = contentState.message,
                        canRetry = contentState.canRetry,
                        onRetry = { onEvent(TafsirReaderEvent.Retry) }
                    )
                }

                is TafsirContentState.NoInternet -> {
                    NoInternetContent(
                        onRetry = { onEvent(TafsirReaderEvent.Retry) }
                    )
                }
            }
        }
    }

    val initialVerseNos = remember(uiState.chapterNo, uiState.verseNo, uiState.contentState) {
        when (val c = uiState.contentState) {
            is TafsirContentState.Success -> {
                TafsirUtils.tafsirVerseRangeInChapter(c.tafsir, uiState.chapterNo)
                    ?.let { range -> (range.first..range.last).toSet() }
                    ?: setOf(uiState.verseNo)
            }

            else -> setOf(uiState.verseNo)
        }
    }

    ChapterVerseNavigator(
        isOpen = showChapterVerseNavigator,
        onDismiss = { showChapterVerseNavigator = false },
        selectedChapterNo = uiState.chapterNo.takeIf { it >= 1 },
        selectedVerseNos = initialVerseNos,
        onVerseSelected = { chapterNo, verseNo ->
            viewModel.onEvent(
                TafsirReaderEvent.Init(
                    uiState.tafsirKey.ifBlank { null },
                    chapterNo,
                    verseNo,
                )
            )
        },
    )
}

@Composable
private fun TafsirTopBar(
    uiState: TafsirReaderUiState,
    onOpenChapterVerseNavigator: () -> Unit,
    onShare: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val chapterName = uiState.chapterMeta?.getCurrentName() ?: ""
    val chapterNo = uiState.chapterNo
    val verseNo = uiState.verseNo

    AppBar(
        titleContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenChapterVerseNavigator)
                    .padding(horizontal = 16.dp),
            ) {
                Text(
                    text = uiState.tafsirInfo?.name ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = String.format(
                            Locale.getDefault(),
                            $$"%1$s %2$d:%3$d",
                            chapterName,
                            chapterNo,
                            verseNo
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Icon(
                        painterResource(R.drawable.dr_icon_chevron_down),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        },
        actions = {
            if (onShare != null) {
                IconButton(onClick = onShare) {
                    Icon(
                        painter = painterResource(R.drawable.dr_icon_share),
                        contentDescription = stringResource(R.string.strLabelShare),
                        tint = colorScheme.onSurface,
                    )
                }
            }
            IconButton(
                onClick = {
                    val intent = Intent(context, ActivitySettings::class.java).apply {
                        putExtra(Keys.NAV_DESTINATION, SettingRoutes.TAFSIR)
                    }
                    context.startActivity(intent, null)
                },
            ) {
                Icon(
                    painter = painterResource(R.drawable.dr_icon_translations),
                    contentDescription = stringResource(R.string.strTitleSelectTafsir),
                    tint = colorScheme.onSurface,
                )
            }
        },
    )
}

@Composable
private fun TafsirWebViewContent(
    text: String,
    verses: List<String>,
    tafsirKey: String,
    textSizeMultiplier: Float,
    langCode: String?,
    onWebViewCreated: (WebView) -> Unit,
    onScrollChanged: (Int) -> Unit,
) {
    val context = LocalContext.current
    val isDarkTheme = ThemeUtils.observeDarkTheme()

    val htmlContent = remember(
        text,
        verses,
        textSizeMultiplier,
        isDarkTheme,
        langCode,
    ) {
        buildTafsirHtml(
            context = context,
            content = text,
            verses = verses,
            fontSizePercent = (textSizeMultiplier * 100).toInt(),
            isDark = isDarkTheme,
            langCode = langCode ?: "en"
        )
    }

    var isLoading by remember { mutableStateOf(true) }
    var lastLoad by remember { mutableStateOf<Pair<String, String>?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    setBackgroundColor(0x00000000)
                    settings.javaScriptEnabled = true
                    overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
                    webChromeClient = WebChromeClient()
                    setOnScrollChangeListener { _, _, scrollY, _, _ ->
                        onScrollChanged(scrollY)
                    }
                    onWebViewCreated(this)
                }
            },
            update = { webView ->
                webView.webViewClient = TafsirWebViewClient(
                    tafsirKey = tafsirKey,
                    onPageFinished = { isLoading = false }
                )
                val signature = htmlContent to tafsirKey

                if (lastLoad != signature) {
                    lastLoad = signature
                    isLoading = true
                    webView.loadDataWithBaseURL(
                        null,
                        htmlContent,
                        "text/html; charset=UTF-8",
                        "utf-8",
                        null
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp
                )
            }
        }
    }
}

private fun buildTafsirHtml(
    context: android.content.Context,
    content: String,
    verses: List<String>,
    fontSizePercent: Int,
    isDark: Boolean,
    langCode: String
): String {
    val theme = if (isDark) "dark" else "light"
    val direction = if (isRtlLanguage(langCode)) "rtl" else "ltr"

    val multiVerseAlert = if (verses.size > 1) {
        val alertMsg = context.getString(R.string.readingTafsirMultiVerses)
        val versesSorted = verses.sortedWith(
            compareBy(
                { it.substringBefore(':').toIntOrNull() ?: 0 },
                { it.substringAfter(':').toIntOrNull() ?: 0 }
            )
        )
        """
        <div class="multiple-verse-alert">
            <strong>$alertMsg</strong> ${versesSorted.joinToString(", ")}
        </div>
        """.trimIndent()
    } else ""

    val fullContent = multiVerseAlert + content

    val boilerplate = ResUtils.readAssetsTextFile(context, "tafsir/tafsir_page.html")

    val replacements = mapOf(
        "{{THEME}}" to theme,
        "{{CONTENT}}" to fullContent,
        "{{DIR}}" to direction,
        "{{LANG}}" to langCode,
        "{{FONT_SIZE}}" to fontSizePercent.toString()
    )

    val pattern = Regex(replacements.keys.joinToString("|") { Regex.escape(it) })
    return pattern.replace(boilerplate) { match -> replacements[match.value].orEmpty() }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = colorScheme.primary,
            modifier = Modifier.size(40.dp),
            strokeWidth = 3.dp
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.dr_icon_info),
                contentDescription = null,
                tint = colorScheme.error,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            if (canRetry) {
                Spacer(modifier = Modifier.height(20.dp))
                TextButton(onClick = onRetry) {
                    Text(
                        text = stringResource(R.string.strLabelRetry),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun NoInternetContent(onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.dr_icon_no_internet),
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.strMsgNoInternet),
                style = MaterialTheme.typography.titleMedium,
                color = colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.strMsgNoInternetLong),
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            TextButton(onClick = onRetry) {
                Text(
                    text = stringResource(R.string.strLabelRetry),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun TafsirBottomNavigation(
    uiState: TafsirReaderUiState,
    onEvent: (TafsirReaderEvent) -> Unit,
) {
    val totalVerses = uiState.chapterMeta?.surah?.ayahCount ?: 0
    val lastChapter = QuranMeta.chapterRange.last
    val range = when (val c = uiState.contentState) {
        is TafsirContentState.Success ->
            TafsirUtils.tafsirVerseRangeInChapter(c.tafsir, uiState.chapterNo)

        else -> null
    }

    val groupFirst = range?.first ?: uiState.verseNo
    val groupLast = range?.last ?: uiState.verseNo

    val hasPrevious = uiState.chapterNo > 1 || groupFirst > 1
    val hasNext = totalVerses > 0 &&
            (groupLast < totalVerses ||
                    (uiState.chapterNo < lastChapter && groupLast >= totalVerses))

    Surface(
        color = colorScheme.surfaceContainer,
        shadowElevation = 12.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavigationButton(
                onClick = { onEvent(TafsirReaderEvent.PreviousVerse) },
                enabled = hasPrevious,
                label = stringResource(R.string.labelPreviousTafsir),
                isNext = false,
                modifier = Modifier.weight(1f)
            )

            NavigationButton(
                onClick = { onEvent(TafsirReaderEvent.NextVerse) },
                enabled = hasNext,
                label = stringResource(R.string.labelNextTafsir),
                isNext = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun NavigationButton(
    onClick: () -> Unit,
    enabled: Boolean,
    label: String,
    isNext: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (enabled) {
        colorScheme.background
    } else {
        Color.Transparent
    }

    val contentColor = if (enabled) {
        colorScheme.primary
    } else {
        colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }

    Surface(
        color = backgroundColor,
        modifier = modifier
            .height(44.dp)
            .clip(
                RoundedCornerShape(12.dp)
            )
            .then(
                if (enabled) Modifier.clickable(onClick = onClick)
                else Modifier
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isNext) {
                Icon(
                    painter = painterResource(R.drawable.dr_icon_chevron_left),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )

            if (isNext) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    painter = painterResource(R.drawable.dr_icon_chevron_left),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(180f)
                )
            }
        }
    }
}


private fun shareTafsir(
    context: Context,
    uiState: TafsirReaderUiState,
    tafsir: TafsirModel,
) {
    val plain = HtmlCompat.fromHtml(tafsir.text, HtmlCompat.FROM_HTML_MODE_LEGACY)
        .toString()
        .replace('\u00A0', ' ')
        .trim()
    if (plain.isEmpty()) return

    val chapterName = uiState.chapterMeta?.getCurrentName() ?: ""
    val ref = "${uiState.chapterNo}:${uiState.verseNo}"
    val text = buildString {
        uiState.tafsirInfo?.name?.let { append(it).append('\n') }
        append(chapterName).append(' ').append(ref).append("\n\n")
        append(plain)
    }

    AppBridge.newSharer(context)
        .setData(text)
        .setChooserTitle(context.getString(R.string.strLabelShare))
        .setPlatform(AppBridge.Platform.SYSTEM_SHARE)
        .share()
}