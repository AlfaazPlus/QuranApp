/*
 * Created by Faisal Khan on (c) 16/8/2021.
 */
package com.quranapp.android.api.models.recitation

import com.quranapp.android.compose.utils.appFallbackLanguageCodes
import kotlinx.serialization.Serializable

@Serializable
data class RecitationInfoModel(
    val style: String?,
    val styleTranslations: Map<String, String> = mapOf(),
) : RecitationInfoBaseModel() {
    fun getStyleName(): String? {
        return appFallbackLanguageCodes()
            .firstNotNullOfOrNull { styleTranslations[it] }
            ?: this.reciter
    }
}
