package com.quranapp.android.compose.screens.tafsir

import ThemeUtilsV2
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quranapp.android.R
import com.quranapp.android.utils.tafsir.TafsirWebViewClient
import com.quranapp.android.utils.univ.ResUtils
import com.quranapp.android.utils.univ.StringUtils.isRtlLanguage
import com.quranapp.android.viewModels.TafsirContentState
import com.quranapp.android.viewModels.TafsirReaderEvent
import com.quranapp.android.viewModels.TafsirReaderViewModel

@Composable
fun TafsirReaderScreen(
    onBack: () -> Unit,
    showFontSizeDialog: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = viewModel<TafsirReaderViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    var webViewScrollY by remember { mutableIntStateOf(0) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TafsirTopBar(
                onBack = onBack,
                onOpenSettings = onOpenSettings
            )

            // Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (val contentState = uiState.contentState) {
                    is TafsirContentState.Loading -> {
                        LoadingContent()
                    }

                    is TafsirContentState.Success -> {
                        TafsirWebViewContent(
                            text = contentState.tafsir.text,
                            verses = contentState.tafsir.verses,
                            onWebViewCreated = { webViewRef = it },
                            onScrollChanged = { scrollY -> webViewScrollY = scrollY },
                            onGoToTop = { webViewRef?.scrollTo(0, 0) },
                        )
                    }

                    is TafsirContentState.Error -> {
                        ErrorContent(
                            message = contentState.message,
                            canRetry = contentState.canRetry,
                            onRetry = { viewModel.onEvent(TafsirReaderEvent.Retry) }
                        )
                    }

                    is TafsirContentState.NoInternet -> {
                        NoInternetContent(
                            onRetry = { viewModel.onEvent(TafsirReaderEvent.Retry) }
                        )
                    }
                }

                // FABs
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .navigationBarsPadding(),
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

            TafsirBottomNavigation()
        }
    }
}

@Composable
private fun TafsirTopBar(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val viewModel = viewModel<TafsirReaderViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    val chapterName = uiState.chapterMeta?.name ?: ""
    val chapterNo = uiState.chapterNo
    val verseNo = uiState.verseNo

    Surface(
        color = colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            ) {
                Icon(
                    painter = painterResource(R.drawable.dr_icon_chevron_left),
                    contentDescription = stringResource(R.string.strDescClose),
                    tint = colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Title
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
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
                Text(
                    text = "$chapterName $chapterNo:$verseNo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }

            // Settings button
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            ) {
                Icon(
                    painter = painterResource(R.drawable.dr_icon_translations),
                    contentDescription = stringResource(R.string.strTitleSelectTafsir),
                    tint = colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun TafsirWebViewContent(
    text: String,
    verses: List<String>,
    onWebViewCreated: (WebView) -> Unit,
    onScrollChanged: (Int) -> Unit,
    onGoToTop: () -> Unit,
) {
    val viewModel = viewModel<TafsirReaderViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    val context = LocalContext.current
    val isDarkTheme = ThemeUtilsV2.isDarkTheme()

    val htmlContent = remember(
        text,
        verses,
        uiState.textSizeMultiplier,
        isDarkTheme,
        uiState.tafsirInfo?.langCode
    ) {
        buildTafsirHtml(
            context = context,
            content = text,
            verses = verses,
            fontSizePercent = (uiState.textSizeMultiplier * 100).toInt(),
            isDark = isDarkTheme,
            langCode = uiState.tafsirInfo?.langCode ?: "en"
        )
    }

    var isLoading by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    setBackgroundColor(0x00000000)
                    settings.javaScriptEnabled = true
                    overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS

                    webChromeClient = WebChromeClient()
                    webViewClient = TafsirWebViewClient(
                        tafsirKey = uiState.tafsirKey,
                        onPageFinished = { isLoading = false }
                    )

                    setOnScrollChangeListener { _, _, scrollY, _, _ ->
                        onScrollChanged(scrollY)
                    }

                    onWebViewCreated(this)
                }
            },
            update = { webView ->
                webView.loadDataWithBaseURL(
                    null,
                    htmlContent,
                    "text/html; charset=UTF-8",
                    "utf-8",
                    null
                )
            },
            modifier = Modifier.fillMaxSize()
        )

        // Loading overlay
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
        """
        <div class="multiple-verse-alert">
            <strong>$alertMsg</strong> ${verses.joinToString(", ")}
        </div>
        """.trimIndent()
    } else ""

    val fullContent = multiVerseAlert + content

    val boilerplate = ResUtils.readAssetsTextFile(context, "tafsir/tafsir_page.html")

    val replacements = mapOf(
        "{{THEME}}" to theme,
        "{{CONTENT}}" to fullContent,
        "{{DIR}}" to direction,
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
                text = "Please check your connection and try again",
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
private fun TafsirBottomNavigation() {
    val viewModel = viewModel<TafsirReaderViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    val hasPrevious = uiState.verseNo > 1
    val totalVerses = uiState.chapterMeta?.verseCount ?: 0
    val verseNo = uiState.verseNo
    val hasNext = verseNo < totalVerses

    Surface(color = colorScheme.surface, shadowElevation = 5.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavigationButton(
                onClick = {
                    viewModel.onEvent(TafsirReaderEvent.PreviousVerse)
                },
                enabled = hasPrevious,
                label = stringResource(R.string.labelPreviousTafsir),
                isNext = false,
                modifier = Modifier.weight(1f)
            )


            NavigationButton(
                onClick = {
                    viewModel.onEvent(TafsirReaderEvent.NextVerse)
                },
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