package com.quranapp.android.components.reader;

import androidx.annotation.NonNull;
import static com.quranapp.android.reader_managers.ReaderParams.RecyclerItemViewType.BISMILLAH;
import static com.quranapp.android.reader_managers.ReaderParams.RecyclerItemViewType.CHAPTER_INFO;
import static com.quranapp.android.reader_managers.ReaderParams.RecyclerItemViewType.CHAPTER_TITLE;
import static com.quranapp.android.reader_managers.ReaderParams.RecyclerItemViewType.IS_VOTD;
import static com.quranapp.android.reader_managers.ReaderParams.RecyclerItemViewType.NO_TRANSL_SELECTED;
import static com.quranapp.android.reader_managers.ReaderParams.RecyclerItemViewType.READER_FOOTER;
import static com.quranapp.android.reader_managers.ReaderParams.RecyclerItemViewType.READER_PAGE;
import static com.quranapp.android.reader_managers.ReaderParams.RecyclerItemViewType.VERSE;

import com.quranapp.android.components.quran.subcomponents.Verse;
import com.quranapp.android.reader_managers.ReaderParams;

public class ReaderRecyclerItemModel {
    @ReaderParams.RecyclerItemViewTypeConst
    private int viewType;
    private Verse verse;
    private int chapterNo;
    private boolean scrollHighlightPending;

    public ReaderRecyclerItemModel setViewType(@ReaderParams.RecyclerItemViewTypeConst int viewType) {
        this.viewType = viewType;
        return this;
    }

    @ReaderParams.RecyclerItemViewTypeConst
    public int getViewType() {
        return viewType;
    }

    public ReaderRecyclerItemModel setVerse(Verse verse) {
        this.verse = verse;
        return this;
    }

    public Verse getVerse() {
        return verse;
    }

    public int getChapterNo() {
        return chapterNo;
    }

    public ReaderRecyclerItemModel setChapterNo(int chapterNo) {
        this.chapterNo = chapterNo;
        return this;
    }

    @NonNull
    @Override
    public String toString() {
        String viewTypeStr;
        switch (viewType) {
            case BISMILLAH: viewTypeStr = "BISMILLAH";
                break;
            case VERSE: viewTypeStr = "VERSE";
                break;
            case CHAPTER_TITLE: viewTypeStr = "CHAPTER_TITLE";
                break;
            case READER_FOOTER: viewTypeStr = "READER_FOOTER";
                break;
            case CHAPTER_INFO:
                viewTypeStr = "CHAPTER_INFO";
                break;
            case IS_VOTD:
                viewTypeStr = "IS_VOTD";
                break;
            case NO_TRANSL_SELECTED:
                viewTypeStr = "NO_TRANSL_SELECTED";
                break;
            case READER_PAGE:
                viewTypeStr = "READER_PAGE";
                break;
            default: viewTypeStr = "NONE";
        }
        return "ReaderRecyclerItemModel: VIEW_TYPE: " + viewTypeStr;
    }

    public void setScrollHighlightPending(boolean pending) {
        scrollHighlightPending = pending;
    }

    public boolean isScrollHighlightPending() {
        return scrollHighlightPending;
    }
}
