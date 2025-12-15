package com.alfaazplus.sunnah.ui.theme.colors

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

class ThemePurpleColors : BaseColors() {
    override fun lightColors(): ColorScheme {
        return lightColorScheme(
            primary = Color(0xFF9B3489),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFFFD7F0),
            onPrimaryContainer = Color(0xFF3A0032),
            secondary = Color(0xFF6F5767),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFF9DAEC),
            onSecondaryContainer = Color(0xFF281623),
            tertiary = Color(0xFF815341),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFFFDBCE),
            onTertiaryContainer = Color(0xFF321205),
            error = Color(0xFFBA1A1A),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002),
            background = Color(0xFFF0F0F0),
            onBackground = Color(0xFF1F1A1D),
            surface = Color(0xFFFFFBFF),
            onSurface = Color(0xFF1F1A1D),
            surfaceVariant = Color(0xFFEFDEE6),
            onSurfaceVariant = Color(0xFF4F444A),
            inverseOnSurface = Color(0xFFF8EEF1),
            inverseSurface = Color(0xFF342F32),
            inversePrimary = Color(0xFFFFACE7),
            surfaceContainer = Color(0xFFFFFBFF),
            surfaceContainerLow = Color(0xFFFFFBFF),
        )
    }

    override fun darkColors(): ColorScheme {
        return darkColorScheme(
            primary = Color(0xFFFFACE7),
            onPrimary = Color(0xFF5E0052),
            primaryContainer = Color(0xFF7E186F),
            onPrimaryContainer = Color(0xFFFFD7F0),
            secondary = Color(0xFFDCBED0),
            onSecondary = Color(0xFF3E2A38),
            secondaryContainer = Color(0xFF56404F),
            onSecondaryContainer = Color(0xFFF9DAEC),
            tertiary = Color(0xFFF5B9A2),
            onTertiary = Color(0xFF4B2617),
            tertiaryContainer = Color(0xFF663C2B),
            onTertiaryContainer = Color(0xFFFFDBCE),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
            background = Color(0xFF110D0F),
            onBackground = Color(0xFFEAE0E3),
            surface = Color(0xFF292226),
            onSurface = Color(0xFFEAE0E3),
            surfaceVariant = Color(0xFF4F444A),
            onSurfaceVariant = Color(0xFFD2C2CA),
            inverseOnSurface = Color(0xFF1F1A1D),
            inverseSurface = Color(0xFFEAE0E3),
            inversePrimary = Color(0xFF9B3489),
            surfaceContainer = Color(0xFF292226),
            surfaceContainerLow = Color(0xFF292226),
        )
    }
}