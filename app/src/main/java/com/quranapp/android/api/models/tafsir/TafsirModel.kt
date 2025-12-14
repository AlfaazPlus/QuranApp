package com.quranapp.android.api.models.tafsir

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant


@Serializable
data class TafsirModel(
    val key: String,
    @SerialName("verse_key")
    val verseKey: String,
    val verses: List<String>,
    val text: String,
    val timestamp: String,
    val version: String?,
) {
    @Transient
    val timestamp1: Long = try {
        Instant.parse(timestamp).toEpochMilli()
    } catch (e: Exception) {
        -1L
    }
}

@Serializable
data class TafsirResponseModel(
    val version: String,
    val tafsirs: List<TafsirModel>,
)