package com.quranapp.android.compose.components.reader

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.quranapp.android.db.entities.quran.AyahWordEntity

/**
 * Word bounds as packed floats. Position updates invalidate draw at most once
 * until the next draw, and highlight merge reuses one scratch buffer.
 */
internal class RecitationWordBounds(val wordCount: Int) {
    private val coords = FloatArray(wordCount * 4)
    private val placed = BooleanArray(wordCount)
    private val scratch = FloatArray(wordCount * 4)

    var scratchCount = 0
        private set

    var drawVersion by mutableIntStateOf(0)
        private set

    private var dirty = false

    fun update(index: Int, left: Float, top: Float, right: Float, bottom: Float) {
        val offset = index * 4
      
        if (placed[index] &&
            coords[offset] == left &&
            coords[offset + 1] == top &&
            coords[offset + 2] == right &&
            coords[offset + 3] == bottom
        ) {
            return
        }

        coords[offset] = left
        coords[offset + 1] = top
        coords[offset + 2] = right
        coords[offset + 3] = bottom
        placed[index] = true

        if (!dirty) {
            dirty = true
            drawVersion++
        }
    }

    fun consumeDirty() {
        dirty = false
    }

    fun fill(
        words: List<AyahWordEntity>,
        ayahId: Int,
        wordIndex: Int,
        splitRows: Boolean,
    ) {
        scratchCount = 0
        if (ayahId < 0 || words.isEmpty()) return

        val count = words.size
        var i = 0

        while (i < count) {
            if (!matches(words[i], ayahId, wordIndex) || !placed[i]) {
                i++
                continue
            }

            val start = i * 4
            var left = coords[start]
            var top = coords[start + 1]
            var right = coords[start + 2]
            var bottom = coords[start + 3]
         
            var j = i + 1

            while (j < count) {
                if (!matches(words[j], ayahId, wordIndex) || !placed[j]) break

                val next = j * 4
                val nextLeft = coords[next]
                val nextTop = coords[next + 1]
                val nextRight = coords[next + 2]
                val nextBottom = coords[next + 3]

                if (splitRows && !sameRow(top, bottom, nextTop, nextBottom)) break

                if (nextLeft < left) left = nextLeft
                if (nextTop < top) top = nextTop
                if (nextRight > right) right = nextRight
                if (nextBottom > bottom) bottom = nextBottom
             
                j++
            }

            val out = scratchCount * 4
           
            scratch[out] = left
            scratch[out + 1] = top
            scratch[out + 2] = right
            scratch[out + 3] = bottom
            scratchCount++
           
            i = j
        }
    }

    fun left(index: Int): Float = scratch[index * 4]

    fun top(index: Int): Float = scratch[index * 4 + 1]

    fun right(index: Int): Float = scratch[index * 4 + 2]

    fun bottom(index: Int): Float = scratch[index * 4 + 3]
}

internal fun DrawScope.drawRecitationHighlights(
    bounds: RecitationWordBounds,
    words: List<AyahWordEntity>,
    ayahId: Int,
    wordIndex: Int,
    splitRows: Boolean,
    verseColor: Color,
    wordColor: Color,
    corner: CornerRadius,
) {
    bounds.drawVersion
    bounds.consumeDirty()
    if (ayahId < 0) return

    bounds.fill(words, ayahId, wordIndex = -1, splitRows = splitRows)
    drawScratch(bounds, verseColor, corner)

    if (wordIndex < 0) return

    bounds.fill(words, ayahId, wordIndex, splitRows)
    drawScratch(bounds, wordColor, corner)
}

private fun DrawScope.drawScratch(
    bounds: RecitationWordBounds,
    color: Color,
    corner: CornerRadius,
) {
    for (index in 0 until bounds.scratchCount) {
        val left = bounds.left(index)
        val top = bounds.top(index)
        val right = bounds.right(index)
        val bottom = bounds.bottom(index)
       
        drawRoundRect(
            color = color,
            topLeft = Offset(left, top),
            size = Size(right - left, bottom - top),
            cornerRadius = corner,
        )
    }
}

private fun matches(word: AyahWordEntity, ayahId: Int, wordIndex: Int): Boolean {
    if (word.ayahId != ayahId) return false
    return wordIndex < 0 || word.wordIndex == wordIndex
}

private fun sameRow(top: Float, bottom: Float, nextTop: Float, nextBottom: Float): Boolean {
    val center = (top + bottom) * 0.5f
    val nextCenter = (nextTop + nextBottom) * 0.5f
  
    val height = bottom - top
    val nextHeight = nextBottom - nextTop
  
    val limit = if (height > nextHeight) height else nextHeight
    val delta = if (center > nextCenter) center - nextCenter else nextCenter - center
  
    return delta < limit * 0.5f
}
