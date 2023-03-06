package com.quranapp.android.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecitationsCommonUrlInfoModel(
    @SerialName("common-host") val commonHost: String
)
