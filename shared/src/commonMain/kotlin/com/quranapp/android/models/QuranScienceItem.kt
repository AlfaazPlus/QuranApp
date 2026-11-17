package com.quranapp.android.models

import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.ic_science_physics
import com.quranapp.android.locale.appFallbackLanguageCodes
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.compose.resources.DrawableResource

@Serializable
data class QuranScienceItem(
    private val title: String,
    val referencesCount: Int,
    val path: String,
    @Transient
    val drawableRes: DrawableResource = Res.drawable.ic_science_physics,
    val translations: Map<String, String>
) {
    fun getTitle(): String {
        return appFallbackLanguageCodes().firstNotNullOfOrNull {
            translations.get(it)
        } ?: title
    }
}
