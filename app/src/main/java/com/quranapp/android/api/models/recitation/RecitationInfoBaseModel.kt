package com.quranapp.android.api.models.recitation

import com.quranapp.android.api.models.mediaplayer.ReciterAudioType
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
    
    /**
     * Type of audio files this reciter provides.
     * "verse" = individual verse files (default)
     * "chapter" = full chapter files with optional timing metadata
     */
    @SerialName("audio-type")
    var audioType: ReciterAudioType = ReciterAudioType.VERSE_BY_VERSE
    
    /**
     * URL path for timing metadata (only for FULL_CHAPTER audio type).
     * Pattern can include {chapNo} placeholder.
     * If null, chapter audio plays without verse sync.
     */
    @SerialName("timing-url-path")
    var timingUrlPath: String? = null
    
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