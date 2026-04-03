package com.quranapp.android.api.models.recitation2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
open class RecitationModelBase {
    var id: String = ""
    var reciter: String = ""

    @SerialName("url_template")
    var urlTemplate: String = ""

    /**
     * URL path for timing metadata (only for FULL_CHAPTER audio type).
     * Pattern can include {chapNo} placeholder.
     * If null, chapter audio plays without verse sync.
     */
    @SerialName("timing_url")
    var timingUrl: String? = null

    @SerialName("timing_version")
    var timingVersion: Int? = null

    val translations: Map<String, String> = mapOf()


    fun getReciterName(): String {
        return translations[Locale.getDefault().toLanguageTag()] ?: this.reciter
    }

    override fun equals(other: Any?): Boolean {
        if (other !is RecitationModelBase) {
            return false
        }
        return other.id == id
    }

    override fun toString(): String {
        return "id:$id, reciter:$reciter, translated:${getReciterName()}, urlTemplate:$urlTemplate, timingUrl:$timingUrl"
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}