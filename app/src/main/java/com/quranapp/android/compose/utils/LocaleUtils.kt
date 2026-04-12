package com.quranapp.android.compose.utils

import androidx.appcompat.app.AppCompatDelegate
import java.util.Locale

fun String.normalizedLanguageTag(): String {
    return replace("-r", "-")
}

fun appLocale(): Locale {
    val locale = AppCompatDelegate.getApplicationLocales()[0]
    return locale ?: Locale.getDefault()
}