package com.quranapp.android.components.quran

import com.quranapp.android.locale.appFallbackLanguageCodes
import java.io.Serializable

data class QuranScienceItem(
    private val title: String,
    val referencesCount: Int,
    val path: String,
    val drawableRes: Int,
    val translations: Map<String, String>
) : Serializable {
    fun getTitle(): String {
        return appFallbackLanguageCodes().firstNotNullOfOrNull {
            translations.get(it)
        } ?: title
    }
}
