package com.quranapp.android.api.models.tafsir

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChapterInfoApiResponse(
    @SerialName("chapter_info")
    val chapterInfo: ChapterInfoDetail,
    val resources: List<ChapterInfoResourceMeta> = emptyList(),
)

@Serializable
data class ChapterInfoDetail(
    @SerialName("lang_code")
    val languageCode: String? = null,
    @SerialName("source")
    val source: String? = null,
    @SerialName("short_text")
    val shortText: String? = null,
    @SerialName("text")
    val text: String? = null,
) {
    fun primaryContent(): String =
        text?.trim().orEmpty().ifEmpty { shortText?.trim().orEmpty() }
}

@Serializable
data class ChapterInfoResourceMeta(
    val id: Int,
    @SerialName("display_name")
    val displayName: String? = null,
    @SerialName("author_name")
    val authorName: String? = null,
    @SerialName("language_name")
    val languageName: String? = null,
)