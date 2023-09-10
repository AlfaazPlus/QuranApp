package com.quranapp.android.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppUrls(
    @SerialName("privacy-policy") val privacyPolicy: String,
    val about: String,
    val help: String,
    val feedback: String,
    val discord: String,
)
