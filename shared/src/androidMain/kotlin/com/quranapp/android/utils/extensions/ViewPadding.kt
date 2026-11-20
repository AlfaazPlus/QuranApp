package com.quranapp.android.utils.extensions

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.updatePaddingRelative

fun View.updatePaddings(padding: Int) {
    updatePaddings(padding, padding)
}

fun View.updatePaddings(horizontalPadding: Int, verticalPadding: Int) {
    updatePaddingRelative(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
}

fun View.updatePaddingHorizontal(padding: Int) {
    setPadding(padding, paddingTop, padding, paddingBottom)
}

fun View.updatePaddingVertical(padding: Int) {
    setPadding(ViewCompat.getPaddingStart(this), padding, ViewCompat.getPaddingEnd(this), padding)
}

fun View.updatePaddingVertical(top: Int, bottom: Int) {
    setPadding(ViewCompat.getPaddingStart(this), top, ViewCompat.getPaddingEnd(this), bottom)
}
