package com.quranapp.android.utils.app

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.quranapp.android.R
import com.quranapp.android.utils.sharedPrefs.SPAppConfigs

object ThemeUtils {
    @JvmStatic
    fun resolveThemeTextFromMode(context: Context): String {
        return context.getString(
            when (SPAppConfigs.getThemeMode(context)) {
                SPAppConfigs.THEME_MODE_DARK -> R.string.strLabelThemeDark
                SPAppConfigs.THEME_MODE_LIGHT -> R.string.strLabelThemeLight
                SPAppConfigs.THEME_MODE_DEFAULT -> R.string.strLabelSystemDefault
                else -> R.string.strLabelSystemDefault
            }
        )
    }

    @JvmStatic
    fun resolveThemeIdFromMode(context: Context): Int {
        return when (SPAppConfigs.getThemeMode(context)) {
            SPAppConfigs.THEME_MODE_DARK -> R.id.themeDark
            SPAppConfigs.THEME_MODE_LIGHT -> R.id.themeLight
            SPAppConfigs.THEME_MODE_DEFAULT -> R.id.systemDefault
            else -> R.id.systemDefault
        }
    }

    @JvmStatic
    fun resolveThemeModeFromId(id: Int): String {
        return when (id) {
            R.id.themeDark -> {
                SPAppConfigs.THEME_MODE_DARK
            }
            R.id.themeLight -> {
                SPAppConfigs.THEME_MODE_LIGHT
            }
            else -> {
                SPAppConfigs.THEME_MODE_DEFAULT
            }
        }
    }

    @JvmStatic
    fun resolveThemeModeFromSP(context: Context): Int {
        return when (SPAppConfigs.getThemeMode(context)) {
            SPAppConfigs.THEME_MODE_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            SPAppConfigs.THEME_MODE_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            SPAppConfigs.THEME_MODE_DEFAULT -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
    }
}
