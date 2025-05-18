package com.quranapp.android.api.models.tafsir.v2

import kotlinx.serialization.SerialName

@kotlinx.serialization.Serializable
data class TafsirModelV2(
    val version: String,
    val key: String,
    val slug: String,
    @SerialName("verse_key")
    val verseKey: String,
    val verses: List<String>,
    val text: String,
    val timestamp: String,
)