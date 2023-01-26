package com.quranapp.android.utils.univ;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.style.LineBackgroundSpan;

public class PaddingBackgroundColorSpan implements LineBackgroundSpan {
    private final int mBackgroundColor;
    private final int mPadding;
    private final Rect mBgRect;

    public PaddingBackgroundColorSpan(int backgroundColor, int padding) {
        mBackgroundColor = backgroundColor;
        mPadding = padding;
        // Precreate rect for performance
        mBgRect = new Rect();
    }

    @Override
    public void drawBackground(Canvas c, Paint p, int left, int right, int top, int baseline, int bottom, CharSequence text, int start, int end, int lnum) {
        int textWidth = Math.round(p.measureText(text, start, end));
        int paintColor = p.getColor();
        // Draw the background
        mBgRect.set(left - mPadding,
                top - (lnum == 0 ? mPadding / 2 : -(mPadding / 2)),
                left + textWidth + mPadding,
                bottom + mPadding / 2);
        p.setColor(mBackgroundColor);
        c.drawRect(mBgRect, p);
        p.setColor(paintColor);
    }
}