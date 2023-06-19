package com.quranapp.android.utils.extended

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class GapedItemDecoration(private val gap: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)
        outRect.set(gap, gap, gap, gap)
    }
}