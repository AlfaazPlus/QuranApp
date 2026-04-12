package com.quranapp.android.api.models.recitation2

import com.quranapp.android.compose.utils.appLocale
import kotlinx.serialization.Serializable

@Serializable
data class RecitationQuranModel(
    val style: String?,
    val styleTranslations: Map<String, String> = mapOf(),
) : RecitationModelBase() {
    fun getStyleName(): String? {
        val locale = appLocale()
        return styleTranslations[locale.toLanguageTag()]
            ?: styleTranslations[locale.language]
            ?: this.style
    }

    override fun toString(): String {
        return "RecitationQuranModel(${super.toString()})"
    }
}
