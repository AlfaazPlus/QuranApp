package com.quranapp.android.utils.sharedPrefs

import ThemeUtils
import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object SPAppConfigs {
    const val SP_APP_CONFIGS = "sp_app_configs"
    const val KEY_APP_THEME = "key.app.theme"
    private const val KEY_APP_LANGUAGE = "key.app.language"
    private const val KEY_URLS_VERSION = "key.versions.urls"

    const val LOCALE_DEFAULT = "default"

    private fun sp(context: Context) = context.getSharedPreferences(
        SP_APP_CONFIGS,
        Context.MODE_PRIVATE
    )

    @JvmStatic
    fun setThemeMode(ctx: Context, themeMode: String) {
        sp(ctx).edit() {
            putString(KEY_APP_THEME, themeMode)
        }

        CoroutineScope(Dispatchers.IO).launch {
            ThemeUtils.setThemeMode(themeMode)
        }
    }

    fun getThemeMode(ctx: Context): String =
        sp(ctx).getString(KEY_APP_THEME, ThemeUtils.THEME_MODE_DEFAULT)
            ?: ThemeUtils.THEME_MODE_DEFAULT

    @JvmStatic
    fun setLocale(ctx: Context, locale: String?) {
        sp(ctx).edit(commit = true) {
            putString(KEY_APP_LANGUAGE, locale)
        }
    }

    @JvmStatic
    fun getLocale(ctx: Context): String =
        sp(ctx).getString(KEY_APP_LANGUAGE, LOCALE_DEFAULT) ?: LOCALE_DEFAULT

}
