/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 24/7/2022.
 * All rights reserved.
 */

package com.quranapp.android.views.readerSpinner2.viewholders

import android.view.View
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.recyclerview.widget.RecyclerView
import com.quranapp.android.R
import com.quranapp.android.views.reader.chapterSpinner.ChapterSpinnerItem
import com.quranapp.android.views.reader.juzSpinner.JuzSpinnerItem
import com.quranapp.android.views.reader.spinner.ReaderSpinnerItem
import com.quranapp.android.views.reader.verseSpinner.VerseSpinnerItem
import com.quranapp.android.views.readerSpinner2.adapters.ADPJuzChapterVerseBase
import com.quranapp.android.views.readerSpinner2.adapters.ChapterSelectorAdapter2
import com.quranapp.android.views.readerSpinner2.adapters.JuzSelectorAdapter2
import com.quranapp.android.views.readerSpinner2.adapters.VerseSelectorAdapter2
import com.quranapp.android.widgets.chapterCard.ChapterCard

class VHJuzSpinner(adapter: JuzSelectorAdapter2, view: View) : VHJuzChapterVerseBase<JuzSpinnerItem>(
    adapter,
    view
) {
    override fun bind(item: JuzSpinnerItem) {
        super.bind(item)
        (itemView as TextView).text = item.label
    }
}

class VHChapterSpinner(adapter: ChapterSelectorAdapter2, private val chapterCard: ChapterCard) :
    VHJuzChapterVerseBase<ChapterSpinnerItem>(adapter, chapterCard) {
    override fun bind(item: ChapterSpinnerItem) {
        super.bind(item)
        val chapter = item.chapter
        chapterCard.chapterNumber = chapter.chapterNumber
        chapterCard.setName(chapter.name, chapter.nameTranslation)
    }
}

class VHVerseSpinner(adapter: VerseSelectorAdapter2, view: View) : VHJuzChapterVerseBase<VerseSpinnerItem>(
    adapter,
    view
) {
    override fun bind(item: VerseSpinnerItem) {
        super.bind(item)
        (itemView as TextView).text = item.label
    }
}

open class VHJuzChapterVerseBase<T : ReaderSpinnerItem>(
    private val adapter: ADPJuzChapterVerseBase<T, *>,
    itemView: View
) :
    RecyclerView.ViewHolder(itemView) {
    init {
        itemView.setBackgroundResource(R.drawable.dr_bg_juz_chapter_verse_item)
    }

    @CallSuper
    open fun bind(item: T) {
        itemView.isSelected = item.selected
        itemView.setOnClickListener { adapter.onItemSelectInAdapter(item, true) }
    }
}
