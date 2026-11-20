package com.quranapp.android.components.collections

import androidx.compose.ui.graphics.Color
import com.quranapp.android.generated.resources.Res
import com.quranapp.android.generated.resources.col_book_open
import com.quranapp.android.generated.resources.col_bookmark
import com.quranapp.android.generated.resources.col_brain
import com.quranapp.android.generated.resources.col_folder_open
import com.quranapp.android.generated.resources.col_graduation
import com.quranapp.android.generated.resources.col_heart
import com.quranapp.android.generated.resources.col_mic_vocal
import com.quranapp.android.generated.resources.col_moon_star
import com.quranapp.android.generated.resources.col_shield_check
import com.quranapp.android.generated.resources.col_sprout
import com.quranapp.android.generated.resources.col_star
import com.quranapp.android.generated.resources.col_sun
import org.jetbrains.compose.resources.DrawableResource

fun collectionColor(hex: String?): Color {
    return parseHexColor(hex) ?: Color(80, 84, 71)
}

fun parseHexColor(hex: String?): Color? {
    val raw = hex?.trim()?.removePrefix("#")?.takeIf { it.isNotEmpty() } ?: return null
    val value = raw.toLongOrNull(16) ?: return null

    return when (raw.length) {
        6 -> Color(
            red = ((value shr 16) and 0xFF) / 255f,
            green = ((value shr 8) and 0xFF) / 255f,
            blue = (value and 0xFF) / 255f,
        )

        8 -> Color(
            alpha = ((value shr 24) and 0xFF) / 255f,
            red = ((value shr 16) and 0xFF) / 255f,
            green = ((value shr 8) and 0xFF) / 255f,
            blue = (value and 0xFF) / 255f,
        )

        else -> null
    }
}

fun collectionIcon(iconKey: String?): DrawableResource {
    return when (iconKey) {
        "reading" -> Res.drawable.col_book_open
        "growth" -> Res.drawable.col_sprout
        "day" -> Res.drawable.col_sun
        "devotion" -> Res.drawable.col_heart
        "saved" -> Res.drawable.col_bookmark
        "reflection" -> Res.drawable.col_brain
        "night" -> Res.drawable.col_moon_star
        "protection" -> Res.drawable.col_shield_check
        "favorite" -> Res.drawable.col_star
        "learning" -> Res.drawable.col_graduation
        "recitation" -> Res.drawable.col_mic_vocal
        else -> Res.drawable.col_folder_open
    }
}
