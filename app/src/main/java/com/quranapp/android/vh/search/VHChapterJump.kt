package com.quranapp.android.vh.search

import com.quranapp.android.components.search.ChapterJumpModel
import com.quranapp.android.components.search.SearchResultModelBase
import com.quranapp.android.utils.reader.factory.ReaderFactory.startChapter
import com.quranapp.android.widgets.chapterCard.ChapterCard

class VHChapterJump(private val chapterCard: ChapterCard, applyMargins: Boolean) : VHSearchResultBase(chapterCard) {
    init {
        setupJumperView(chapterCard, applyMargins)
    }

    override fun bind(model: SearchResultModelBase, pos: Int) {
        if (model is ChapterJumpModel) {
            chapterCard.apply {
                chapterNumber = model.chapterNo
                setName(model.name, model.nameTranslation)
                setOnClickListener { startChapter(it.context, model.chapterNo) }
            }
        }
    }
}