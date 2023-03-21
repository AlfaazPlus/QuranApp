package com.quranapp.android.components.reader;

import com.quranapp.android.components.quran.subcomponents.Verse;
import com.quranapp.android.reader_managers.ReaderParams.RecyclerItemViewTypeConst;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import kotlin.Pair;

public class QuranPageModel {
    private int pageNo;
    private String chaptersName;
    private ArrayList<QuranPageSectionModel> sections;
    private Pair<Integer, Integer> chaptersOnPage;
    private final Map<Integer, int[]> fromToVerses = new TreeMap<>();
    private int juzNo;
    private int scrollHighlightPendingChapterNo = -1;
    private int scrollHighlightPendingVerseNo = -1;
    @RecyclerItemViewTypeConst
    private int viewType;

    public QuranPageModel() {
    }

    public QuranPageModel(int pageNo, int juzNo, Pair<Integer, Integer> chaptersOnPage, String chaptersName, ArrayList<QuranPageSectionModel> sections) {
        this.pageNo = pageNo;
        this.juzNo = juzNo;
        this.chaptersOnPage = chaptersOnPage;
        this.chaptersName = chaptersName;
        this.sections = sections;

        for (QuranPageSectionModel section : sections) {
            ArrayList<Verse> verses = section.getVerses();
            int[] verseNos = {verses.get(0).verseNo, verses.get(verses.size() - 1).verseNo};
            fromToVerses.put(section.getChapterNo(), verseNos);
        }
    }

    public int getPageNo() {
        return pageNo;
    }

    public List<QuranPageSectionModel> getSections() {
        return sections;
    }

    public int getJuzNo() {
        return juzNo;
    }

    public String getChaptersName() {
        return chaptersName;
    }

    public QuranPageModel setViewType(@RecyclerItemViewTypeConst int viewType) {
        this.viewType = viewType;
        return this;
    }

    @RecyclerItemViewTypeConst
    public int getViewType() {
        return viewType;
    }

    public boolean hasChapter(int chapterNo) {
        return chaptersOnPage.getFirst() <= chapterNo && chapterNo <= chaptersOnPage.getSecond();
    }

    public boolean hasVerse(int chapterNo, int verseNo) {
        boolean hasVerse = false;
        if (hasChapter(chapterNo)) {
            for (Integer chapterN : fromToVerses.keySet()) {
                int[] verses = fromToVerses.get(chapterN);
                if (verses != null) {
                    hasVerse = verses[0] <= verseNo && verseNo <= verses[1];
                    if (hasVerse) {
                        break;
                    }
                }
            }
        }
        return hasVerse;
    }

    public void setScrollHighlightPendingChapterNo(int scrollHighlightPendingChapterNo) {
        this.scrollHighlightPendingChapterNo = scrollHighlightPendingChapterNo;
    }

    public int getScrollHighlightPendingChapterNo() {
        return scrollHighlightPendingChapterNo;
    }

    public void setScrollHighlightPendingVerseNo(int scrollHighlightPendingVerseNo) {
        this.scrollHighlightPendingVerseNo = scrollHighlightPendingVerseNo;
    }

    public int getScrollHighlightPendingVerseNo() {
        return scrollHighlightPendingVerseNo;
    }
}
