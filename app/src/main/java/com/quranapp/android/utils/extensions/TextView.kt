package com.quranapp.android.utils.extensions

import android.util.TypedValue
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.core.content.ContextCompat

fun TextView.setTextSizePx(@DimenRes dimenResId: Int) {
    setTextSize(TypedValue.COMPLEX_UNIT_PX, context.getDimenPx(dimenResId).toFloat())
}

fun TextView.setTextColorResource(@ColorRes colorResId: Int) {
    setTextColor(ContextCompat.getColor(context, colorResId))
}
