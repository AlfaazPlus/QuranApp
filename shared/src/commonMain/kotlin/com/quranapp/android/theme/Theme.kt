package com.quranapp.android.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.quranapp.android.locale.LocalAppLocale
import com.quranapp.android.locale.appLocaleFlow
import com.quranapp.android.preferences.DataStoreManager

@Composable
fun QuranAppTheme(
    content: @Composable () -> Unit,
) {
    val isDarkTheme = ThemeUtils.observeDarkTheme()
    val themeColor = ThemeUtils.observeThemeColor()
    val dynamic = ThemeUtils.observeIsDynamicColor()
    val colorScheme = if (dynamic) {
        dynamicColorScheme(isDarkTheme) ?: ThemeUtils.palette(isDarkTheme, themeColor)
    } else {
        ThemeUtils.palette(isDarkTheme, themeColor)
    }
    val appLocale by appLocaleFlow.collectAsState()

    SyncSystemBars(isDarkTheme)

    CompositionLocalProvider(LocalAppLocale provides appLocale) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = getAppTypography(),
            content = content,
        )
    }
}

@Composable
fun ThemeUtils.observeThemeMode(): String =
    DataStoreManager.observe(ThemeUtils.KEY_THEME_MODE)

@Composable
fun ThemeUtils.observeThemeColor(): String =
    DataStoreManager.observe(ThemeUtils.KEY_THEME_COLOR)

@Composable
fun ThemeUtils.observeIsDynamicColor(): Boolean =
    DataStoreManager.observe(ThemeUtils.KEY_THEME_DYNAMIC_COLOR)

@Composable
fun ThemeUtils.observeDarkTheme(): Boolean {
    return when (observeThemeMode()) {
        ThemeUtils.THEME_MODE_LIGHT -> false
        ThemeUtils.THEME_MODE_DARK -> true
        else -> isSystemInDarkTheme()
    }
}

@Composable
fun ThemeUtils.observeIsMonoPalette(): Boolean =
    observeThemeColor() == ThemeUtils.THEME_COLOR_MONO && !observeIsDynamicColor()

@Composable
internal expect fun dynamicColorScheme(isDarkTheme: Boolean): ColorScheme?

@Composable
internal expect fun SyncSystemBars(isDarkTheme: Boolean)
