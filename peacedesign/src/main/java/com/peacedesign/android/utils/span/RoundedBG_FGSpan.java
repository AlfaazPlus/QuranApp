/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 23/2/2022.
 * All rights reserved.
 */

package com.peacedesign.android.utils.span;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.style.ReplacementSpan;

public class RoundedBG_FGSpan extends ReplacementSpan {
    private int PADDING_H;

    private final int mBGColor;
    private final int mBGColorPressed;
    private final int mTextColor;
    private int mRadius;
    private boolean mPressed;

    public RoundedBG_FGSpan(int bgColor, int textColor) {
        PADDING_H = 4;
        mBGColor = bgColor;
        mBGColorPressed = bgColor;
        mTextColor = textColor;
        mRadius = 10;
    }

    public RoundedBG_FGSpan(int[] bgColors, int textColor) {
        this(bgColors, textColor, 10);
    }

    public RoundedBG_FGSpan(int[] bgColors, int textColor, int radius) {
        PADDING_H = 4;
        mBGColor = bgColors[0];
        mBGColorPressed = bgColors[1];
        mTextColor = textColor;
        mRadius = radius;
    }

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        return (int) (PADDING_H + paint.measureText(text.subSequence(start, end).toString()) + PADDING_H);
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
        int mInitialTxtColor = paint.getColor();

        float width = paint.measureText(text.subSequence(start, end).toString());
        RectF rect = new RectF(x, top, x + width + (2 * PADDING_H), bottom);
        paint.setColor(mPressed ? mBGColorPressed : mBGColor);
        if (mRadius != 0) {
            canvas.drawRoundRect(rect, mRadius, mRadius, paint);
        } else {
            canvas.drawRect(rect, paint);
        }

        paint.setColor(mTextColor != -1 ? mTextColor : mInitialTxtColor);

        canvas.drawText(text, start, end, x + PADDING_H, y, paint);
    }

    public RoundedBG_FGSpan setPaddingH(int padding) {
        PADDING_H = padding;
        return this;
    }

    public RoundedBG_FGSpan setPressed(boolean pressed) {
        mPressed = pressed;
        return this;
    }

    public RoundedBG_FGSpan setRadius(int radius) {
        mRadius = radius;
        return this;
    }
}