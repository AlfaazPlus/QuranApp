package com.quranapp.android.api.models.recitation2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecitationTranslationModel(
    @SerialName("lang_code") val langCode: String,
    @SerialName("lang_name") var langName: String,
    val book: String?,
) : RecitationModelBase() {
    override fun toString(): String {
        return "RecitationTranslationModel(${super.toString()}, langCode:$langCode, langName:$langName, book:$book)"
    }
}
