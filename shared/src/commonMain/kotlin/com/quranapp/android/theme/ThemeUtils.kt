package com.quranapp.android.theme

import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.strLabelSystemDefault
import com.quranapp.android.generated.resources.strLabelThemeDark
import com.quranapp.android.generated.resources.strLabelThemeLight
import androidx.compose.material3.ColorScheme
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.quranapp.android.preferences.DataStoreManager
import com.quranapp.android.theme.colors.BaseColors
import com.quranapp.android.theme.colors.ThemeAmoledColors
import com.quranapp.android.theme.colors.ThemeBlueColors
import com.quranapp.android.theme.colors.ThemeDefaultColors
import com.quranapp.android.theme.colors.ThemeMonoColors
import com.quranapp.android.theme.colors.ThemePurpleColors
import com.quranapp.android.theme.colors.ThemeRedColors
import com.quranapp.android.theme.colors.ThemeVioletColors
import com.quranapp.android.theme.colors.ThemeYellowColors
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.compose.resources.StringResource

object ThemeUtils {
    const val THEME_MODE_DEFAULT = "app.theme.default"
    const val THEME_MODE_LIGHT = "app.theme.light"
    const val THEME_MODE_DARK = "app.theme.dark"

    val KEY_THEME_MODE = stringPreferencesKey("v2.theme_mode")
    val KEY_THEME_COLOR = stringPreferencesKey("v2.theme_color")
    val KEY_THEME_DYNAMIC_COLOR = booleanPreferencesKey("v2.theme_dynamic_color")

    const val THEME_COLOR_DEFAULT = "default"
    const val THEME_COLOR_BLUE = "blue"
    const val THEME_COLOR_RED = "red"
    const val THEME_COLOR_PURPLE = "purple"
    const val THEME_COLOR_MONO = "mono"
    const val THEME_COLOR_AMOLED = "amoled"
    const val THEME_COLOR_VIOLET = "violet"
    const val THEME_COLOR_YELLOW = "yellow"

    const val DEFAULT_DYNAMIC_COLOR = false

    fun resolveThemeModeLabel(themeMode: String): StringResource {
        return when (themeMode) {
            THEME_MODE_LIGHT -> Res.string.strLabelThemeLight
            THEME_MODE_DARK -> Res.string.strLabelThemeDark
            else -> Res.string.strLabelSystemDefault
        }
    }

    fun getThemeMode(): String {
        return DataStoreManager.read(KEY_THEME_MODE, THEME_MODE_DEFAULT)
    }

    suspend fun setThemeMode(themeMode: String) {
        DataStoreManager.write(KEY_THEME_MODE, themeMode)
    }

    fun getThemeColor(): String {
        return DataStoreManager.read(KEY_THEME_COLOR, THEME_COLOR_DEFAULT)
    }

    fun getDynamicColor(): Boolean {
        return DataStoreManager.read(KEY_THEME_DYNAMIC_COLOR, DEFAULT_DYNAMIC_COLOR)
    }

    suspend fun setThemeColor(themeColor: String) {
        DataStoreManager.write(KEY_THEME_COLOR, themeColor)
    }

    suspend fun setDynamicColor(isDynamicColor: Boolean) {
        DataStoreManager.write(KEY_THEME_DYNAMIC_COLOR, isDynamicColor)
    }

    fun widgetAppearancePreferencesFlow(): Flow<Triple<String, String, Boolean>> {
        return combine(
            DataStoreManager.flow(KEY_THEME_MODE, THEME_MODE_DEFAULT),
            DataStoreManager.flow(KEY_THEME_COLOR, THEME_COLOR_DEFAULT),
            DataStoreManager.flow(KEY_THEME_DYNAMIC_COLOR, DEFAULT_DYNAMIC_COLOR),
        ) { mode, color, dynamicColor ->
            Triple(mode, color, dynamicColor)
        }.distinctUntilChanged()
    }

    fun palette(isDarkTheme: Boolean, themeColor: String): ColorScheme {
        val preferredColor: BaseColors = when (themeColor) {
            THEME_COLOR_BLUE -> ThemeBlueColors()
            THEME_COLOR_RED -> ThemeRedColors()
            THEME_COLOR_PURPLE -> ThemePurpleColors()
            THEME_COLOR_MONO -> ThemeMonoColors()
            THEME_COLOR_AMOLED -> ThemeAmoledColors()
            THEME_COLOR_VIOLET -> ThemeVioletColors()
            THEME_COLOR_YELLOW -> ThemeYellowColors()
            else -> ThemeDefaultColors()
        }

        return if (isDarkTheme) preferredColor.darkColors() else preferredColor.lightColors()
    }
}
