package com.alfaazplus.sunnah.ui.theme.colors

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

class ThemeMonoColors : BaseColors() {
    override fun lightColors(): ColorScheme {
        return lightColorScheme(
            primary = Color(0xFF616161),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFDCDCDC),
            onPrimaryContainer = Color(0xFF272727),
            secondary = Color(0xFF797979),
            onSecondary = Color(0xFFB39494),
            secondaryContainer = Color(0xFFDCDCDC),
            onSecondaryContainer = Color(0xFF272727),
            tertiary = Color(0xFFDCDCDC),
            onTertiary = Color(0xFF272727),
            tertiaryContainer = Color(0xFFDCDCDC),
            onTertiaryContainer = Color(0xFF272727),
            error = Color(0xFFDCDCDC),
            errorContainer = Color(0xFFDCDCDC),
            onError = Color(0xFF272727),
            onErrorContainer = Color(0xFF272727),
            background = Color(0xFFF0F0F0),
            onBackground = Color(0xFF272727),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF272727),
            surfaceVariant = Color(0xFFDCDCDC),
            onSurfaceVariant = Color(0xFF353535),
            inverseOnSurface = Color(0xFFDCDCDC),
            inverseSurface = Color(0xFF606060),
            inversePrimary = Color(0xFFC1C1C1),
            surfaceContainer = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFFFFFFF),
        )
    }

    override fun darkColors(): ColorScheme {
        return darkColorScheme(
            primary = Color(0xFF959595),
            onPrimary = Color(0xFF161616),
            primaryContainer = Color(0xFF4D4D4D),
            onPrimaryContainer = Color(0xFFA3A3A3),
            secondary = Color(0xFFA3A3A3),
            onSecondary = Color(0xFF886A6A),
            secondaryContainer = Color(0xFF4D4D4D),
            onSecondaryContainer = Color(0xFFCCCCCC),
            tertiary = Color(0xFF4D4D4D),
            onTertiary = Color(0xFFA3A3A3),
            tertiaryContainer = Color(0xFF4D4D4D),
            onTertiaryContainer = Color(0xFFA3A3A3),
            error = Color(0xFF4D4D4D),
            errorContainer = Color(0xFF4D4D4D),
            onError = Color(0xFFA3A3A3),
            onErrorContainer = Color(0xFFA3A3A3),
            background = Color(0xFF101010),
            onBackground = Color(0xFFA3A3A3),
            surface = Color(0xFF202020),
            onSurface = Color(0xFFBDBDBD),
            surfaceVariant = Color(0xFF4D4D4D),
            onSurfaceVariant = Color(0xFFA3A3A3),
            inverseOnSurface = Color(0xFF404040),
            inverseSurface = Color(0xFFC3C3C3),
            inversePrimary = Color(0xFF707070),
            surfaceContainer = Color(0xFF101010),
            surfaceContainerLow = Color(0xFF101010),
        )
    }
}