package com.quranapp.android.compose.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.quranapp.android.utils.univ.StringUtils

fun Color.alpha(alpha: Float): Color {
    return copy(alpha = alpha)
}


fun Color.toCssHex(): String =
    StringUtils.formatInvariant("#%06X", 0xFFFFFF and toArgb())

fun Color.toCssRgba(): String {
    val r = (red * 255f).toInt().coerceIn(0, 255)
    val g = (green * 255f).toInt().coerceIn(0, 255)
    val b = (blue * 255f).toInt().coerceIn(0, 255)

    return "rgba($r,$g,$b,${alpha})"
}
