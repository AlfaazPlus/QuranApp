package com.quranapp.android.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
sealed class ReferenceThumbnail {
    @Serializable
    @SerialName("url")
    data class RemoteUrl(val url: String) : ReferenceThumbnail()

    @Serializable
    @SerialName("res")
    data class ResourceId(val id: Int) : ReferenceThumbnail()
}

@Serializable
data class ReferenceVerseModel(
    val title: String,
    val desc: String? = null,
    val translSlugs: Set<String> = emptySet(),
    val chapters: Set<Int> = emptySet(),
    val verses: Set<String> = emptySet(),
    val thumbnail: ReferenceThumbnail? = null,
) {

    fun encode(): String = json.encodeToString(this)

    fun toExtras(): Map<String, String> = mapOf(KEY to encode())

    companion object {
        const val KEY = "reference_verse_model"

        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

        fun decode(payload: String?): ReferenceVerseModel? {
            if (payload.isNullOrBlank()) {
                return null
            }

            return runCatching {
                json.decodeFromString<ReferenceVerseModel>(payload)
            }.getOrNull()
        }
    }
}
