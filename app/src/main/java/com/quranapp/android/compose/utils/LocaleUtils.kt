package com.quranapp.android.compose.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.quranapp.android.utils.sharedPrefs.SPAppConfigs
import java.util.Locale

fun String.normalizedLanguageTag(): String {
    return replace("-r", "-")
}

fun setAppLocale(context: Context, languageTag: String) {
    SPAppConfigs.setLocale(context, languageTag)
    AppCompatDelegate.setApplicationLocales(
        LocaleListCompat.forLanguageTags(languageTag.normalizedLanguageTag())
    )
}

fun appFallbackLanguageCodes(default: String = "en"): Sequence<String> {
    val locale = appLocale()
    return listOf(locale.toLanguageTag(), locale.language, default).asSequence()
}

fun appLocale(): Locale {
    val locale = AppCompatDelegate.getApplicationLocales()[0]
    return locale ?: Locale.getDefault()
}