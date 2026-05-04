package com.quranapp.android.compose.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAndroidColorSpace
import androidx.compose.ui.graphics.toArgb
import com.quranapp.android.utils.univ.StringUtils

fun Color.alpha(alpha: Float): Color {
    return copy(alpha = alpha)
}


fun Color.toCssHex(): String =
    StringUtils.formatInvariant("#%06X", 0xFFFFFF and toArgb())

fun Color.toAndroidColor(): android.graphics.Color =
    android.graphics.Color.valueOf(red, green, blue, alpha, colorSpace.toAndroidColorSpace())

fun Color.toCssRgba(): String {
    val r = (red * 255f).toInt().coerceIn(0, 255)
    val g = (green * 255f).toInt().coerceIn(0, 255)
    val b = (blue * 255f).toInt().coerceIn(0, 255)

    return "rgba($r,$g,$b,${alpha})"
}

fun ColorScheme.toCssVariables(): String {
    fun StringBuilder.appendCssColor(cssName: String, color: Color) {
        append("--$cssName:")
        append(color.toCssHex())
        append(';')
    }

    return buildString {
        appendCssColor("primary", primary)
        appendCssColor("on-primary", onPrimary)
        appendCssColor("primary-container", primaryContainer)
        appendCssColor("on-primary-container", onPrimaryContainer)
        appendCssColor("inverse-primary", inversePrimary)
        appendCssColor("secondary", secondary)
        appendCssColor("on-secondary", onSecondary)
        appendCssColor("secondary-container", secondaryContainer)
        appendCssColor("on-secondary-container", onSecondaryContainer)
        appendCssColor("tertiary", tertiary)
        appendCssColor("on-tertiary", onTertiary)
        appendCssColor("tertiary-container", tertiaryContainer)
        appendCssColor("on-tertiary-container", onTertiaryContainer)
        appendCssColor("error", error)
        appendCssColor("on-error", onError)
        appendCssColor("error-container", errorContainer)
        appendCssColor("on-error-container", onErrorContainer)
        appendCssColor("background", background)
        appendCssColor("on-background", onBackground)
        appendCssColor("surface", surface)
        appendCssColor("on-surface", onSurface)
        appendCssColor("surface-variant", surfaceVariant)
        appendCssColor("on-surface-variant", onSurfaceVariant)
        appendCssColor("surface-dim", surfaceDim)
        appendCssColor("surface-bright", surfaceBright)
        appendCssColor("surface-container-lowest", surfaceContainerLowest)
        appendCssColor("surface-container-low", surfaceContainerLow)
        appendCssColor("surface-container", surfaceContainer)
        appendCssColor("surface-container-high", surfaceContainerHigh)
        appendCssColor("surface-container-highest", surfaceContainerHighest)
        appendCssColor("surface-tint", surfaceTint)
        appendCssColor("inverse-surface", inverseSurface)
        appendCssColor("inverse-on-surface", inverseOnSurface)
        appendCssColor("outline", outline)
        appendCssColor("outline-variant", outlineVariant)
        appendCssColor("scrim", scrim)
    }
}
