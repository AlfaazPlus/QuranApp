package com.alfaazplus.sunnah.ui.theme.colors

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

class ThemeDefaultColors : BaseColors() {
    override fun lightColors(): ColorScheme {
        return lightColorScheme(
            primary = Color(0xFF008B5B),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFF007444),
            onPrimaryContainer = Color(0xFFFFFFFF),
            secondary = Color(0xFF0094A7),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFF006774),
            onSecondaryContainer = Color(0xFFFFFFFF),
            tertiary = Color(0xFF9c3e00),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFF6D2C01),
            onTertiaryContainer = Color(0xFFFFFFFF),
            error = Color(0xFFBA1A1A),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002),
            background = Color(0xFFF0F0F0),
            onBackground = Color(0xFF000000),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF1B1B1F),
            surfaceVariant = Color(0xFFE1E2EC),
            onSurfaceVariant = Color(0xFF44464F),
            inverseOnSurface = Color(0xFFF2F0F4),
            inverseSurface = Color(0xFF303034),
            inversePrimary = Color(0xFFB0C6FF),
            surfaceContainer = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFFFFFFF),
        )
    }

    override fun darkColors(): ColorScheme {
        return darkColorScheme(
            primary = Color(0xFF008B5B),
            onPrimary = Color(0xFFE0E0E0),
            primaryContainer = Color(0xFF007444),
            onPrimaryContainer = Color(0xFFE0E0E0),
            secondary = Color(0xFF0094A7),
            onSecondary = Color(0xFFE0E0E0),
            secondaryContainer = Color(0xFF006774),
            onSecondaryContainer = Color(0xFFE0E0E0),
            tertiary = Color(0xFF9c3e00),
            onTertiary = Color(0xFFE0E0E0),
            tertiaryContainer = Color(0xFF6D2C01),
            onTertiaryContainer = Color(0xFFE0E0E0),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
            background = Color(0xFF101010),
            onBackground = Color(0xFFE0E0E0),
            surface = Color(0xFF202020),
            onSurface = Color(0xFFE0E0E0),
            surfaceVariant = Color(0xFF303030),
            onSurfaceVariant = Color(0xFFE0E0E0),
            inverseOnSurface = Color(0xFFF2F0F4),
            inverseSurface = Color(0xFF303034),
            inversePrimary = Color(0xFFB0C6FF),
            surfaceContainerLow = Color(0xFF202020),
            surfaceContainer = Color(0xFF202020),
        )
    }
}