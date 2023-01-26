/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 23/2/2022.
 * All rights reserved.
 */

package com.peacedesign.android.utils.span;

import android.text.TextPaint;
import android.text.style.SuperscriptSpan;

import androidx.annotation.NonNull;

public class SuperscriptSpan2 extends SuperscriptSpan {
    @Override
    public void updateDrawState(@NonNull TextPaint textPaint) {
        textPaint.baselineShift += (int) ((textPaint.ascent()) / 3);
    }

    @Override
    public void updateMeasureState(@NonNull TextPaint textPaint) {
        textPaint.baselineShift += (int) ((textPaint.ascent()) / 3);
    }
}
