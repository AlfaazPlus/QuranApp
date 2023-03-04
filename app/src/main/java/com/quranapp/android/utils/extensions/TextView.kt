package com.quranapp.android.utils.extensions

import android.util.TypedValue
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.core.content.ContextCompat
import com.peacedesign.android.utils.ResUtils

fun TextView.setTextSizePx(@DimenRes dimenResId: Int) {
    setTextSize(TypedValue.COMPLEX_UNIT_PX, ResUtils.getDimenPx(context, dimenResId).toFloat())
}

fun TextView.setTextColorResource(@ColorRes colorResId: Int) {
    setTextColor(ContextCompat.getColor(context, colorResId))
}