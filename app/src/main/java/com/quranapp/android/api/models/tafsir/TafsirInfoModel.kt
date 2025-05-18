package com.quranapp.android.api.models.tafsir

import kotlinx.serialization.SerialName

@kotlinx.serialization.Serializable
data class TafsirInfoModel(
    val key: String,
    val name: String,
    val author: String,
    @SerialName("lang_code")
    val langCode: String,
    @SerialName("lang_name")
    val langName: String,
    val slug: String,
) {
    var isChecked = false


    override fun equals(other: Any?): Boolean {
        if (other !is TafsirInfoModel) {
            return false
        }
        return other.key == key
    }

    override fun hashCode(): Int {
        return key.hashCode()
    }
}