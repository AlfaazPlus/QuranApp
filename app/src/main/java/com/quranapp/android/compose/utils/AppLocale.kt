package com.quranapp.android.compose.utils

import android.content.Context
import android.content.res.Resources
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.app.LocaleManagerCompat
import androidx.core.os.LocaleListCompat
import com.quranapp.android.utils.sharedPrefs.SPAppConfigs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

enum class NumeralSystem(val storageKey: String) {
    LATN("latn"),
    ARAB("arab"),
    ARABEXT("arabext"),
    ;

    val nuKeyword: String get() = storageKey

    companion object {
        fun fromStorage(key: String?): NumeralSystem {
            if (key == null) return LATN
            return entries.firstOrNull { it.storageKey == key } ?: LATN
        }
    }
}

data class AppLocale(
    val rawLanguageTag: String,
    val numeralSystem: NumeralSystem?,
    private val baseLocale: Locale,
) {
    val platformLocale: Locale
        get() = Locale.Builder()
            .setLocale(baseLocale)
            .apply {
                if (numeralSystem != null) {
                    setUnicodeLocaleKeyword("nu", numeralSystem.nuKeyword)
                }
            }
            .build()

    fun fallbackLanguageCodes(default: String = "en"): Sequence<String> =
        sequenceOf(baseLocale.toLanguageTag(), baseLocale.language, default)
}

fun String.normalizedLanguageTag(): String = replace("-r", "-")

private fun systemDisplayLocale(context: Context? = null): Locale {
    if (context != null) {
        val system = LocaleManagerCompat.getSystemLocales(context.applicationContext)
        if (!system.isEmpty) return system[0]!!
    }

    val config = Resources.getSystem().configuration
    return config.locales[0] ?: Locale.getDefault()
}

fun numeralSystemsForLanguage(language: String): List<Pair<NumeralSystem, String>> =
    when (language.lowercase(Locale.ROOT)) {
        "ar" -> listOf(
            NumeralSystem.LATN to "الأرقام الغربية (0–9)",
            NumeralSystem.ARAB to "الأرقام العربية (٠–٩)"
        )

        "fa" -> listOf(
            NumeralSystem.LATN to "اعداد غربی (0-9)",
            NumeralSystem.ARABEXT to "اعداد فارسی (۰–۹)"
        )

        else -> emptyList()
    }

fun coerceNumeralForLanguage(language: String, preference: NumeralSystem?): NumeralSystem? {
    if (preference == null) return null
    return if (numeralSystemsForLanguage(language).any { it.first == preference }) preference else null
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

    return AppLocale(
        rawLanguageTag = languageTag,
        baseLocale = baseLocale,
        numeralSystem = coerceNumeralForLanguage(baseLocale.language, storedNumeral),
    )
}

fun appLocaleForLanguageChange(
    context: Context,
    languageTag: String,
    numberSystem: NumeralSystem?
): AppLocale {
    val baseLocale = if (languageTag == SPAppConfigs.LOCALE_DEFAULT) {
        systemDisplayLocale(context)
    } else {
        Locale.forLanguageTag(languageTag.normalizedLanguageTag())
    }

    return AppLocale(
        rawLanguageTag = languageTag,
        baseLocale = baseLocale,
        numeralSystem = coerceNumeralForLanguage(baseLocale.language, numberSystem),
    )
}

private val _appLocaleFlow = MutableStateFlow(
    run {
        val base = systemDisplayLocale()

        AppLocale(
            rawLanguageTag = SPAppConfigs.LOCALE_DEFAULT,
            baseLocale = base,
            numeralSystem = coerceNumeralForLanguage(base.language, null),
        )
    },
)

val appLocaleFlow: StateFlow<AppLocale> = _appLocaleFlow

fun refreshAppLocale(context: Context) {
    _appLocaleFlow.value = readAppLocale(context.applicationContext)
}

fun setAppLocale(context: Context, locale: AppLocale) {
    SPAppConfigs.setLocale(context, locale.rawLanguageTag)
    SPAppConfigs.setNumeralSystem(context, locale.numeralSystem?.storageKey)

    if (locale.rawLanguageTag == SPAppConfigs.LOCALE_DEFAULT) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
    } else {
        val normalized = locale.rawLanguageTag.normalizedLanguageTag()
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(normalized))
    }

    refreshAppLocale(context)
}

fun appLocale(): AppLocale = _appLocaleFlow.value

fun appPlatformLocale(): Locale = appLocale().platformLocale

fun appFallbackLanguageCodes(default: String = "en"): Sequence<String> =
    appLocale().fallbackLanguageCodes(default)

fun String.isUrduLanguageCode(): Boolean = this == "ur"

val LocalAppLocale = staticCompositionLocalOf<AppLocale> {
    error("LocalAppLocale not provided; wrap content with QuranAppTheme")
}
