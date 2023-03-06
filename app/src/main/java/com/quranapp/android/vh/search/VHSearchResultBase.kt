package com.quranapp.android.vh.search

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.updateMarginsRelative
import androidx.recyclerview.widget.RecyclerView
import com.peacedesign.android.utils.Dimen
import com.quranapp.android.R
import com.quranapp.android.components.search.SearchResultModelBase

open class VHSearchResultBase(itemView: View) : RecyclerView.ViewHolder(itemView) {
    open fun bind(model: SearchResultModelBase, pos: Int) {}

    protected fun setupJumperView(view: View, applyMargins: Boolean) {
        view.layoutParams = MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            if (applyMargins) {
                val marg = Dimen.dp2px(view.context, 10f)
                updateMarginsRelative(start = marg, end = marg, bottom = 10)
            }
        }
        view.setBackgroundResource(R.drawable.dr_bg_chapter_card_bordered_onlylight)
    }
}
