package com.quranapp.android.components.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.quranapp.android.theme.ThemeUtils
import com.quranapp.android.theme.observeColorScheme
import kotlin.math.max

@Composable
fun Background(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val scheme = ThemeUtils.observeColorScheme(context, isDarkTheme = true)
    val background = scheme.background

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .then(modifier)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        scheme.surface,
                        background,
                        background,
                    )
                )
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        scheme.primary.copy(alpha = 0.14f),
                        scheme.primary.copy(alpha = 0.04f),
                        Color.Transparent,
                    )
                ),
                radius = max(w, h) * 0.72f,
                center = Offset(w * 0.5f, h * 0.12f),
            )

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        background.copy(alpha = 0.55f),
                    ),
                    center = Offset(w * 0.5f, h * 0.42f),
                    radius = max(w, h) * 0.85f,
                )
            )
        }

        content()
    }
}
