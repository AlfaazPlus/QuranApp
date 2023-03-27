package com.quranapp.android.api.models

import com.quranapp.android.components.tafsir.TafsirModel
import kotlinx.serialization.Serializable

@Serializable
data class AvailableTafsirsModel(
    val tafsirs: Map<String, List<TafsirModel>>
)
