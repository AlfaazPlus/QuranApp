package com.quranapp.android.api.models.tafsir.v2

import com.quranapp.android.api.models.tafsir.TafsirInfoModel
import kotlinx.serialization.Serializable

@Serializable
data class AvailableTafsirsModelV2(
    val version: String,
    val tafsirs: Map<String, List<TafsirInfoModel>>
)
