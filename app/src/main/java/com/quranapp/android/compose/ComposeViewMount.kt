package com.quranapp.android.compose

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.quranapp.android.compose.theme.QuranAppTheme

fun createComposeView(
    context: Context,
    content: @Composable () -> Unit,
): ComposeView {
    return ComposeView(context).apply {
        layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )

        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            QuranAppTheme {
                content()
            }
        }
    }
}

fun ViewGroup.attachComposeView(
    content: @Composable () -> Unit,
): ComposeView {
    return createComposeView(context, content).also { addView(it) }
}
