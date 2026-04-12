package com.quranapp.android.components.quran

import com.quranapp.android.compose.utils.appLocale
import java.io.Serializable

data class QuranScienceItem(
    private val title: String,
    val referencesCount: Int,
    val path: String,
    val drawableRes: Int,
    val translations: Map<String, String>
) : Serializable {
    fun getTitle(): String {
        val locale = appLocale()
        val langCode1 = with(locale.language) {
            // Hosted weblate uses "id" for Indonesian but Android uses "in"
            if (this == "in") "id" else this
        }
        val langCode2 = "$langCode1-r${locale.country}"

        var name = translations.getOrElse(langCode2, {
            translations.getOrDefault(
                langCode1,
                title
            )
        })

        return name
    }

}