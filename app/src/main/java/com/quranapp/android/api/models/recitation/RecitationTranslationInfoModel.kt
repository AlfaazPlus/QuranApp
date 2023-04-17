/*
 * Created by Faisal Khan on (c) 16/8/2021.
 */
package com.quranapp.android.api.models.recitation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class RecitationTranslationInfoModel(
    @SerialName("lang-code") val langCode: String,
    @SerialName("lang-name") var langName: String,
    val book: String?,
) : RecitationInfoBaseModel()
