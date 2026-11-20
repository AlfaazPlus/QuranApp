package com.quranapp.android.models

import android.annotation.SuppressLint
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.quranapp.android.html.AppWebViewClient

actual fun htmlContentHandle(): HtmlContentHandle = AndroidHtmlContentHandle()

class AndroidHtmlContentHandle : HtmlContentHandle {
    internal var webView: WebView? = null

    override fun scrollToTop() {
        webView?.scrollTo(0, 0)
    }

    override fun evaluate(script: String) {
        webView?.evaluateJavascript(script, null)
    }
}

class AndroidHtmlContentBindings(
    val client: WebViewClient,
    val chromeClient: WebChromeClient? = null,
    val javaScriptEnabled: Boolean = true,
    val allowFileAccess: Boolean = false,
    val domStorageEnabled: Boolean = false,
    val overScrollMode: Int = View.OVER_SCROLL_NEVER,
    val transparentBackground: Boolean = false,
    val onCreated: (WebView) -> Unit = {},
    val onUpdate: (WebView) -> Unit = {},
) : HtmlContentBindings

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun HtmlContentHost(
    page: HtmlPage?,
    bindings: HtmlContentBindings,
    handle: HtmlContentHandle,
    modifier: Modifier,
    onBeforeLoad: () -> Unit,
) {
    val androidHandle = handle as AndroidHtmlContentHandle
    val currentBindings by rememberUpdatedState(bindings as AndroidHtmlContentBindings)
    val currentPage by rememberUpdatedState(page)
    val currentBeforeLoad by rememberUpdatedState(onBeforeLoad)
    var generation by remember { mutableIntStateOf(0) }

    val recoverFromRenderProcessGone by rememberUpdatedState {
        androidHandle.webView = null
        generation++
    }

    DisposableEffect(currentBindings.client) {
        val client = currentBindings.client as? AppWebViewClient
        client?.onRenderProcessGoneAction = { recoverFromRenderProcessGone() }
        onDispose {
            client?.onRenderProcessGoneAction = null
        }
    }

    key(generation) {
        var loadedKey by remember { mutableStateOf<Any?>(null) }

        AndroidView(
            modifier = modifier,
            factory = { context ->
                WebView(context).apply {
                    val setup = currentBindings

                    if (setup.transparentBackground) {
                        setBackgroundColor(0x00000000)
                    }

                    settings.apply {
                        javaScriptEnabled = setup.javaScriptEnabled
                        setSupportZoom(false)
                        builtInZoomControls = false
                        displayZoomControls = false
                        useWideViewPort = false
                        allowFileAccess = setup.allowFileAccess
                        allowUniversalAccessFromFileURLs = setup.allowFileAccess
                        domStorageEnabled = setup.domStorageEnabled
                    }

                    overScrollMode = setup.overScrollMode
                    webViewClient = setup.client
                    setup.chromeClient?.let { webChromeClient = it }

                    androidHandle.webView = this
                    setup.onCreated(this)
                }
            },
            update = { webView ->
                currentBindings.onUpdate(webView)

                val next = currentPage ?: return@AndroidView
                if (loadedKey == next.reloadKey) return@AndroidView

                loadedKey = next.reloadKey
                currentBeforeLoad()
                webView.loadDataWithBaseURL(
                    next.baseUrl,
                    next.html,
                    next.mimeType,
                    next.encoding,
                    next.historyUrl,
                )
            },
            onRelease = { webView ->
                if (androidHandle.webView === webView) {
                    androidHandle.webView = null
                    webView.destroy()
                }
            },
        )
    }
}
