package com.alfaazplus.sunnah.ui.theme.colors

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

class ThemeYellowColors : BaseColors() {
    override fun lightColors(): ColorScheme {
        return lightColorScheme(
            primary = Color(0xFF745B00),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFFFE08D),
            onPrimaryContainer = Color(0xFF241A00),
            secondary = Color(0xFF695D3F),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFF2E1BB),
            onSecondaryContainer = Color(0xFF231B04),
            tertiary = Color(0xFF47664A),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFC9ECC8),
            onTertiaryContainer = Color(0xFF04210B),
            error = Color(0xFFBA1A1A),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002),
            background = Color(0xFFF0F0F0),
            onBackground = Color(0xFF1E1B16),
            surface = Color(0xFFFFFBFF),
            onSurface = Color(0xFF1E1B16),
            surfaceVariant = Color(0xFFEBE1CF),
            onSurfaceVariant = Color(0xFF4C4639),
            inverseOnSurface = Color(0xFFF7F0E7),
            inverseSurface = Color(0xFF33302A),
            inversePrimary = Color(0xFFEBC248),
            surfaceContainer = Color(0xFF1E1B16),
            surfaceContainerLow = Color(0xFF1E1B16),
        )
    }

    override fun darkColors(): ColorScheme {
        return darkColorScheme(
            primary = Color(0xFFEBC248),
            onPrimary = Color(0xFF3D2F00),
            primaryContainer = Color(0xFF584400),
            onPrimaryContainer = Color(0xFFFFE08D),
            secondary = Color(0xFFD5C5A1),
            onSecondary = Color(0xFF392F15),
            secondaryContainer = Color(0xFF50462A),
            onSecondaryContainer = Color(0xFFF2E1BB),
            tertiary = Color(0xFFADCFAD),
            onTertiary = Color(0xFF19361F),
            tertiaryContainer = Color(0xFF304D34),
            onTertiaryContainer = Color(0xFFC9ECC8),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
            background = Color(0xFF191712),
            onBackground = Color(0xFFE8E1D9),
            surface = Color(0xFF2B261F),
            onSurface = Color(0xFFE8E1D9),
            surfaceVariant = Color(0xFF4C4639),
            onSurfaceVariant = Color(0xFFCFC5B4),
            inverseOnSurface = Color(0xFF1E1B16),
            inverseSurface = Color(0xFFE8E1D9),
            inversePrimary = Color(0xFF745B00),
            surfaceContainer = Color(0xFF2B261F),
            surfaceContainerLow = Color(0xFF2B261F),
        )
    }
}