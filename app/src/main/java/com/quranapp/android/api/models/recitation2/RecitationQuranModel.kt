package com.quranapp.android.api.models.recitation2

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class RecitationQuranModel(
    val style: String?,
    val styleTranslations: Map<String, String> = mapOf(),
) : RecitationModelBase() {
    fun getStyleName(): String? {
        return styleTranslations[Locale.getDefault().toLanguageTag()] ?: this.style
    }

    override fun toString(): String {
        return "RecitationQuranModel(${super.toString()})"
    }
}
