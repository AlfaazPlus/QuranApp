package com.quranapp.android.api.models.recitation2

import com.quranapp.android.compose.utils.appFallbackLanguageCodes
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecitationQuranModel(
    val style: String?,
    @SerialName("style_translations")
    val styleTranslations: Map<String, String> = mapOf(),
) : RecitationModelBase() {
    fun getStyleName(): String? {
        return appFallbackLanguageCodes(default = "")
            .firstNotNullOfOrNull { langCode -> styleTranslations[langCode] }
            ?: this.style
    }

    override fun toString(): String {
        return "RecitationQuranModel(${super.toString()})"
    }
}
