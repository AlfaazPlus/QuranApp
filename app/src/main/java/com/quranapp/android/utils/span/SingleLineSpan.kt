package com.quranapp.android.utils.span

import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.ReplacementSpan

class SingleLineSpan : ReplacementSpan() {
    override fun getSize(paint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int {
        return paint.measureText(text.subSequence(start, end).toString()).toInt()
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        // Draw the text without wrapping
        canvas.drawText(text.subSequence(start, end).toString(), x, y.toFloat(), paint)
    }
}