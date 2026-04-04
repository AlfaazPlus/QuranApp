package com.quranapp.android.utils.reader

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import com.alfaazplus.sunnah.ui.theme.fontFamily
import com.alfaazplus.sunnah.ui.theme.fontUrdu
import com.quranapp.android.utils.extensions.getDimension

data class TextBuilderParams(
    val context: Context,
    val fontResolver: FontResolver,
    val verseActions: VerseActions,
    val colors: ColorScheme,
    val type: Typography,
    val arabicEnabled: Boolean,
    val script: String,
    val arabicSizeMultiplier: Float,
    val translationSizeMultiplier: Float,
    val slugs: Set<String>,
) {
    fun toKey(): String {
        return "$script,$arabicSizeMultiplier,$translationSizeMultiplier,$slugs"
    }

    override fun toString(): String {
        return "script:$script, arabicSizeMultiplier:$arabicSizeMultiplier, translationSizeMultiplier:$translationSizeMultiplier ,slugs:$slugs"
    }
}

data class TranslationTextStyleParams(
    val slug: String,
    val sizeMultiplier: Float
)

data class QuranTextStyleParams(
    val context: Context,
    val fontResolver: FontResolver,
    val colors: ColorScheme,
    val type: Typography,
    val pageNo: Int,
    val script: String,
    val sizeMultiplier: Float
)

fun getTranslationTextStyle(
    params: TranslationTextStyleParams
): TextStyle {
    val isUrdu = TranslUtils.isUrdu(params.slug)
    val resolvedFontSize = 16.sp * params.sizeMultiplier

    return TextStyle(
        textDirection = if (isUrdu) TextDirection.Rtl else TextDirection.Ltr,
        fontFamily = if (isUrdu) fontUrdu else fontFamily,
        platformStyle = PlatformTextStyle(
            includeFontPadding = true
        ),
        fontSize = resolvedFontSize,
        lineHeight = if (isUrdu) resolvedFontSize * 2.5f else resolvedFontSize * 1.5,
    )
}

fun getQuranTextStyle(
    params: QuranTextStyleParams,
): TextStyle {
    val context = params.context

    val density = Density(
        density = context.resources.displayMetrics.density,
        fontScale = context.resources.configuration.fontScale
    )

    val basePx = context.getDimension(
        params.script.getQuranScriptVerseTextSizeMediumRes()
    )
    val fontSize = with(density) { (basePx * params.sizeMultiplier).toSp() }

    return params.type.headlineSmall.copy(
        fontFamily = params.fontResolver.fontFamily(params.script, params.pageNo),
        fontSize = fontSize,
        color = params.colors.onBackground,
        textDirection = TextDirection.Rtl,
        lineHeight = fontSize * 1.8f
    )
}