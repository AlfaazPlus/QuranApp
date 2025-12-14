package com.alfaazplus.sunnah.ui.theme.colors

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

class ThemeBlueColors : BaseColors() {
    override fun lightColors(): ColorScheme {
        return lightColorScheme(
            primary = Color(0xFF0058CB),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFD9E2FF),
            onPrimaryContainer = Color(0xFF001945),
            secondary = Color(0xFF575E71),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFDCE2F9),
            onSecondaryContainer = Color(0xFF141B2C),
            tertiary = Color(0xFF725572),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFFDD7FA),
            onTertiaryContainer = Color(0xFF2A132C),
            error = Color(0xFFBA1A1A),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002),
            background = Color(0xFFF0F0F0),
            onBackground = Color(0xFF1B1B1F),
            surface = Color(0xFFFEFBFF),
            onSurface = Color(0xFF1B1B1F),
            surfaceVariant = Color(0xFFE1E2EC),
            onSurfaceVariant = Color(0xFF44464F),
            inverseOnSurface = Color(0xFFF2F0F4),
            inverseSurface = Color(0xFF303034),
            inversePrimary = Color(0xFFB0C6FF),
            surfaceContainer = Color(0xFFFEFBFF),
            surfaceContainerLow = Color(0xFFFEFBFF),
        )
    }

    override fun darkColors(): ColorScheme {
        return darkColorScheme(
            primary = Color(0xFFB0C6FF),
            onPrimary = Color(0xFF002D6F),
            primaryContainer = Color(0xFF00429B),
            onPrimaryContainer = Color(0xFFD9E2FF),
            secondary = Color(0xFFC0C6DC),
            onSecondary = Color(0xFF293042),
            secondaryContainer = Color(0xFF404659),
            onSecondaryContainer = Color(0xFFDCE2F9),
            tertiary = Color(0xFFE0BBDD),
            onTertiary = Color(0xFF412742),
            tertiaryContainer = Color(0xFF593D5A),
            onTertiaryContainer = Color(0xFFFDD7FA),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
            background = Color(0xFF101016),
            onBackground = Color(0xFFE3E2E6),
            surface = Color(0xFF22222A),
            onSurface = Color(0xFFE3E2E6),
            surfaceVariant = Color(0xFF44464F),
            onSurfaceVariant = Color(0xFFC5C6D0),
            inverseOnSurface = Color(0xFF1B1B1F),
            inverseSurface = Color(0xFFE3E2E6),
            inversePrimary = Color(0xFF0058CB),
            surfaceContainer = Color(0xFF22222A),
            surfaceContainerLow = Color(0xFF22222A),
        )
    }
}