package com.quranapp.android.components.reader

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp
import com.quranapp.android.utils.reader.atlas.AtlasGlyphPlacement
import com.quranapp.android.utils.reader.atlas.QuranAtlasBundle
import com.quranapp.android.utils.reader.atlas.drawSignal
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private class DrawnGlyph(
    val pageIndex: Int,
    val srcL: Int,
    val srcT: Int,
    val srcR: Int,
    val srcB: Int,
    val dstL: Int,
    val dstT: Int,
    val dstR: Int,
    val dstB: Int,
)

@Composable
fun QuranAtlasText(
    modifier: Modifier = Modifier,
    placements: List<AtlasGlyphPlacement>,
    bundle: QuranAtlasBundle,
    fontSize: TextUnit,
    lineHeight: TextUnit = TextUnit.Unspecified,
    color: Color,
) {
    val density = LocalDensity.current
    val signal = remember(bundle) { bundle.drawSignal() }
    val atlasReady = signal.epoch
    val tint = remember(color) { ColorFilter.tint(color, BlendMode.SrcIn) }

    val layout = remember(placements, bundle, fontSize, lineHeight, density) {
        val fontSizePx = with(density) {
            if (fontSize.isUnspecified) 20.sp.toPx() else fontSize.toPx()
        }

        val unitsPerEm = bundle.meta.font.unitsPerEm
        val fontScale = fontSizePx / unitsPerEm
        val glyphScale = fontSizePx / bundle.layer.ppem

        val totalWidthFu = placements.sumOf { it.xAdvanceFu }
        val totalWidthPx = totalWidthFu * fontScale

        val ascenderFu = bundle.meta.font.ascenderFu
        val descenderFu = bundle.meta.font.descenderFu
        val heightFu = ascenderFu - descenderFu
        val fallbackHeightPx = if (heightFu > 0) heightFu * fontScale else fontSizePx

        val baselineY =
            if (ascenderFu > 0) ascenderFu * fontScale else fallbackHeightPx * 0.8f

        val prepared = ArrayList<DrawnGlyph>(placements.size)
        var currentX = 0f
        var minY = Float.POSITIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY

        for (p in placements) {
            val glyph = bundle.glyphFor(p.gid) ?: run {
                currentX += p.xAdvanceFu.toFloat() * fontScale
                continue
            }

            val x = (currentX + p.xOffsetFu * fontScale + glyph.bearingX * glyphScale).toFloat()
            val y = (baselineY - p.yOffsetFu * fontScale - glyph.bearingY * glyphScale).toFloat()
            val dstW = (glyph.w * glyphScale).roundToInt().coerceAtLeast(1)
            val dstH = (glyph.h * glyphScale).roundToInt().coerceAtLeast(1)
            val dstL = x.roundToInt()
            val dstT = y.roundToInt()

            prepared.add(
                DrawnGlyph(
                    pageIndex = glyph.textureIndex,
                    srcL = glyph.x,
                    srcT = glyph.y,
                    srcR = glyph.x + glyph.w,
                    srcB = glyph.y + glyph.h,
                    dstL = dstL,
                    dstT = dstT,
                    dstR = dstL + dstW,
                    dstB = dstT + dstH,
                )
            )

            val bottom = y + glyph.h * glyphScale
            minY = min(minY, y)
            maxY = max(maxY, bottom)
            currentX += p.xAdvanceFu.toFloat() * fontScale
        }

        val (tightMinY, tightHeightPx) = if (prepared.isEmpty()) {
            0f to fallbackHeightPx
        } else {
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

        object {
            val totalWidthPx = totalWidthPx
            val boxHeightPx = boxHeightPx
            val verticalInsetPx = verticalInsetPx
            val tightMinY = tightMinY
            val prepared = prepared
        }
    }

    Canvas(
        modifier = modifier
            .width(with(density) { layout.totalWidthPx.roundToInt().toDp() })
            .height(with(density) { layout.boxHeightPx.toDp() })
    ) {
        if (atlasReady < 0 || layout.prepared.isEmpty()) return@Canvas

        val onlyPage = bundle.onlyTextureIndex
        val sharedPage = if (onlyPage != null) bundle.cachedBitmap(onlyPage) else null
        val sharedImage = sharedPage?.let(signal::imageOf)
        val shiftY = (layout.verticalInsetPx - layout.tightMinY).roundToInt()
        val glyphs = layout.prepared

        for (i in glyphs.indices) {
            val glyph = glyphs[i]
            val page = pageFor(sharedPage, onlyPage, bundle, glyph.pageIndex) ?: continue
            val srcW = glyph.srcR - glyph.srcL
            val srcH = glyph.srcB - glyph.srcT

            if (
                glyph.srcL < 0 ||
                glyph.srcT < 0 ||
                glyph.srcR > page.width ||
                glyph.srcB > page.height ||
                srcW <= 0 ||
                srcH <= 0
            ) {
                continue
            }

            val image = if (page === sharedPage && sharedImage != null) {
                sharedImage
            } else {
                signal.imageOf(page)
            }

            drawImage(
                image = image,
                srcOffset = IntOffset(glyph.srcL, glyph.srcT),
                srcSize = IntSize(srcW, srcH),
                dstOffset = IntOffset(glyph.dstL, glyph.dstT + shiftY),
                dstSize = IntSize(glyph.dstR - glyph.dstL, glyph.dstB - glyph.dstT),
                filterQuality = FilterQuality.Medium,
                colorFilter = tint,
            )
        }
    }
}

private fun pageFor(
    shared: Bitmap?,
    onlyPage: Int?,
    bundle: QuranAtlasBundle,
    index: Int,
): Bitmap? {
    if (shared != null && index == onlyPage) return shared
    return bundle.cachedBitmap(index)
}
