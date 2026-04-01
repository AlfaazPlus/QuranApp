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

@Composable
fun Background(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF09090F))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Deep base gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0A12),
                        Color(0xFF101020),
                        Color(0xFF050507)
                    )
                )
            )

            // Glow 1 - purple
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF7C4DFF).copy(alpha = 0.28f),
                        Color.Transparent
                    )
                ),
                radius = w * 0.45f,
                center = Offset(w * 0.18f, h * 0.22f)
            )

            // Glow 2 - pink
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFF4FC3).copy(alpha = 0.22f),
                        Color.Transparent
                    )
                ),
                radius = w * 0.42f,
                center = Offset(w * 0.82f, h * 0.28f)
            )

            // Glow 3 - cyan
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF00D1FF).copy(alpha = 0.18f),
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
                        Color.Black.copy(alpha = 0.38f)
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
                        Color.Black.copy(alpha = 0.18f),
                        Color.Black.copy(alpha = 0.45f),
                        Color.Black.copy(alpha = 0.72f)
                    ),
                    startY = h * 0.45f,
                    endY = h
                )
            )
        }

        content()
    }
}