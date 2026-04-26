package com.quranapp.android.utils.reader

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alfaazplus.sunnah.ui.theme.appFontFamily
import com.alfaazplus.sunnah.ui.theme.fontUrdu
import com.quranapp.android.compose.components.reader.MushafLineLayout
import com.quranapp.android.db.entities.quran.AyahWordEntity
import com.quranapp.android.utils.extensions.getDimension
import com.quranapp.android.utils.univ.StringUtils

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

data class PageBuilderParams(
    val context: Context,
    val textMeasurer: TextMeasurer,
    val colors: ColorScheme,
    val type: Typography,
    val density: Density,
    val contentWidthPx: Int,
) {
    fun toKey(): String {
        return "$contentWidthPx:${density.density}"
    }
}

data class TranslationPageBuilderParams(
    val context: Context,
    val colors: ColorScheme,
    val type: Typography,
    val verseActions: VerseActions,
    val translationSizeMultiplier: Float,
)

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
    val sizeMultiplier: Float,
    val useSmallSize: Boolean = false
)

fun getTranslationTextStyle(
    params: TranslationTextStyleParams,
    baseLineHeightMultiplier: Float = 1.5f
): TextStyle {
    val isRtl = StringUtils.isRtlLanguage(params.slug)
    val isUrdu = TranslUtils.isUrdu(params.slug)
    val resolvedFontSize = 16.sp * params.sizeMultiplier

    return TextStyle(
        textDirection = if (isRtl) TextDirection.Rtl else TextDirection.Ltr,
        fontFamily = if (isUrdu) fontUrdu else appFontFamily,
        platformStyle = PlatformTextStyle(
            includeFontPadding = true
        ),
        fontSize = resolvedFontSize,
        lineHeight = if (isUrdu) resolvedFontSize * 2.5f else resolvedFontSize * baseLineHeightMultiplier,
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
        if (params.useSmallSize) params.script.getQuranScriptVerseTextSizeSmallRes()
        else params.script.getQuranScriptVerseTextSizeMediumRes()
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

fun mushafCappedBaseStyle(base: TextStyle, lineInnerWidthDp: Float): TextStyle {
    val cap = mushafScreenMaxFontScale(lineInnerWidthDp)
    return mushafCappedBaseStyleForScale(base, cap)
}

fun mushafCappedBaseStyleForScale(base: TextStyle, pageScale: Float): TextStyle {
    val cap = pageScale.coerceIn(
        MUSHAF_FONT_SCALE_AT_MIN_WIDTH,
        MUSHAF_FONT_SCALE_AT_MAX_WIDTH
    )
    val scaled = (base.fontSize.value * cap).sp
    return base.copy(
        fontSize = scaled,
        lineHeight = (scaled.value * MUSHAF_LINE_HEIGHT_MULT).sp,
    )
}

/**
 * Scales the dimen-derived mushaf base font by available line width (dp).
 * Narrow phones use a slightly lower ceiling; large screens allow a larger max before shrink-only logic.
 */
private fun mushafScreenMaxFontScale(lineInnerWidthDp: Float): Float {
    val w = lineInnerWidthDp.coerceIn(MUSHAF_FONT_WIDTH_DP_MIN, MUSHAF_FONT_WIDTH_DP_MAX)
    return (MUSHAF_FONT_SCALE_AT_MIN_WIDTH + (w - MUSHAF_FONT_WIDTH_DP_MIN) / (MUSHAF_FONT_WIDTH_DP_MAX - MUSHAF_FONT_WIDTH_DP_MIN) * (MUSHAF_FONT_SCALE_AT_MAX_WIDTH - MUSHAF_FONT_SCALE_AT_MIN_WIDTH)).coerceIn(
        MUSHAF_FONT_SCALE_AT_MIN_WIDTH,
        MUSHAF_FONT_SCALE_AT_MAX_WIDTH
    )
}

fun mushafScaleForWidth(lineInnerWidthDp: Float): Float {
    return mushafScreenMaxFontScale(lineInnerWidthDp)
}


fun fitMushafLineLayout(
    words: List<AyahWordEntity>,
    centered: Boolean,
    cappedBaseStyle: TextStyle,
    maxLineWidthPx: Float,
    lineWidthBounded: Boolean,
    density: Density,
    textMeasurer: TextMeasurer,
): MushafLineLayout {
    if (words.isEmpty()) {
        return MushafLineLayout(
            fittedStyle = cappedBaseStyle,
            centeredGap = 0.dp,
        )
    }

    val baseGapPx =
        with(density) { cappedBaseStyle.fontSize.toPx() * MUSHAF_CENTERED_GAP_FRACTION }
    val minInterWordGapPx =
        with(density) { cappedBaseStyle.fontSize.toPx() * MUSHAF_MIN_INTER_WORD_GAP_FRACTION }

    val baseWidth = measureMushafLineWidth(
        words = words,
        centered = centered,
        textMeasurer = textMeasurer,
        style = cappedBaseStyle,
        centeredGapPx = baseGapPx,
        minInterWordGapPx = minInterWordGapPx,
    ).coerceAtLeast(1f)

    val shrinkOnly = when {
        !lineWidthBounded -> 1f
        baseWidth <= maxLineWidthPx -> 1f
        else -> (maxLineWidthPx / baseWidth).coerceAtLeast(MUSHAF_LINE_SHRINK_MIN)
    }

    val fittedSp = (cappedBaseStyle.fontSize.value * shrinkOnly).sp
    return MushafLineLayout(
        fittedStyle = cappedBaseStyle.copy(
            fontSize = fittedSp,
            lineHeight = (fittedSp.value * MUSHAF_LINE_HEIGHT_MULT).sp,
        ),
        centeredGap = with(density) {
            maxOf(baseGapPx, minInterWordGapPx).times(shrinkOnly).toDp()
        },
    )
}

private fun measureMushafLineWidth(
    words: List<AyahWordEntity>,
    centered: Boolean,
    textMeasurer: TextMeasurer,
    style: TextStyle,
    centeredGapPx: Float,
    minInterWordGapPx: Float,
): Float {
    var sum = 0f

    for (word in words) {
        sum += textMeasurer.measure(
            text = AnnotatedString(word.text),
            style = style,
            softWrap = false,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        ).size.width
    }

    if (words.size > 1) {
        val gapPerPair = if (centered) {
            maxOf(centeredGapPx, minInterWordGapPx)
        } else {
            minInterWordGapPx
        }
        sum += gapPerPair * (words.size - 1)
    }

    return sum
}

fun measureMushafLineWidthForStyle(
    words: List<AyahWordEntity>,
    centered: Boolean,
    textMeasurer: TextMeasurer,
    style: TextStyle,
    centeredGapPx: Float,
    minInterWordGapPx: Float,
): Float {
    return measureMushafLineWidth(
        words = words,
        centered = centered,
        textMeasurer = textMeasurer,
        style = style,
        centeredGapPx = centeredGapPx,
        minInterWordGapPx = minInterWordGapPx,
    )
}


private const val MUSHAF_LINE_HEIGHT_MULT = 1.8f
private const val MUSHAF_FONT_WIDTH_DP_MIN = 260f
const val MUSHAF_FONT_WIDTH_DP_MAX = 720f

val MUSHAF_PAGE_HORIZONTAL_PADDING = 12.dp
const val MUSHAF_FONT_SCALE_AT_MIN_WIDTH = 0.85f
const val MUSHAF_FONT_SCALE_AT_MAX_WIDTH = 2f

/** Inter-word gap as a fraction of font size for centered mushaf lines. */
const val MUSHAF_CENTERED_GAP_FRACTION = 0.22f

/** Minimum gap between adjacent words (all lines), as a fraction of font size — avoids overlap when justified. */
const val MUSHAF_MIN_INTER_WORD_GAP_FRACTION = 0.1f

/** When shrinking to fit, do not go below this fraction of the (screen-capped) base size. */
private const val MUSHAF_LINE_SHRINK_MIN = 0.16f