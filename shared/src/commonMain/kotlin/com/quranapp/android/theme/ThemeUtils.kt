package com.quranapp.android.theme

import androidx.compose.material3.ColorScheme
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.strLabelSystemDefault
import com.quranapp.android.generated.resources.strLabelThemeDark
import com.quranapp.android.generated.resources.strLabelThemeLight
import com.quranapp.android.platform.AppThemeMode
import com.quranapp.android.platform.Platform
import com.quranapp.android.preferences.DataStoreManager
import com.quranapp.android.preferences.PrefKey
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
    val KEY_THEME_MODE =
        PrefKey(stringPreferencesKey("v2.theme_mode"), AppThemeMode.System.storageKey)
    val KEY_THEME_COLOR = PrefKey(stringPreferencesKey("v2.theme_color"), THEME_COLOR_DEFAULT)
    val KEY_THEME_DYNAMIC_COLOR =
        PrefKey(booleanPreferencesKey("v2.theme_dynamic_color"), DEFAULT_DYNAMIC_COLOR)

    const val THEME_COLOR_DEFAULT = "default"
    const val THEME_COLOR_BLUE = "blue"
    const val THEME_COLOR_RED = "red"
    const val THEME_COLOR_PURPLE = "purple"
    const val THEME_COLOR_MONO = "mono"
    const val THEME_COLOR_AMOLED = "amoled"
    const val THEME_COLOR_VIOLET = "violet"
    const val THEME_COLOR_YELLOW = "yellow"

    const val DEFAULT_DYNAMIC_COLOR = false

    fun resolveThemeModeLabel(themeMode: AppThemeMode): StringResource {
        return when (themeMode) {
            AppThemeMode.Light -> Res.string.strLabelThemeLight
            AppThemeMode.Dark -> Res.string.strLabelThemeDark
            AppThemeMode.System -> Res.string.strLabelSystemDefault
        }
    }

    fun applyNightMode(mode: AppThemeMode = getThemeMode()) {
        Platform.environment.applyThemeMode(mode)
    }

    fun getThemeMode(): AppThemeMode {
        return AppThemeMode.fromStorage(DataStoreManager.read(KEY_THEME_MODE))
    }

    suspend fun setThemeMode(themeMode: AppThemeMode) {
        DataStoreManager.write(KEY_THEME_MODE, themeMode.storageKey)
    }

    fun getThemeColor(): String {
        return DataStoreManager.read(KEY_THEME_COLOR)
    }

    fun getDynamicColor(): Boolean {
        return DataStoreManager.read(KEY_THEME_DYNAMIC_COLOR)
    }

    fun isDynamicColorSupported(): Boolean =
        Platform.environment.isDynamicColorSupported

    fun isDarkTheme(): Boolean {
        return when (getThemeMode()) {
            AppThemeMode.Light -> false
            AppThemeMode.Dark -> true
            AppThemeMode.System -> Platform.environment.isDarkTheme
        }
    }

    suspend fun setThemeColor(themeColor: String) {
        DataStoreManager.write(KEY_THEME_COLOR, themeColor)
    }

    suspend fun setDynamicColor(isDynamicColor: Boolean) {
        DataStoreManager.write(KEY_THEME_DYNAMIC_COLOR, isDynamicColor)
    }

    fun widgetAppearancePreferencesFlow(): Flow<Triple<String, String, Boolean>> {
        return combine(
            DataStoreManager.flow(KEY_THEME_MODE),
            DataStoreManager.flow(KEY_THEME_COLOR),
            DataStoreManager.flow(KEY_THEME_DYNAMIC_COLOR),
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
