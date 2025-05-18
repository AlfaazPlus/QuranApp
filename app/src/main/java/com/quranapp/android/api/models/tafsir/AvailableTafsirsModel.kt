package com.quranapp.android.api.models.tafsir

import kotlinx.serialization.Serializable

@Deprecated("Use AvailableTafsirsModelV2 instead")
@Serializable
data class AvailableTafsirsModel(
    val tafsirs: Map<String, List<TafsirInfoModel>>
)
