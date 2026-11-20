package com.quranapp.android.utils.reader.atlas

import com.quranapp.android.db.entities.quran.AyahWordEntity
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class AtlasPreparedGlyph(
    val x: Float,
    val y: Float,
    val glyph: AtlasGlyphJson,
)

private data class WordGlyphLayout(
    val widthPx: Float,
    val glyphScale: Float,
    val prepared: List<AtlasPreparedGlyph>,
    val tightMinY: Float,
    val tightHeightPx: Float,
)

/**
 * A software rasterizer for rendering Quran Ayahs from a texture atlas.
 *
 * Drawing goes through a platform bitmap surface, so it runs on the CPU.
 *
 * Use cases:
 * - Static widgets (e.g., VotdWidgetReceiver)
 * - Image sharing/export (e.g., QuranVerseWebHtml)
 *
 * AVOID using this in the main UI reader or scrollable lists, as it will cause jank and high
 * memory pressure. For the main UI, use QuranAtlasText which is hardware-accelerated.
 */
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
    ): AtlasBitmap? {
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
        val bitmap = createAtlasBitmap(bitmapW, bitmapH)
        val surface = bitmap.atlasSurface(argbColor)

        var yLine = 0f

        val singleTexture = if (bundle.textureCount == 1) bundle.singleAndroidTexture()
        else null

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

                    val texture = singleTexture ?: bundle.androidTextureForGlyph(glyph)

                    if (texture != null && !texture.isRecycled()) {
                        surface.drawGlyph(
                            source = texture,
                            srcLeft = glyph.x,
                            srcTop = glyph.y,
                            srcRight = glyph.x + glyph.w,
                            srcBottom = glyph.y + glyph.h,
                            dstLeft = dstLeft,
                            dstTop = dstTop,
                            dstRight = dstLeft + dstW,
                            dstBottom = dstTop + dstH,
                        )
                    }
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

        val encoded = bitmap.encodePng()
        bitmap.recycleAtlas()

        return encoded
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
        val prepared = ArrayList<AtlasPreparedGlyph>(placements.size)
        var currentX = 0f

        for (p in placements) {
            val glyph = bundle.glyphFor(p.gid)

            if (glyph != null) {
                val x = currentX + p.xOffsetFu * fontScale + glyph.bearingX * glyphScale
                val y = baselineY - p.yOffsetFu * fontScale - glyph.bearingY * glyphScale

                prepared.add(AtlasPreparedGlyph(x.toFloat(), y.toFloat(), glyph))
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
