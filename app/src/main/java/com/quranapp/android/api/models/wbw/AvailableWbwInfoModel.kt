package com.quranapp.android.api.models.wbw

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AvailableWbwInfoModel(
    @SerialName("version")
    val version: Int?,
    @SerialName("wbw")
    val wbw: List<WbwLanguageInfo>,
)

@Serializable
data class WbwLanguageInfo(
    @SerialName("id")
    val id: String,
    @SerialName("lang_code")
    val langCode: String,
    @SerialName("lang_name")
    val langName: String,
    @SerialName("has_translation")
    val hasTranslation: Boolean,
    @SerialName("has_transliteration")
    val hasTransliteration: Boolean,
    @SerialName("url")
    val url: String,
    @SerialName("version")
    val version: Int,
)

@Serializable
data class WbwPayloadModel(
    @SerialName("version")
    val version: Int,

    // verse id -> ordered words
    // each word -> [string, string] -> [translation, transliteration];
    @SerialName("verses")
    val verses: Map<Int, List<List<String?>>>,
)
