/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 24/7/2022.
 * All rights reserved.
 */

package com.quranapp.android.views.readerSpinner2.adapters

import android.content.Context
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.quranapp.android.R
import com.quranapp.android.utils.extensions.dp2px
import com.quranapp.android.utils.extensions.updatePaddings
import com.quranapp.android.views.reader.chapterSpinner.ChapterSpinnerItem
import com.quranapp.android.views.reader.juzSpinner.JuzSpinnerItem
import com.quranapp.android.views.reader.verseSpinner.VerseSpinnerItem
import com.quranapp.android.views.readerSpinner2.viewholders.VHChapterSpinner
import com.quranapp.android.views.readerSpinner2.viewholders.VHJuzSpinner
import com.quranapp.android.views.readerSpinner2.viewholders.VHVerseSpinner
import com.quranapp.android.widgets.chapterCard.ChapterCardWithoutIcon

class JuzSelectorAdapter2(items: MutableList<JuzSpinnerItem>) : ADPJuzChapterVerseBase<JuzSpinnerItem, VHJuzSpinner>(
    items
) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHJuzSpinner {
        return VHJuzSpinner(this, makeItemView(parent.context))
    }

    private fun makeItemView(context: Context): TextView {
        val txtView = TextView(context)
        txtView.updatePaddings(context.dp2px(15f), context.dp2px(10f))
        txtView.setTextColor(
            ContextCompat.getColorStateList(context, R.color.color_reader_spinner_item_label)
        )
        txtView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return txtView
    }
}

class ChapterSelectorAdapter2(items: MutableList<ChapterSpinnerItem>) :
    ADPJuzChapterVerseBase<ChapterSpinnerItem, VHChapterSpinner>(items) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHChapterSpinner {
        return VHChapterSpinner(this, ChapterCardWithoutIcon(parent.context))
    }
}

class VerseSelectorAdapter2(items: MutableList<VerseSpinnerItem>) :
    ADPJuzChapterVerseBase<VerseSpinnerItem, VHVerseSpinner>(items) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHVerseSpinner {
        return VHVerseSpinner(this, makeItemView(parent.context))
    }

    private fun makeItemView(context: Context): TextView {
        val txtView = TextView(context)
        txtView.updatePaddings(context.dp2px(8f), context.dp2px(10f))
        txtView.setTextColor(
            ContextCompat.getColorStateList(context, R.color.color_reader_spinner_item_label)
        )
        txtView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return txtView
    }
}
