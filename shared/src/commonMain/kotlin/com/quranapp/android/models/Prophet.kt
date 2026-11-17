package com.quranapp.android.models

import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.prophet_adam
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.compose.resources.DrawableResource
import java.text.MessageFormat

@Serializable
data class Prophet(
    val order: Int = 0,
    val nameAr: String,
    val nameEn: String,
    val name: String,
    val honorific: String,
    @Transient
    val icon: DrawableResource = Res.drawable.prophet_adam,
    val thumbnail: String?
) {
    var references: String? = null

    /**
     * To display in recycler view
     */
    var inChapters: String? = null
    var chapters: Set<Int> = setOf()

    /**
     * Item format -> chapNo:VERSE or chapNo:fromVERSE-toVERSE
     */
    var verses: Set<String> = setOf()
    override fun toString(): String {
        return MessageFormat.format(
            "Prophet: {0} ({1}) {2} : [order={3}, iconRes={4}]", name, nameEn,
            honorific, order
        )
    }
}
