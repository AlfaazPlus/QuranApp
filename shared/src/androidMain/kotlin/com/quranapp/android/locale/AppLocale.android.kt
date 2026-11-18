package com.quranapp.android.locale

import android.content.Context
import android.content.res.Resources
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.LocaleManagerCompat
import androidx.core.os.LocaleListCompat
import com.quranapp.android.preferences.AppPreferences
import com.quranapp.android.preferences.DataStoreManager
import kotlinx.coroutines.runBlocking
import java.util.Locale

val AppLocale.platformLocale: Locale
    get() = javaLocale(languageTag, numeralSystem)

fun appPlatformLocale(): Locale = appLocale().platformLocale

internal actual fun platformInitialAppLocale(): AppLocale {
    val base = systemDisplayLocale()

    return base.toAppLocale(
        storedTag = LOCALE_DEFAULT,
        numeralSystem = null,
    )
}

private fun javaLocale(languageTag: String, numeralSystem: NumeralSystem?): Locale {
    return Locale.Builder()
        .setLocale(Locale.forLanguageTag(languageTag))
        .apply {
            if (numeralSystem != null) {
                setUnicodeLocaleKeyword("nu", numeralSystem.nuKeyword)
            }
        }
        .build()
}

private fun Locale.toAppLocale(
    storedTag: String,
    numeralSystem: NumeralSystem?,
): AppLocale {
    val languageTag = toLanguageTag()

    return AppLocale(
        storedTag = storedTag,
        languageTag = languageTag,
        numeralSystem = coerceNumeralForLanguage(languageTag.substringBefore('-'), numeralSystem),
    )
}

private fun systemDisplayLocale(context: Context? = null): Locale {
    if (context != null) {
        val system = LocaleManagerCompat.getSystemLocales(context.applicationContext)
        if (!system.isEmpty) return system[0]!!
    }

    val config = Resources.getSystem().configuration
    return config.locales[0] ?: Locale.getDefault()
}

fun readAppLocale(context: Context): AppLocale {
    return resolveAppLocale(
        context,
        AppPreferences.getLocale(),
        AppPreferences.getNumeralSystem(),
    )
}

private fun resolveAppLocale(
    context: Context,
    languageTag: String,
    numeralSystem: String?,
): AppLocale {
    val baseLocale = when {
        languageTag == LOCALE_DEFAULT -> systemDisplayLocale(context)
        else -> {
            val appLocales = AppCompatDelegate.getApplicationLocales()

            if (!appLocales.isEmpty) appLocales[0]!!
            else Locale.forLanguageTag(languageTag.normalizedLanguageTag())
        }
    }

    return baseLocale.toAppLocale(
        storedTag = languageTag,
        numeralSystem = NumeralSystem.fromStorage(numeralSystem),
    )
}

fun appLocaleForLanguageChange(
    context: Context,
    languageTag: String,
    numberSystem: NumeralSystem?,
): AppLocale {
    val baseLocale = if (languageTag == LOCALE_DEFAULT) {
        systemDisplayLocale(context)
    } else {
        Locale.forLanguageTag(languageTag.normalizedLanguageTag())
    }

    return baseLocale.toAppLocale(
        storedTag = languageTag,
        numeralSystem = numberSystem,
    )
}

fun refreshAppLocale(context: Context) {
    val appContext = context.applicationContext
    val locale = runBlocking {
        resolveAppLocale(
            appContext,
            DataStoreManager.readFirst(AppPreferences.KEY_APP_LANGUAGE),
            DataStoreManager.readFirst(AppPreferences.KEY_NUMERAL_SYSTEM)
                .takeIf { it.isNotEmpty() },
        )
    }

    publishAppLocale(locale)
}

fun setAppLocale(context: Context, locale: AppLocale) {
    runBlocking {
        AppPreferences.setLocale(locale.storedTag)
        AppPreferences.setNumeralSystem(locale.numeralSystem?.storageKey)
    }

    if (locale.storedTag == LOCALE_DEFAULT) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
    } else {
        val normalized = locale.storedTag.normalizedLanguageTag()
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(normalized))
    }

    refreshAppLocale(context)
}
