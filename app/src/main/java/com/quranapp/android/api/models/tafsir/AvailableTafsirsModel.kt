package com.quranapp.android.api.models.tafsir

import com.quranapp.android.api.models.tafsir.TafsirInfoModel
import kotlinx.serialization.Serializable

@Serializable
data class AvailableTafsirsModel(
    val tafsirs: Map<String, List<TafsirInfoModel>>
)
