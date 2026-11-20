package com.quranapp.android.locale

import com.quranapp.android.platform.Platform
import java.util.Locale

val AppLocale.platformLocale: Locale
    get() = javaLocale(languageTag, numeralSystem)

fun appPlatformLocale(): Locale = appLocale().platformLocale

internal actual fun platformDefaultLanguageTag(): String {
    return Platform.environment.systemLanguageTag
}

actual fun languageDisplayName(tag: String, inLanguageTag: String): String {
    val name = Locale.forLanguageTag(tag.normalizedLanguageTag())
        .getDisplayName(Locale.forLanguageTag(inLanguageTag.normalizedLanguageTag()))

    return name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
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
