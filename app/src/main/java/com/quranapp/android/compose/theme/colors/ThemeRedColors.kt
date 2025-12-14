package com.alfaazplus.sunnah.ui.theme.colors

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

class ThemeRedColors : BaseColors() {
    override fun lightColors(): ColorScheme {
        return lightColorScheme(
            primary = Color(0xFF9F4034),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFFFDAD5),
            onPrimaryContainer = Color(0xFF410000),
            secondary = Color(0xFF775652),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFFFDAD5),
            onSecondaryContainer = Color(0xFF2C1512),
            tertiary = Color(0xFF705C2E),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFFCDFA6),
            onTertiaryContainer = Color(0xFF251A00),
            error = Color(0xFFBA1A1A),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002),
            background = Color(0xFFF0F0F0),
            onBackground = Color(0xFF201A19),
            surface = Color(0xFFFFFBFF),
            onSurface = Color(0xFF201A19),
            surfaceVariant = Color(0xFFF5DDDA),
            onSurfaceVariant = Color(0xFF534341),
            inverseOnSurface = Color(0xFFFBEEEC),
            inverseSurface = Color(0xFF362F2E),
            inversePrimary = Color(0xFFFFB4A9),
            surfaceContainer = Color(0xFFFFFBFF),
            surfaceContainerLow = Color(0xFFFFFBFF),
        )
    }

    override fun darkColors(): ColorScheme {
        return darkColorScheme(
            primary = Color(0xFFFFB4A9),
            onPrimary = Color(0xFF61120C),
            primaryContainer = Color(0xFF7F291F),
            onPrimaryContainer = Color(0xFFFFDAD5),
            secondary = Color(0xFFE7BDB6),
            onSecondary = Color(0xFF442925),
            secondaryContainer = Color(0xFF5D3F3B),
            onSecondaryContainer = Color(0xFFFFDAD5),
            tertiary = Color(0xFFDEC38C),
            onTertiary = Color(0xFF3E2E04),
            tertiaryContainer = Color(0xFF574419),
            onTertiaryContainer = Color(0xFFFCDFA6),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
            background = Color(0xFF161110),
            onBackground = Color(0xFFEDE0DE),
            surface = Color(0xFF2B211F),
            onSurface = Color(0xFFEDE0DE),
            surfaceVariant = Color(0xFF534341),
            onSurfaceVariant = Color(0xFFD8C2BE),
            inverseOnSurface = Color(0xFF201A19),
            inverseSurface = Color(0xFFEDE0DE),
            inversePrimary = Color(0xFF9F4034),
            surfaceContainer = Color(0xFF2B211F),
            surfaceContainerLow = Color(0xFF2B211F),
        )
    }
}