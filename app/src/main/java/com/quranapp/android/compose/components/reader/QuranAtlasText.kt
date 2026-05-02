package com.quranapp.android.compose.components.reader

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp
import com.quranapp.android.utils.reader.atlas.AtlasGlyphJson
import com.quranapp.android.utils.reader.atlas.AtlasGlyphPlacement
import com.quranapp.android.utils.reader.atlas.QuranAtlasBundle
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private data class AtlasPreparedGlyph(
    val x: Float,
    val y: Float,
    val glyph: AtlasGlyphJson,
)

@Composable
fun QuranAtlasText(
    placements: List<AtlasGlyphPlacement>,
    bundle: QuranAtlasBundle,
    fontSize: TextUnit,
    lineHeight: TextUnit = TextUnit.Unspecified,
    color: Color,
    modifier: Modifier = Modifier
) {
    val currentPlacements = placements

    val density = LocalDensity.current
    val fontSizePx = with(density) {
        if (fontSize.isUnspecified) 20.sp.toPx() else fontSize.toPx()
    }

    val unitsPerEm = bundle.meta.font.unitsPerEm
    val fontScale = fontSizePx / unitsPerEm
    val glyphScale = fontSizePx / bundle.layer.ppem

    val totalWidthFu = currentPlacements.sumOf { it.xAdvanceFu }
    val totalWidthPx = totalWidthFu * fontScale

    val ascenderFu = bundle.meta.font.ascenderFu
    val descenderFu = bundle.meta.font.descenderFu
    val heightFu = ascenderFu - descenderFu
    val fallbackHeightPx = if (heightFu > 0) heightFu * fontScale else fontSizePx

    val baselineY =
        if (ascenderFu > 0) ascenderFu * fontScale else fallbackHeightPx * 0.8f

    val prepared = ArrayList<AtlasPreparedGlyph>(currentPlacements.size)
    var currentX = 0f

    for (p in currentPlacements) {
        val glyph = bundle.layer.glyphs[p.gid.toString()]

        if (glyph != null) {
            val x = currentX + p.xOffsetFu * fontScale + glyph.bearingX * glyphScale
            val y = baselineY - p.yOffsetFu * fontScale - glyph.bearingY * glyphScale
            prepared.add(AtlasPreparedGlyph(x.toFloat(), y.toFloat(), glyph))
        }

        currentX += p.xAdvanceFu.toFloat() * fontScale
    }

    val (tightMinY, tightHeightPx) = if (prepared.isEmpty()) {
        0f to fallbackHeightPx
    } else {
        var minY = Float.POSITIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        for (d in prepared) {
            val top = d.y
            val bottom = d.y + d.glyph.h * glyphScale
            minY = min(minY, top)
            maxY = max(maxY, bottom)
        }
        minY to (maxY - minY).coerceAtLeast(1f)
    }

    val lineHeightPx = with(density) {
        if (!lineHeight.isUnspecified) lineHeight.toPx() else Float.NaN
    }

    val boxHeightPx = when {
        prepared.isEmpty() -> fallbackHeightPx
        !lineHeightPx.isNaN() -> max(lineHeightPx, tightHeightPx)
        else -> tightHeightPx
    }

    val verticalInsetPx = ((boxHeightPx - tightHeightPx) / 2f).coerceAtLeast(0f)

    Canvas(
        modifier = modifier
            .width(with(density) { totalWidthPx.roundToInt().toDp() })
            .height(with(density) { boxHeightPx.toDp() })
    ) {
        translate(0f, verticalInsetPx - tightMinY) {
            val colorFilter = ColorFilter.tint(color)
            for (d in prepared) {
                val g = d.glyph
                drawImage(
                    image = bundle.bitmap,
                    srcOffset = IntOffset(g.x, g.y),
                    srcSize = IntSize(g.w, g.h),
                    dstOffset = IntOffset(d.x.roundToInt(), d.y.roundToInt()),
                    dstSize = IntSize(
                        (g.w * glyphScale).roundToInt(),
                        (g.h * glyphScale).roundToInt()
                    ),
                    colorFilter = colorFilter
                )
            }
        }
    }
}
