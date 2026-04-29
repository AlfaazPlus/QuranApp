package com.quranapp.android.compose.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.alfaazplus.sunnah.ui.theme.colors.BaseColors
import com.alfaazplus.sunnah.ui.theme.colors.ThemeBlueColors
import com.alfaazplus.sunnah.ui.theme.colors.ThemeDefaultColors
import com.alfaazplus.sunnah.ui.theme.colors.ThemeMonoColors
import com.alfaazplus.sunnah.ui.theme.colors.ThemePurpleColors
import com.alfaazplus.sunnah.ui.theme.colors.ThemeRedColors
import com.alfaazplus.sunnah.ui.theme.colors.ThemeVioletColors
import com.alfaazplus.sunnah.ui.theme.colors.ThemeYellowColors
import com.alfaazplus.sunnah.ui.utils.shared_preference.DataStoreManager
import com.quranapp.android.R
import com.quranapp.android.compose.utils.ThemeUtils.observeDarkTheme

object ThemeUtils {
    const val THEME_MODE_DEFAULT = "app.theme.default"
    const val THEME_MODE_LIGHT = "app.theme.light"
    const val THEME_MODE_DARK = "app.theme.dark"

    private val KEY_THEME_MODE = stringPreferencesKey("v2.theme_mode")
    private val KEY_THEME_COLOR = stringPreferencesKey("v2.theme_color")
    private val KEY_THEME_DYNAMIC_COLOR = booleanPreferencesKey("v2.theme_dynamic_color")

    const val THEME_COLOR_DEFAULT = "default"
    const val THEME_COLOR_BLUE = "blue"
    const val THEME_COLOR_RED = "red"
    const val THEME_COLOR_PURPLE = "purple"
    const val THEME_COLOR_MONO = "mono"
    const val THEME_COLOR_VIOLET = "violet"
    const val THEME_COLOR_YELLOW = "yellow"

    fun isDynamicColorSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }

    fun resolveThemeModeLabel(themeMode: String): Int {
        return when (themeMode) {
            THEME_MODE_LIGHT -> R.string.strLabelThemeLight
            THEME_MODE_DARK -> R.string.strLabelThemeDark
            else -> R.string.strLabelSystemDefault
        }
    }

    @Composable
    fun observeDarkTheme(): Boolean {
        val themeMode = observeThemeMode()

        return when (themeMode) {
            THEME_MODE_LIGHT -> false
            THEME_MODE_DARK -> true
            else -> isSystemInDarkTheme()
        }
    }

    @Composable
    fun observeThemeMode(): String {
        return DataStoreManager.observe(KEY_THEME_MODE, THEME_MODE_DEFAULT)
    }

    fun getThemeMode(): String {
        return DataStoreManager.read(KEY_THEME_MODE, THEME_MODE_DEFAULT)
    }

    suspend fun setThemeMode(themeMode: String) {
        DataStoreManager.write(KEY_THEME_MODE, themeMode)
    }

    @Composable
    fun observeThemeColor(): String {
        return DataStoreManager.observe(KEY_THEME_COLOR, THEME_COLOR_DEFAULT)
    }

    suspend fun setThemeColor(themeColor: String) {
        DataStoreManager.write(KEY_THEME_COLOR, themeColor)
    }

    @Composable
    fun observeIsDynamicColor(): Boolean {
        return DataStoreManager.observe(KEY_THEME_DYNAMIC_COLOR, false)
    }

    suspend fun setDynamicColor(isDynamicColor: Boolean) {
        DataStoreManager.write(KEY_THEME_DYNAMIC_COLOR, isDynamicColor)
    }

    @Composable
    fun observeColorScheme(
        context: Context,
        isDarkTheme: Boolean = observeDarkTheme()
    ): ColorScheme {
        val themeColor = observeThemeColor()
        val isDynamicColor = observeIsDynamicColor()
        return buildColorScheme(context, isDarkTheme, themeColor, isDynamicColor)
    }

    fun colorSchemeFromPreferences(context: Context): ColorScheme {
        return buildColorScheme(
            context,
            isDarkTheme(context),
            DataStoreManager.read(KEY_THEME_COLOR, THEME_COLOR_DEFAULT),
            DataStoreManager.read(KEY_THEME_DYNAMIC_COLOR, false),
        )
    }

    private fun buildColorScheme(
        context: Context,
        isDarkTheme: Boolean,
        themeColor: String,
        isDynamicColor: Boolean,
    ): ColorScheme {
        // Dynamic color is available on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isDynamicColor) {
            return if (isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(
                context,
            )
        }

        val preferredColor: BaseColors = when (themeColor) {
            THEME_COLOR_BLUE -> ThemeBlueColors()
            THEME_COLOR_RED -> ThemeRedColors()
            THEME_COLOR_PURPLE -> ThemePurpleColors()
            THEME_COLOR_MONO -> ThemeMonoColors()
            THEME_COLOR_VIOLET -> ThemeVioletColors()
            THEME_COLOR_YELLOW -> ThemeYellowColors()
            else -> ThemeDefaultColors()
        }

        return if (isDarkTheme) preferredColor.darkColors() else preferredColor.lightColors()
    }

    /**
     * Dark/light resolution aligned with [observeDarkTheme] (theme mode + system night).
     */
    fun isDarkTheme(context: Context): Boolean {
        return when (getThemeMode()) {
            THEME_MODE_LIGHT -> false
            THEME_MODE_DARK -> true
            else -> {
                val uiMode =
                    context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                uiMode == Configuration.UI_MODE_NIGHT_YES
            }
        }
    }

    fun resolveThemeModeForDelegate(themeMode: String? = null): Int {
        return when (themeMode ?: getThemeMode()) {
            THEME_MODE_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            THEME_MODE_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            THEME_MODE_DEFAULT -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
    }
}
