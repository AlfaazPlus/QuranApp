package com.quranapp.android.screens.chapterInfo

import android.app.Activity
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranapp.android.components.common.AppBar
import com.quranapp.android.components.reader.dialogs.QuickReference
import com.quranapp.android.components.reader.dialogs.QuickReferenceData
import com.quranapp.android.components.reader.dialogs.QuickReferenceVerses
import com.quranapp.android.components.reader.navigator.ChapterVerseNavigator
import com.quranapp.android.extensions.appViewModel
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.dr_icon_chevron_down
import com.quranapp.android.generated.resources.dr_icon_info
import com.quranapp.android.generated.resources.strLabelRetry
import com.quranapp.android.generated.resources.strLabelSurah
import com.quranapp.android.generated.resources.strMsgNoInternet
import com.quranapp.android.generated.resources.strMsgNoInternetLong
import com.quranapp.android.generated.resources.strTitleAboutSurah
import com.quranapp.android.locale.LocalAppLocale
import com.quranapp.android.models.AndroidHtmlContentBindings
import com.quranapp.android.models.ChapterInfoContentData
import com.quranapp.android.models.HtmlContentHost
import com.quranapp.android.models.HtmlPage
import com.quranapp.android.models.htmlContentHandle
import com.quranapp.android.preferences.ReaderPreferences
import com.quranapp.android.utils.app.Logger
import com.quranapp.android.utils.chapterInfo.ChapterInfoJSInterface
import com.quranapp.android.utils.chapterInfo.ChapterInfoWebViewClient
import com.quranapp.android.utils.reader.factory.ReaderFactory
import com.quranapp.android.viewmodel.ChapterInfoContentState
import com.quranapp.android.viewmodel.ChapterInfoEvent
import com.quranapp.android.viewmodel.ChapterInfoUiState
import com.quranapp.android.viewmodel.ChapterInfoViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.quranapp.android.navigation.AppRoute
import com.quranapp.android.platform.PlatformServices

@Composable
fun ChapterInfoScreen(
    initialChapterNo: Int,
    initialLanguage: String?,
    modifier: Modifier = Modifier,
) {
    val viewModel = appViewModel<ChapterInfoViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val onEvent = viewModel::onEvent
    val context = LocalContext.current

    LaunchedEffect(initialChapterNo, initialLanguage) {
        onEvent(ChapterInfoEvent.Init(context, initialChapterNo, initialLanguage))
    }

    var quickRefData by remember { mutableStateOf<QuickReferenceData?>(null) }
    var showChapterNavigator by rememberSaveable { mutableStateOf(false) }
    val translationSlugs = ReaderPreferences.observeTranslations()

    Scaffold(
        modifier = modifier,
        topBar = {
            ChapterInfoTopBar(
                uiState = uiState,
                onOpenChapterNavigator = { showChapterNavigator = true },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val contentState = uiState.contentState) {
                is ChapterInfoContentState.Loading -> LoadingContent()

                is ChapterInfoContentState.Success -> {
                    ChapterInfoWebViewContent(
                        uiState = uiState,
                        contentState = contentState,
                        onOpenReference = { chapterNo, fromVerse, toVerse ->
                            quickRefData = QuickReferenceData(
                                slugs = translationSlugs,
                                chapterNo = chapterNo,
                                parsedVerses = QuickReferenceVerses.Range(
                                    chapterNo,
                                    fromVerse..toVerse
                                ),
                            )
                        },
                    )
                }

                is ChapterInfoContentState.Error -> {
                    ErrorContent(
                        message = contentState.message,
                        canRetry = contentState.canRetry,
                        onRetry = { onEvent(ChapterInfoEvent.Retry(context)) },
                    )
                }

                is ChapterInfoContentState.NoInternet -> {
                    NoInternetContent(
                        onRetry = { onEvent(ChapterInfoEvent.Retry(context)) },
                    )
                }

                is ChapterInfoContentState.InvalidParams -> {
                    InvalidParamsContent()
                }
            }
        }
    }

    ChapterVerseNavigator(
        isOpen = showChapterNavigator,
        onDismiss = { showChapterNavigator = false },
        selectedChapterNo = uiState.chapterNo.takeIf { it >= 1 },
        selectedVerseNos = emptySet(),
        onChapterSelected = { chapterNo ->
            onEvent(ChapterInfoEvent.Init(context, chapterNo, initialLanguage))
        },
    )

    QuickReference(
        data = quickRefData,
        onOpenInReader = { chapterNo, range ->
            quickRefData = null
            PlatformServices.navigator.navigate(AppRoute.Reader(ReaderFactory.verseRange(chapterNo, range.first, range.last)))
        },
        onClose = { quickRefData = null },
    )
}

@Composable
private fun ChapterInfoTopBar(
    uiState: ChapterInfoUiState,
    onOpenChapterNavigator: () -> Unit,
) {
    val appLocale = LocalAppLocale.current
    val chapterName = uiState.swl?.let { it.getCurrentName() }.orEmpty()
    val chapterNo = uiState.chapterNo

    AppBar(
        title = stringResource(Res.string.strTitleAboutSurah),
        actions = {
            TextButton(onOpenChapterNavigator) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (chapterNo >= 1) {
                            if (chapterName.isNotEmpty()) {
                                appLocale.format(
                                    $$"%1$d. %2$s",
                                    chapterNo,
                                    chapterName,
                                )
                            } else {
                                appLocale.format("%d", chapterNo)
                            }
                        } else {
                            ""
                        },
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )

                    Icon(
                        painter = painterResource(Res.drawable.dr_icon_chevron_down),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    )
}

@Composable
private fun ChapterInfoWebViewContent(
    uiState: ChapterInfoUiState,
    contentState: ChapterInfoContentState.Success,
    onOpenReference: (Int, Int, Int) -> Unit,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val appLocale = LocalAppLocale.current
    val chapterTitle = stringResource(
        Res.string.strLabelSurah,
        uiState.swl?.getCurrentName().orEmpty(),
    )

    val contentData =
        remember(
            uiState.chapterNo,
            uiState.swl,
            uiState.juzNos,
            contentState.langCode,
            appLocale,
            chapterTitle,
        ) {
            val meta = uiState.swl ?: return@remember null
            ChapterInfoContentData(
                uiState.chapterNo,
                chapterTitle,
                contentState.langCode,
                meta.surah.ayahCount,
                meta.surah.rukusCount,
                meta.surah.revelationOrder,
                meta.surah.revelationType,
                uiState.juzNos,
            )
        } ?: return

    var isLoading by remember { mutableStateOf(true) }
    val webClient = remember {
        ChapterInfoWebViewClient(contentData) { isLoading = false }
    }
    val handle = remember { htmlContentHandle() }
    val chromeClient = remember {
        object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                val msg = "[${consoleMessage.lineNumber()}] ${consoleMessage.message()}"
                Logger.d(msg)
                return true
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HtmlContentHost(
            page = HtmlPage(
                html = contentState.html,
                reloadKey = contentState.html,
            ),
            bindings = AndroidHtmlContentBindings(
                client = webClient,
                chromeClient = chromeClient,
                allowFileAccess = true,
                domStorageEnabled = true,
                transparentBackground = true,
                onCreated = { webView ->
                    webView.addJavascriptInterface(
                        ChapterInfoJSInterface(
                            contentData.verseCount,
                            onOpenReference,
                        ),
                        "ChapterInfoJSInterface",
                    )
                },
                onUpdate = {
                    webClient.update(contentData) { isLoading = false }
                },
            ),
            handle = handle,
            modifier = Modifier.fillMaxSize(),
            onBeforeLoad = {
                isLoading = false
            },
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
                painter = painterResource(Res.drawable.dr_icon_info),
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
                        text = stringResource(Res.string.strLabelRetry),
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
                painter = painterResource(Res.drawable.dr_icon_info),
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(Res.string.strMsgNoInternet),
                style = MaterialTheme.typography.titleMedium,
                color = colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.strMsgNoInternetLong),
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            TextButton(onClick = onRetry) {
                Text(
                    text = stringResource(Res.string.strLabelRetry),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun InvalidParamsContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                painter = painterResource(Res.drawable.dr_icon_info),
                contentDescription = null,
                tint = colorScheme.error,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Invalid params",
                style = MaterialTheme.typography.bodyLarge,
                color = colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}
