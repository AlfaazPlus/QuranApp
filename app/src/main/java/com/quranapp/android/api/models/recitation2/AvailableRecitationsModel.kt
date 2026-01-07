package com.quranapp.android.api.models.recitation2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AvailableRecitationsModel(
    @SerialName("reciters") val reciters: List<RecitationQuranModel>
)
