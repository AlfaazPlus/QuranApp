/*
 * Created by Faisal Khan on (c) 16/8/2021.
 */
package com.quranapp.android.api.models.recitation

import com.quranapp.android.compose.utils.appLocale
import kotlinx.serialization.Serializable

@Serializable
data class RecitationInfoModel(
    val style: String?,
    val styleTranslations: Map<String, String> = mapOf(),
) : RecitationInfoBaseModel() {
    fun getStyleName(): String? {
        val locale = appLocale()
        return styleTranslations[locale.toLanguageTag()]
            ?: styleTranslations[locale.language]
            ?: this.style
    }
}
