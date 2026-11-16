package com.quranapp.android.theme

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.quranapp.android.core.R
import com.quranapp.android.preferences.DataStoreManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

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
