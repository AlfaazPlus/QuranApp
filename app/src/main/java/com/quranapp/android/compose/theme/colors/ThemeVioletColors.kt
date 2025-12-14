package com.alfaazplus.sunnah.ui.theme.colors

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

class ThemeVioletColors : BaseColors() {
    override fun lightColors(): ColorScheme {
        return lightColorScheme(
            primary = Color(0xFF714AAA),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFEDDCFF),
            onPrimaryContainer = Color(0xFF290055),
            secondary = Color(0xFF714AAA),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFEDDCFF),
            onSecondaryContainer = Color(0xFF290055),
            tertiary = Color(0xFF714AAA),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFEDDCFF),
            onTertiaryContainer = Color(0xFF290055),
            error = Color(0xFFBA1A1A),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002),
            background = Color(0xFFF0F0F0),
            onBackground = Color(0xFF2B0052),
            surface = Color(0xFFFFFBFF),
            onSurface = Color(0xFF2B0052),
            surfaceVariant = Color(0xFFE8E0EB),
            onSurfaceVariant = Color(0xFF4A454E),
            inverseOnSurface = Color(0xFFF9ECFF),
            inverseSurface = Color(0xFF421A6C),
            inversePrimary = Color(0xFFD7BAFF),
            surfaceContainer = Color(0xFFFFFBFF),
            surfaceContainerLow = Color(0xFFFFFBFF),
        )
    }

    override fun darkColors(): ColorScheme {
        return darkColorScheme(
            primary = Color(0xFFD7BAFF),
            onPrimary = Color(0xFF411478),
            primaryContainer = Color(0xFF593090),
            onPrimaryContainer = Color(0xFFEDDCFF),
            secondary = Color(0xFFD7BAFF),
            onSecondary = Color(0xFF411478),
            secondaryContainer = Color(0xFF593090),
            onSecondaryContainer = Color(0xFFEDDCFF),
            tertiary = Color(0xFFD7BAFF),
            onTertiary = Color(0xFF411478),
            tertiaryContainer = Color(0xFF593090),
            onTertiaryContainer = Color(0xFFEDDCFF),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
            background = Color(0xFF17111D),
            onBackground = Color(0xFFEFDBFF),
            surface = Color(0xFF2D2036),
            onSurface = Color(0xFFEFDBFF),
            surfaceVariant = Color(0xFF4A454E),
            onSurfaceVariant = Color(0xFFCCC4CF),
            inverseOnSurface = Color(0xFF2B0052),
            inverseSurface = Color(0xFFEFDBFF),
            inversePrimary = Color(0xFF714AAA),
            surfaceContainer = Color(0xFF2D2036),
            surfaceContainerLow = Color(0xFF2D2036),
        )
    }
}