package com.quranapp.android.compose.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.quranapp.android.utils.sharedPrefs.SPAppConfigs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

fun String.normalizedLanguageTag(): String {
    return replace("-r", "-")
}

fun setAppLocale(context: Context, languageTag: String) {
    SPAppConfigs.setLocale(context, languageTag)

    if (languageTag == "default") {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        _localeFlow.value = Locale.getDefault()
    } else {
        val normalizedlangTag = languageTag.normalizedLanguageTag()
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(normalizedlangTag)
        )

        _localeFlow.value = Locale.forLanguageTag(normalizedlangTag)
    }
}

fun appFallbackLanguageCodes(default: String = "en"): Sequence<String> {
    val locale = appLocale()
    return sequenceOf(locale.toLanguageTag(), locale.language, default)
}

fun appLocale(): Locale {
    val locales = AppCompatDelegate.getApplicationLocales()

    return if (locales.isEmpty) {
        Locale.getDefault()
    } else {
        locales[0]!!
    }
}

private val _localeFlow = MutableStateFlow(appLocale())
val localeFlow: StateFlow<Locale> = _localeFlow

private val formatters = ConcurrentHashMap<Locale, NumberFormat>()

fun String.isUrduLanguageCode(): Boolean {
    return this == "ur"
}
