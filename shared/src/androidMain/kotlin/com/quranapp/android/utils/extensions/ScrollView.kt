package com.quranapp.android.utils.extensions

import android.graphics.Point
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ScrollView
import androidx.core.widget.NestedScrollView

fun HorizontalScrollView.centerInHorizontalScrollView(child: View) {
    val center = scrollX + width / 2
    val left = child.left
    val childWidth = child.width
    if (center >= left && center <= left + childWidth) {
        scrollBy(left + childWidth / 2 - center, 0)
    }
}

fun ScrollView.scrollToViewVertically(view: View) {
    val childOffset = Point()
    getDeepChildOffset(this, view.parent, view, childOffset)
    this.smoothScrollTo(0, childOffset.y)
}

fun NestedScrollView.scrollToViewVertically(view: View) {
    val childOffset = Point()
    getDeepChildOffset(this, view.parent, view, childOffset)
    this.smoothScrollTo(0, childOffset.y)
}
