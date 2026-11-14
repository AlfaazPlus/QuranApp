package com.quranapp.android.compose.components.player


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
import com.quranapp.android.compose.utils.ThemeUtils

@Composable
fun Background(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isMono = ThemeUtils.observeIsMonoPalette()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PlayerBgColor)
            .then(modifier)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        PlayerBgColor,
                        if (isMono) Color(0xFF101010) else Color(0xFF101020),
                        PlayerBgColor
                    )
                )
            )

            val glow = if (isMono) Color.White else null

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        (glow ?: Color(0xFF7C4DFF)).copy(alpha = if (isMono) 0.08f else 0.28f),
                        Color.Transparent
                    )
                ),
                radius = w * 0.45f,
                center = Offset(w * 0.18f, h * 0.22f)
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        (glow ?: Color(0xFFFF4FC3)).copy(alpha = if (isMono) 0.06f else 0.22f),
                        Color.Transparent
                    )
                ),
                radius = w * 0.42f,
                center = Offset(w * 0.82f, h * 0.28f)
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        (glow ?: Color(0xFF00D1FF)).copy(alpha = if (isMono) 0.05f else 0.18f),
                        Color.Transparent
                    )
                ),
                radius = w * 0.50f,
                center = Offset(w * 0.52f, h * 0.78f)
            )

            // Focus light behind album art area
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.06f),
                        Color.Transparent
                    )
                ),
                radius = w * 0.30f,
                center = Offset(w * 0.5f, h * 0.38f)
            )

            // Subtle vignette
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        PlayerBgColor.copy(alpha = 0.38f)
                    ),
                    radius = w * 0.9f,
                    center = Offset(w / 2f, h / 2f)
                )
            )

            // Bottom darkness for controls readability
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        PlayerBgColor.copy(alpha = 0.18f),
                        PlayerBgColor.copy(alpha = 0.45f),
                        PlayerBgColor.copy(alpha = 0.72f)
                    ),
                    startY = h * 0.45f,
                    endY = h
                )
            )
        }

        content()
    }
}