/*
 * Created by Faisal Khan on (c) 16/8/2021.
 */
package com.quranapp.android.api.models.recitation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class RecitationTranslationInfoModel(
    val slug: String,
    @SerialName("lang-code") val langCode: String,
    @SerialName("lang-name") var langName: String,
    val reciter: String,
    @SerialName("url-host") var urlHost: String?,
    @SerialName("url-path") val urlPath: String,
    val translations: Map<String, String> = mapOf(),
    var isChecked: Boolean = false
) {

    fun getReciterName(): String {
        return translations[Locale.getDefault().toLanguageTag()] ?: this.reciter
    }

    override fun equals(other: Any?): Boolean {
        if (other !is RecitationTranslationInfoModel) {
            return false
        }
        return other.slug == slug
    }

    override fun toString(): String {
        return "slug:$slug, reciter:$reciter, translated:${getReciterName()}"
    }

    override fun hashCode(): Int {
        return slug.hashCode()
    }
}
