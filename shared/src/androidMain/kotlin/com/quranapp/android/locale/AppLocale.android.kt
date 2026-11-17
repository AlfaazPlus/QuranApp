package com.quranapp.android.locale

import android.content.Context
import android.content.res.Resources
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.LocaleManagerCompat
import androidx.core.os.LocaleListCompat
import com.quranapp.android.utils.sharedPrefs.SPAppConfigs
import java.util.Locale

val AppLocale.platformLocale: Locale
    get() = javaLocale(languageTag, numeralSystem)

fun appPlatformLocale(): Locale = appLocale().platformLocale

internal actual fun platformInitialAppLocale(): AppLocale {
    val base = systemDisplayLocale()
    return base.toAppLocale(
        storedTag = SPAppConfigs.LOCALE_DEFAULT,
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
    val languageTag = SPAppConfigs.getLocale(context)

    val baseLocale = when {
        languageTag == SPAppConfigs.LOCALE_DEFAULT -> systemDisplayLocale(context)
        else -> {
            val appLocales = AppCompatDelegate.getApplicationLocales()

            if (!appLocales.isEmpty) appLocales[0]!!
            else Locale.forLanguageTag(languageTag.normalizedLanguageTag())
        }
    }

    val storedNumeral = NumeralSystem.fromStorage(SPAppConfigs.getNumeralSystem(context))

    return baseLocale.toAppLocale(
        storedTag = languageTag,
        numeralSystem = storedNumeral,
    )
}

fun appLocaleForLanguageChange(
    context: Context,
    languageTag: String,
    numberSystem: NumeralSystem?,
): AppLocale {
    val baseLocale = if (languageTag == SPAppConfigs.LOCALE_DEFAULT) {
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
    publishAppLocale(readAppLocale(context.applicationContext))
}

fun setAppLocale(context: Context, locale: AppLocale) {
    SPAppConfigs.setLocale(context, locale.storedTag)
    SPAppConfigs.setNumeralSystem(context, locale.numeralSystem?.storageKey)

    if (locale.storedTag == SPAppConfigs.LOCALE_DEFAULT) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
    } else {
        val normalized = locale.storedTag.normalizedLanguageTag()
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(normalized))
    }

    refreshAppLocale(context)
}
