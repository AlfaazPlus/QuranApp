package com.quranapp.android.utils.extensions

import android.view.ViewGroup
import androidx.core.view.MarginLayoutParamsCompat

fun ViewGroup.MarginLayoutParams.updateMargins(margin: Int) {
    setMargins(margin, margin, margin, margin)
}

fun ViewGroup.MarginLayoutParams.updateMargins(marginH: Int, marginV: Int) {
    setMargins(marginH, marginV, marginH, marginV)
}

fun ViewGroup.MarginLayoutParams.updateMarginHorizontal(margin: Int) {
    MarginLayoutParamsCompat.setMarginStart(this, margin)
    MarginLayoutParamsCompat.setMarginEnd(this, margin)
}

fun ViewGroup.MarginLayoutParams.updateMarginVertical(margin: Int) {
    topMargin = margin
    bottomMargin = margin
}
