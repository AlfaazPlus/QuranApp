package com.quranapp.android.api.models.tafsir

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TafsirModel(
    val version: String,
    val key: String,
    val slug: String,
    @SerialName("verse_key")
    val verseKey: String,
    val verses: List<String>,
    val text: String,
    val timestamp: String,
)