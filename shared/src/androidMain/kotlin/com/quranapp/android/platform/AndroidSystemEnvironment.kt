package com.quranapp.android.platform

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.LocaleManagerCompat
import androidx.core.os.LocaleListCompat

class AndroidSystemEnvironment(
    context: Context,
) : SystemEnvironment {
    private val app = context.applicationContext ?: context

    private val metrics get() = app.resources.displayMetrics
    private val configuration get() = app.resources.configuration

    override val density: Float
        get() = metrics.density

    override val screenWidthPx: Int
        get() = metrics.widthPixels

    override val screenHeightPx: Int
        get() = metrics.heightPixels

    override val systemLanguageTag: String
        get() {
            val system = LocaleManagerCompat.getSystemLocales(app)

            if (!system.isEmpty) {
                return system[0]!!.toLanguageTag()
            }

            return configuration.locales[0]?.toLanguageTag()
                ?: java.util.Locale.getDefault().toLanguageTag()
        }

    override val isDarkTheme: Boolean
        get() {
            val night = configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            return night == Configuration.UI_MODE_NIGHT_YES
        }

    override val isDynamicColorSupported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    override fun applyThemeMode(mode: AppThemeMode) {
        AppCompatDelegate.setDefaultNightMode(
            when (mode) {
                AppThemeMode.Dark -> AppCompatDelegate.MODE_NIGHT_YES
                AppThemeMode.Light -> AppCompatDelegate.MODE_NIGHT_NO
                AppThemeMode.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            },
        )
    }

    override fun applyLanguage(languageTag: String?) {
        if (languageTag.isNullOrEmpty()) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
        }
    }
}
