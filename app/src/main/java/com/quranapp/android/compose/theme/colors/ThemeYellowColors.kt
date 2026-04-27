package com.alfaazplus.sunnah.ui.theme.colors

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

class ThemeYellowColors : BaseColors() {
    override fun lightColors(): ColorScheme {
        return lightColorScheme(
            primary = Color(0xFF6B4F3A),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFE7D4BE),
            onPrimaryContainer = Color(0xFF25180F),

            secondary = Color(0xFF7A6250),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFF0E0D0),
            onSecondaryContainer = Color(0xFF2B1D13),

            tertiary = Color(0xFF5F6B52),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFDCE5D2),
            onTertiaryContainer = Color(0xFF1A2115),

            error = Color(0xFFBA1A1A),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002),

            background = Color(0xFFF4ECE3),
            onBackground = Color(0xFF2A211A),

            surface = Color(0xFFFFF8F1),
            onSurface = Color(0xFF2A211A),

            surfaceVariant = Color(0xFFE2D4C5),
            onSurfaceVariant = Color(0xFF54463A),

            inverseOnSurface = Color(0xFFFDF6EE),
            inverseSurface = Color(0xFF362C24),
            inversePrimary = Color(0xFFD1B08A),

            surfaceContainer = Color(0xFFF8F1E8),
            surfaceContainerLow = Color(0xFFFFF8F1),
        )
    }

    override fun darkColors(): ColorScheme {
        return darkColorScheme(
            primary = Color(0xFFD1B08A),
            onPrimary = Color(0xFF3A2818),
            primaryContainer = Color(0xFF523B29),
            onPrimaryContainer = Color(0xFFE7D4BE),

            secondary = Color(0xFFC8AE96),
            onSecondary = Color(0xFF3D2B1F),
            secondaryContainer = Color(0xFF574234),
            onSecondaryContainer = Color(0xFFF0E0D0),

            tertiary = Color(0xFFBEC9B1),
            onTertiary = Color(0xFF283021),
            tertiaryContainer = Color(0xFF414B38),
            onTertiaryContainer = Color(0xFFDCE5D2),

            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),

            background = Color(0xFF1F1813),
            onBackground = Color(0xFFE9DED2),

            surface = Color(0xFF2A211A),
            onSurface = Color(0xFFE9DED2),

            surfaceVariant = Color(0xFF54463A),
            onSurfaceVariant = Color(0xFFD8C7B7),

            inverseOnSurface = Color(0xFF2A211A),
            inverseSurface = Color(0xFFE9DED2),
            inversePrimary = Color(0xFF6B4F3A),

            surfaceContainer = Color(0xFF2F251E),
            surfaceContainerLow = Color(0xFF2A211A),
        )
    }
}