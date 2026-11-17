package com.quranapp.android.html

import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebView
import androidx.webkit.WebViewClientCompat

open class AppWebViewClient : WebViewClientCompat() {
    @Volatile
    var onRenderProcessGoneAction: (() -> Unit)? = null

    final override fun onRenderProcessGone(
        view: WebView,
        detail: RenderProcessGoneDetail,
    ): Boolean {
        (view.parent as? ViewGroup)?.removeView(view)
        view.destroy()
        onRenderProcessGoneAction?.invoke()
        return true
    }
}
