package com.quranapp.android.api.models.recitation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
open class RecitationInfoBaseModel : java.io.Serializable {
    var slug: String = ""
    var reciter: String = ""

    @SerialName("url-host")
    var urlHost: String? = null

    @SerialName("url-path")
    var urlPath: String = ""
    val translations: Map<String, String> = mapOf()
    var isChecked: Boolean = false


    fun getReciterName(): String {
        return translations[Locale.getDefault().toLanguageTag()] ?: this.reciter
    }

    override fun equals(other: Any?): Boolean {
        if (other !is RecitationInfoBaseModel) {
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