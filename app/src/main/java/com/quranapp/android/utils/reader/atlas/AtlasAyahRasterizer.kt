package com.quranapp.android.utils.reader.atlas

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import androidx.compose.ui.graphics.asAndroidBitmap
import com.quranapp.android.db.entities.quran.AyahWordEntity
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private data class WordGlyphLayout(
    val widthPx: Float,
    val glyphScale: Float,
    val prepared: List<AtlasPreparedGlyphRaster>,
    val tightMinY: Float,
    val tightHeightPx: Float,
)

private data class AtlasPreparedGlyphRaster(
    val x: Float,
    val y: Float,
    val glyph: AtlasGlyphJson,
)

object AtlasAyahRasterizer {

    /**
     * @param maxLineWidthPx when > 0, wraps words onto multiple lines (avoids ultra-wide bitmaps).
     */
    fun renderAyahToBitmap(
        bundle: QuranAtlasBundle,
        words: List<AyahWordEntity>,
        placementsByWord: Map<Int, List<AtlasGlyphPlacement>>,
        fontSizePx: Float,
        lineHeightPx: Float,
        argbColor: Int,
        wordGapPx: Float,
        maxLineWidthPx: Float = 0f,
    ): Bitmap? {
        if (words.isEmpty()) return null

        val unitsPerEm = bundle.meta.font.unitsPerEm
        val fontScale = fontSizePx / unitsPerEm
        val glyphScale = fontSizePx / bundle.layer.ppem

        val ascenderFu = bundle.meta.font.ascenderFu
        val descenderFu = bundle.meta.font.descenderFu
        val heightFu = ascenderFu - descenderFu
        val fallbackHeightPx = if (heightFu > 0) heightFu * fontScale else fontSizePx
        val baselineY =
            if (ascenderFu > 0) ascenderFu * fontScale else fallbackHeightPx * 0.8f

        val wordLayouts = ArrayList<WordGlyphLayout>(words.size)

        for (word in words) {
            wordLayouts.add(
                layoutWord(
                    bundle = bundle,
                    word = word,
                    placements = placementsByWord.getForWord(word) ?: emptyList(),
                    fontScale = fontScale,
                    glyphScale = glyphScale,
                    baselineY = baselineY,
                    fallbackHeightPx = fallbackHeightPx,
                    fontSizePx = fontSizePx,
                )
            )
        }

        val lines = if (maxLineWidthPx > 0f) {
            wrapWordLayoutsToLines(wordLayouts, wordGapPx, maxLineWidthPx)
        } else {
            listOf(wordLayouts)
        }

        fun lineContentWidth(line: List<WordGlyphLayout>): Float {
            if (line.isEmpty()) return 0f
            var s = 0f
            for (w in line) {
                s += w.widthPx
            }
            return s + (line.size - 1).coerceAtLeast(0) * wordGapPx
        }

        val interLineGapPx = (lineHeightPx * 0.2f).coerceAtLeast(wordGapPx * 0.5f)

        val lineMetrics = lines.map { line ->
            val contentW = lineContentWidth(line)
            val maxTight = line.maxOfOrNull { it.tightHeightPx } ?: 0f
            val lbH = max(lineHeightPx, maxTight).coerceAtLeast(1f)
            Triple(line, contentW, lbH)
        }

        val bitmapW = lineMetrics.maxOf { it.second }.roundToInt().coerceAtLeast(1)
        var totalHeight = 0f

        for ((_, _, lbH) in lineMetrics) {
            totalHeight += lbH
        }

        if (lineMetrics.size > 1) {
            totalHeight += interLineGapPx * (lineMetrics.size - 1)
        }

        val bitmapH = totalHeight.roundToInt().coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(bitmapW, bitmapH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val androidAtlas = bundle.bitmap.asAndroidBitmap()

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = PorterDuffColorFilter(argbColor, PorterDuff.Mode.SRC_IN)
        }

        var yLine = 0f

        for ((lineIndex, triple) in lineMetrics.withIndex()) {
            val (line, contentW, lbH) = triple
            val startX = (bitmapW - contentW) / 2f
            var cursorX = startX + contentW

            for ((i, layout) in line.withIndex()) {
                cursorX -= layout.widthPx

                val verticalInset = ((lbH - layout.tightHeightPx) / 2f).coerceAtLeast(0f)
                val dy = yLine + verticalInset - layout.tightMinY

                for (g in layout.prepared) {
                    val glyph = g.glyph
                    val dstLeft = (cursorX + g.x).roundToInt()
                    val dstTop = (g.y + dy).roundToInt()
                    val dstW = (glyph.w * layout.glyphScale).roundToInt().coerceAtLeast(1)
                    val dstH = (glyph.h * layout.glyphScale).roundToInt().coerceAtLeast(1)

                    canvas.drawBitmap(
                        androidAtlas,
                        Rect(glyph.x, glyph.y, glyph.x + glyph.w, glyph.y + glyph.h),
                        Rect(dstLeft, dstTop, dstLeft + dstW, dstTop + dstH),
                        paint,
                    )
                }

                if (i != line.lastIndex) {
                    cursorX -= wordGapPx
                }
            }

            yLine += lbH
            if (lineIndex != lineMetrics.lastIndex) {
                yLine += interLineGapPx
            }
        }

        return bitmap
    }

    fun renderAyahToPng(
        bundle: QuranAtlasBundle,
        words: List<AyahWordEntity>,
        placementsByWord: Map<Int, List<AtlasGlyphPlacement>>,
        fontSizePx: Float,
        lineHeightPx: Float,
        argbColor: Int,
        wordGapPx: Float,
        maxLineWidthPx: Float = 0f,
    ): ByteArray? {
        val bitmap = renderAyahToBitmap(
            bundle,
            words,
            placementsByWord,
            fontSizePx,
            lineHeightPx,
            argbColor,
            wordGapPx,
            maxLineWidthPx,
        ) ?: return null

        val out = ByteArrayOutputStream(bitmap.width * bitmap.height / 4)

        if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
            bitmap.recycle()
            return null
        }

        bitmap.recycle()

        return out.toByteArray()
    }

    private fun wrapWordLayoutsToLines(
        wordLayouts: List<WordGlyphLayout>,
        wordGapPx: Float,
        maxLineWidthPx: Float,
    ): List<List<WordGlyphLayout>> {
        if (wordLayouts.isEmpty()) return emptyList()

        val lines = ArrayList<ArrayList<WordGlyphLayout>>()
        var cur = ArrayList<WordGlyphLayout>()
        var curW = 0f

        for (layout in wordLayouts) {
            val extra = if (cur.isEmpty()) 0f else wordGapPx
            val need = layout.widthPx + extra

            if (cur.isNotEmpty() && curW + need > maxLineWidthPx) {
                lines.add(cur)
                cur = ArrayList()
                curW = 0f
            }

            cur.add(layout)

            curW = if (cur.size == 1) {
                layout.widthPx
            } else {
                curW + extra + layout.widthPx
            }
        }

        if (cur.isNotEmpty()) {
            lines.add(cur)
        }

        return lines
    }

    private fun layoutWord(
        bundle: QuranAtlasBundle,
        word: AyahWordEntity,
        placements: List<AtlasGlyphPlacement>,
        fontScale: Float,
        glyphScale: Float,
        baselineY: Float,
        fallbackHeightPx: Float,
        fontSizePx: Float,
    ): WordGlyphLayout {
        val prepared = ArrayList<AtlasPreparedGlyphRaster>(placements.size)
        var currentX = 0f

        for (p in placements) {
            val glyph = bundle.layer.glyphs[p.gid.toString()]

            if (glyph != null) {
                val x = currentX + p.xOffsetFu * fontScale + glyph.bearingX * glyphScale
                val y = baselineY - p.yOffsetFu * fontScale - glyph.bearingY * glyphScale

                prepared.add(AtlasPreparedGlyphRaster(x.toFloat(), y.toFloat(), glyph))
            }

            currentX += p.xAdvanceFu.toFloat() * fontScale
        }

        val widthPx = if (prepared.isEmpty()) {
            max(fontSizePx * 0.35f * word.text.length.coerceAtLeast(1), fontSizePx * 0.4f)
        } else {
            currentX
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

        return WordGlyphLayout(
            widthPx = widthPx,
            glyphScale = glyphScale,
            prepared = prepared,
            tightMinY = tightMinY,
            tightHeightPx = tightHeightPx,
        )
    }
}
