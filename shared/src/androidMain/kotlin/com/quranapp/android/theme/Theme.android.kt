package com.quranapp.android.theme

import android.app.Activity
import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

fun ThemeUtils.colorSchemeFromPreferences(
    context: Context,
    isDark: Boolean? = null,
    themeColor: String = getThemeColor(),
    isDynamicColor: Boolean = getDynamicColor(),
): ColorScheme {
    val dark = isDark ?: isDarkTheme()
    if (isDynamicColor && isDynamicColorSupported()) {
        return if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    return palette(dark, themeColor)
}

@Composable
fun ThemeUtils.observeColorScheme(
    context: Context,
    isDarkTheme: Boolean = observeDarkTheme(),
): ColorScheme {
    return colorSchemeFromPreferences(
        context,
        isDarkTheme,
        observeThemeColor(),
        observeIsDynamicColor(),
    )
}

@Composable
internal actual fun dynamicColorScheme(isDarkTheme: Boolean): ColorScheme? {
    if (!ThemeUtils.isDynamicColorSupported()) return null
    val context = LocalContext.current
    return if (isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}

@Composable
internal actual fun SyncSystemBars(isDarkTheme: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return

    SideEffect {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !isDarkTheme
            isAppearanceLightNavigationBars = !isDarkTheme
        }
    }
}
