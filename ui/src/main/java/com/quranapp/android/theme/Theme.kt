package com.quranapp.android.theme

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.quranapp.android.locale.LocalAppLocale
import com.quranapp.android.locale.appLocaleFlow
import com.quranapp.android.preferences.DataStoreManager
import com.quranapp.android.preferences.observe
import com.quranapp.android.theme.colors.BaseColors
import com.quranapp.android.theme.colors.ThemeAmoledColors
import com.quranapp.android.theme.colors.ThemeBlueColors
import com.quranapp.android.theme.colors.ThemeDefaultColors
import com.quranapp.android.theme.colors.ThemeMonoColors
import com.quranapp.android.theme.colors.ThemePurpleColors
import com.quranapp.android.theme.colors.ThemeRedColors
import com.quranapp.android.theme.colors.ThemeVioletColors
import com.quranapp.android.theme.colors.ThemeYellowColors

@Composable
fun QuranAppTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val context = LocalContext.current

    val isDarkTheme = ThemeUtils.observeDarkTheme()
    val colorScheme = ThemeUtils.observeColorScheme(context, isDarkTheme)
    val appLocale by appLocaleFlow.collectAsState()

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !isDarkTheme
                isAppearanceLightNavigationBars = !isDarkTheme
            }
        }
    }

    CompositionLocalProvider(LocalAppLocale provides appLocale) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = getAppTypography(),
            content = content
        )
    }

}

@Composable
fun ThemeUtils.observeThemeMode(): String =
    DataStoreManager.observe(ThemeUtils.KEY_THEME_MODE, ThemeUtils.THEME_MODE_DEFAULT)

@Composable
fun ThemeUtils.observeThemeColor(): String =
    DataStoreManager.observe(ThemeUtils.KEY_THEME_COLOR, ThemeUtils.THEME_COLOR_DEFAULT)

@Composable
fun ThemeUtils.observeIsDynamicColor(): Boolean =
    DataStoreManager.observe(ThemeUtils.KEY_THEME_DYNAMIC_COLOR, ThemeUtils.DEFAULT_DYNAMIC_COLOR)

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

fun ThemeUtils.colorSchemeFromPreferences(
    context: Context,
    isDark: Boolean? = null,
    themeColor: String = getThemeColor(),
    isDynamicColor: Boolean = getDynamicColor(),
): ColorScheme {
    return buildColorScheme(
        context,
        isDark ?: isDarkTheme(context),
        themeColor,
        isDynamicColor,
    )
}

private fun buildColorScheme(
    context: Context,
    isDarkTheme: Boolean,
    themeColor: String,
    isDynamicColor: Boolean,
): ColorScheme {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isDynamicColor) {
        return if (isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }

    val preferredColor: BaseColors = when (themeColor) {
        ThemeUtils.THEME_COLOR_BLUE -> ThemeBlueColors()
        ThemeUtils.THEME_COLOR_RED -> ThemeRedColors()
        ThemeUtils.THEME_COLOR_PURPLE -> ThemePurpleColors()
        ThemeUtils.THEME_COLOR_MONO -> ThemeMonoColors()
        ThemeUtils.THEME_COLOR_AMOLED -> ThemeAmoledColors()
        ThemeUtils.THEME_COLOR_VIOLET -> ThemeVioletColors()
        ThemeUtils.THEME_COLOR_YELLOW -> ThemeYellowColors()
        else -> ThemeDefaultColors()
    }

    return if (isDarkTheme) preferredColor.darkColors() else preferredColor.lightColors()
}
