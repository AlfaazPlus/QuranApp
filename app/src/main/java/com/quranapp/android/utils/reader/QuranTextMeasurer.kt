package com.quranapp.android.utils.reader

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp
import com.quranapp.android.compose.components.reader.MushafLineLayout
import com.quranapp.android.db.entities.quran.AyahWordEntity
import com.quranapp.android.db.entities.quran.MushafLineType
import com.quranapp.android.db.entities.quran.MushafMapEntity
import com.quranapp.android.utils.reader.atlas.AtlasGlyphPlacement
import com.quranapp.android.utils.reader.atlas.QuranAtlasBundle
import com.quranapp.android.utils.reader.atlas.getForWord

class QuranTextMeasurer(
    val bundle: QuranAtlasBundle?,
    private val textMeasurer: TextMeasurer?,
    private val density: Density
) {

    fun computeMushafPageScale(
        rows: List<MushafMapEntity>,
        wordsByLineNo: Map<Int, List<AyahWordEntity>>,
        atlasPlacements: Map<Int, Map<Int, List<AtlasGlyphPlacement>>>,
        baseStyle: TextStyle,
        params: PageBuilderParams,
        fallbackScale: Float,
    ): Float {
        val contentWidthPx = params.contentWidthPx.toFloat().coerceAtLeast(1f)
        val density = params.uiConfig.density

        val centeredGapPx =
            with(density) { baseStyle.fontSize.toPx() * MUSHAF_CENTERED_GAP_FRACTION }
        val minInterWordGapPx =
            with(density) { baseStyle.fontSize.toPx() * MUSHAF_MIN_INTER_WORD_GAP_FRACTION }

        val wideLineRatios = ArrayList<Float>(rows.size)
        for (row in rows) {
            if (row.lineType != MushafLineType.ayah) continue
            val words = wordsByLineNo[row.lineNumber].orEmpty()
            if (words.isEmpty()) continue

            val measuredWidth = measureMushafLineWidth(
                words = words,
                atlasPlacements = atlasPlacements[row.lineNumber] ?: emptyMap(),
                centered = row.isCentered,
                style = baseStyle,
                centeredGapPx = centeredGapPx,
                minInterWordGapPx = minInterWordGapPx,
            ).coerceAtLeast(1f)

            val fillRatio = measuredWidth / contentWidthPx

            if (!row.isCentered && fillRatio >= 0.82f) {
                wideLineRatios.add((contentWidthPx / measuredWidth).coerceAtLeast(0f))
            }
        }

        if (wideLineRatios.isEmpty()) return fallbackScale

        val sorted = wideLineRatios.sorted()
        val middle = sorted.size / 2
        val median = if (sorted.size % 2 == 0) {
            (sorted[middle - 1] + sorted[middle]) / 2f
        } else {
            sorted[middle]
        }

        val conservativeCap = minOf(fallbackScale, MUSHAF_FONT_SCALE_AT_MAX_WIDTH)
        return median.coerceIn(MUSHAF_FONT_SCALE_AT_MIN_WIDTH, conservativeCap)
    }

    fun fitMushafLineLayout(
        words: List<AyahWordEntity>,
        atlasPlacements: Map<Int, List<AtlasGlyphPlacement>>,
        centered: Boolean,
        cappedBaseStyle: TextStyle,
        maxLineWidthPx: Float,
        lineWidthBounded: Boolean,
        density: Density,
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
            atlasPlacements = atlasPlacements,
            centered = centered,
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
        atlasPlacements: Map<Int, List<AtlasGlyphPlacement>>,
        centered: Boolean,
        style: TextStyle,
        centeredGapPx: Float,
        minInterWordGapPx: Float,
    ): Float {
        var sum = 0f

        for (word in words) {
            sum += measureWidth(word.text, atlasPlacements.getForWord(word), style)
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

    fun measureWidth(
        wordText: String,
        placements: List<AtlasGlyphPlacement>?,
        style: TextStyle
    ): Float {
        if (placements != null && bundle != null) {
            val fontSizePx = with(density) {
                if (style.fontSize.isUnspecified) 20.sp.toPx() else style.fontSize.toPx()
            }

            val fontScale = fontSizePx / bundle.meta.font.unitsPerEm

            val totalWidthFu = placements.sumOf { it.xAdvanceFu }

            return (totalWidthFu * fontScale).toFloat()
        }

        return textMeasurer?.measure(
            text = AnnotatedString(wordText),
            style = style,
            softWrap = false,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )?.size?.width?.toFloat() ?: 0f
    }
}
