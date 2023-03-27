package com.quranapp.android.components.tafsir

@kotlinx.serialization.Serializable
data class TafsirModel(
    val key: String,
    val name: String,
    val author: String,
    val langCode: String,
    val langName: String,
    val slug: String,
) {
    var isChecked = false


    override fun equals(other: Any?): Boolean {
        if (other !is TafsirModel) {
            return false
        }
        return other.key == key
    }

    override fun hashCode(): Int {
        return key.hashCode()
    }
}