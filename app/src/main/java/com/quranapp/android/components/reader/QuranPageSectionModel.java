package com.quranapp.android.components.reader;

import com.quranapp.android.components.quran.subcomponents.Verse;
import com.quranapp.android.views.reader.QuranPageView;

import java.util.ArrayList;

public class QuranPageSectionModel {
    private ArrayList<Verse> verses;
    private int[] fromToVerses;
    private boolean showTitle;
    private boolean showBismillah;
    private int chapterNo;
    private CharSequence contentSpannable;
    private QuranPageView.QuranPageSectionView sectionView;
    private int scrollHighlightPendingVerseNo;

    public int parentIndexInAdapter;

    public void setShowBismillah(boolean showBismillah) {
        this.showBismillah = showBismillah;
    }

    public void setShowTitle(boolean showTitle) {
        this.showTitle = showTitle;
    }

    public boolean showTitle() {
        return showTitle;
    }

    public boolean showBismillah() {
        return showBismillah;
    }

    public ArrayList<Verse> getVerses() {
        return verses;
    }

    public void setVerses(ArrayList<Verse> verses) {
        this.verses = verses;
        setFromToVerses(new int[]{verses.get(0).verseNo, verses.get(verses.size() - 1).verseNo});
    }

    private void setFromToVerses(int[] fromToVerses) {
        this.fromToVerses = fromToVerses;
    }

    public int[] getFromToVerses() {
        return fromToVerses;
    }

    public int getChapterNo() {
        return chapterNo;
    }

    public void setChapterNo(int chapterNo) {
        this.chapterNo = chapterNo;
    }

    public CharSequence getContentSpannable() {
        return contentSpannable;
    }

    public void setContentSpannable(CharSequence contentSpannable) {
        this.contentSpannable = contentSpannable;
    }

    public QuranPageView.QuranPageSectionView getSectionView() {
        return sectionView;
    }

    public void setSectionView(QuranPageView.QuranPageSectionView sectionView) {
        this.sectionView = sectionView;
    }

    public boolean hasVerse(int verseNo) {
        return fromToVerses[0] <= verseNo && verseNo <= fromToVerses[1];
    }

    public void setScrollHighlightPendingVerseNo(int scrollHighlightPendingVerseNo) {
        this.scrollHighlightPendingVerseNo = scrollHighlightPendingVerseNo;
    }

    public int getScrollHighlightPendingVerseNo() {
        return scrollHighlightPendingVerseNo;
    }
}
