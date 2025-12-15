/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * All rights reserved.
 */

package com.peacedesign.android.utils.span;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.style.ReplacementSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class FootnoteRefSpan extends ReplacementSpan {
    private final int mTextColor;
    private final float mSizeScale;

    public FootnoteRefSpan(int textColor, float sizeScale) {
        mTextColor = textColor;
        mSizeScale = sizeScale;
    }

    public FootnoteRefSpan(int textColor) {
        this(textColor, 0.8f);
    }

    @Override
    public int getSize(@NonNull Paint paint, CharSequence text, int start, int end, @Nullable Paint.FontMetricsInt fm) {
        Paint scaledPaint = getScaledPaint(paint);
        return (int) scaledPaint.measureText(text, start, end);
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, @NonNull Paint paint) {
        Paint scaledPaint = getScaledPaint(paint);
        scaledPaint.setColor(mTextColor);

        float superscriptOffset = scaledPaint.ascent() / 3;

        canvas.drawText(text, start, end, x, y + superscriptOffset, scaledPaint);
    }

    private Paint getScaledPaint(Paint paint) {
        Paint scaledPaint = new Paint(paint);
        scaledPaint.setTextSize(paint.getTextSize() * mSizeScale);
        return scaledPaint;
    }
}

