package com.quranapp.android.vh.search;

import com.quranapp.android.components.search.ChapterJumpModel;
import com.quranapp.android.components.search.SearchResultModelBase;
import com.quranapp.android.utils.reader.factory.ReaderFactory;
import com.quranapp.android.widgets.chapterCard.ChapterCard;

public class VHChapterJump extends VHSearchResultBase {
    private final ChapterCard mChapterCard;

    public VHChapterJump(ChapterCard chapterCard, boolean applyMargins) {
        super(chapterCard);
        mChapterCard = chapterCard;

        setupJumperView(chapterCard, applyMargins);
    }


    @Override
    public void bind(SearchResultModelBase parentModel, int pos) {
        ChapterJumpModel model = (ChapterJumpModel) parentModel;

        mChapterCard.setChapterNumber(model.getChapterNo());
        mChapterCard.setName(model.getName(), model.getNameTranslation());
        mChapterCard.setOnClickListener(v -> ReaderFactory.startChapter(v.getContext(), model.getChapterNo()));
    }
}
