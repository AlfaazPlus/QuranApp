package com.quranapp.android.models

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * One HTML document for the visible tafsir, chapter info, or science page.
 * [reloadKey] changes when the document should load again. A font-size tweak
 * is not a new key; the screen applies that through [HtmlContentHandle].
 */
data class HtmlPage(
    val html: String,
    val baseUrl: String? = null,
    val reloadKey: Any,
    val mimeType: String = "text/html; charset=UTF-8",
    val encoding: String = "utf-8",
    val historyUrl: String? = null,
)

interface HtmlContentHandle {
    fun scrollToTop()
    fun evaluate(script: String)
}

interface HtmlContentBindings

expect fun htmlContentHandle(): HtmlContentHandle

@Composable
expect fun HtmlContentHost(
    page: HtmlPage?,
    bindings: HtmlContentBindings,
    handle: HtmlContentHandle,
    modifier: Modifier = Modifier,
    onBeforeLoad: () -> Unit = {},
)
