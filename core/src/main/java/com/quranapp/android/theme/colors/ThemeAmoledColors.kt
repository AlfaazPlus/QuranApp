package com.quranapp.android.theme.colors

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

class ThemeAmoledColors : BaseColors() {
    override fun lightColors(): ColorScheme = ThemeDefaultColors().lightColors()

    override fun darkColors(): ColorScheme {
        return ThemeDefaultColors().darkColors().copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceVariant = Color(0xFF141414),
            surfaceDim = Color.Black,
            surfaceBright = Color(0xFF1A1A1A),
            surfaceTint = Color.Black,
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color.Black,
            surfaceContainer = Color(0xFF0A0A0A),
            surfaceContainerHigh = Color(0xFF121212),
            surfaceContainerHighest = Color(0xFF1A1A1A),
        )
    }
}
