package com.quranapp.android.locale

import com.quranapp.android.platform.Platform
import com.quranapp.android.preferences.AppPreferences
import com.quranapp.android.preferences.DataStoreManager
import com.quranapp.android.utils.miscellenous.toFormatted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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
    val storedTag: String,
    val languageTag: String,
    val numeralSystem: NumeralSystem?,
) {
    val language: String
        get() = languageCodeOf(languageTag)

    fun format(pattern: String, vararg args: Any): String =
        pattern.toFormatted(languageTag, numeralSystem, *args)

    fun fallbackLanguageCodes(default: String = "en"): Sequence<String> {
        val legacy = when (language) {
            "id" -> "in"
            "in" -> "id"
            "he" -> "iw"
            "iw" -> "he"
            "yi" -> "ji"
            "ji" -> "yi"
            else -> null
        }

        return sequenceOf(languageTag, language, legacy, default)
            .filterNotNull()
            .distinct()
    }
}

fun String.normalizedLanguageTag(): String = replace("-r", "-")

fun languageCodeOf(tag: String): String =
    tag.substringBefore('-').substringBefore('_').lowercase()

fun numeralSystemsForLanguage(language: String): List<Pair<NumeralSystem, String>> =
    when (language.lowercase()) {
        "ar" -> listOf(
            NumeralSystem.LATN to "الأرقام الغربية (0–9)",
            NumeralSystem.ARAB to "الأرقام العربية (٠–٩)",
        )

        "fa" -> listOf(
            NumeralSystem.LATN to "اعداد غربی (0-9)",
            NumeralSystem.ARABEXT to "اعداد فارسی (۰–۹)",
        )

        else -> emptyList()
    }

fun coerceNumeralForLanguage(language: String, preference: NumeralSystem?): NumeralSystem? {
    if (preference == null) return null
    return if (numeralSystemsForLanguage(language).any { it.first == preference }) preference else null
}

fun String.isUrduLanguageCode(): Boolean = this == "ur"

fun resolveAppLocale(storedTag: String, numeralKey: String?): AppLocale {
    val languageTag = if (storedTag == LOCALE_DEFAULT) {
        platformDefaultLanguageTag()
    } else {
        storedTag.normalizedLanguageTag()
    }
    val preference =
        if (numeralKey.isNullOrEmpty()) null else NumeralSystem.fromStorage(numeralKey)

    return AppLocale(
        storedTag = storedTag,
        languageTag = languageTag,
        numeralSystem = coerceNumeralForLanguage(languageCodeOf(languageTag), preference),
    )
}

fun appLocaleForLanguageChange(
    languageTag: String,
    numberSystem: NumeralSystem?,
): AppLocale = resolveAppLocale(languageTag, numberSystem?.storageKey)

suspend fun refreshAppLocale() {
    publishAppLocale(
        resolveAppLocale(
            DataStoreManager.readFirst(AppPreferences.KEY_APP_LANGUAGE),
            DataStoreManager.readFirst(AppPreferences.KEY_NUMERAL_SYSTEM)
                .takeIf { it.isNotEmpty() },
        ),
    )
}

suspend fun setAppLocale(locale: AppLocale) {
    AppPreferences.setLocale(locale.storedTag)
    AppPreferences.setNumeralSystem(locale.numeralSystem?.storageKey)

    val languageTag = if (locale.storedTag == LOCALE_DEFAULT) {
        null
    } else {
        locale.storedTag.normalizedLanguageTag()
    }
    Platform.environment.applyLanguage(languageTag)

    refreshAppLocale()
}

private val appLocaleState = MutableStateFlow(platformInitialAppLocale())

val appLocaleFlow: StateFlow<AppLocale> = appLocaleState

fun appLocale(): AppLocale = appLocaleState.value

fun appFallbackLanguageCodes(default: String = "en"): Sequence<String> =
    appLocale().fallbackLanguageCodes(default)

internal fun publishAppLocale(locale: AppLocale) {
    appLocaleState.value = locale
}

private fun platformInitialAppLocale(): AppLocale =
    resolveAppLocale(LOCALE_DEFAULT, null)

internal expect fun platformDefaultLanguageTag(): String

expect fun languageDisplayName(tag: String, inLanguageTag: String): String
