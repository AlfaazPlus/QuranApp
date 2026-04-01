package com.quranapp.android.compose.components.player

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.quranapp.android.compose.theme.QuranAppTheme

/**
 * Creates a pluggable player layer that can be attached in non-Compose screens.
 * Add this view where needed (root layout, fragment container, etc.).
 */
fun createRecitationPlayerComposeView(
    context: Context,
): ComposeView {
    return ComposeView(context).apply {
        layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )

        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            QuranAppTheme {
                RecitationPlayerSheet()
            }
        }
    }
}