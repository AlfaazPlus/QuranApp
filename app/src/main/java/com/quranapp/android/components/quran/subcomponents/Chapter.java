package com.quranapp.android.components.quran.subcomponents;

import com.quranapp.android.components.quran.QuranMeta;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Objects;

import kotlin.Pair;

public class Chapter implements Serializable {
    private int currentVerseNo = 1;
    private int chapterNumber;
    private QuranMeta.ChapterMeta chapterMeta = new QuranMeta.ChapterMeta();
    private ArrayList<Integer> juzs;
    private ArrayList<Verse> verses = new ArrayList<>();

    public Chapter() {}

    public Chapter(Chapter chapter) {
        currentVerseNo = chapter.currentVerseNo;
        chapterNumber = chapter.chapterNumber;
        chapterMeta = chapter.chapterMeta;
        juzs = chapter.juzs;

        verses = new ArrayList<>();
        for (Verse verse : chapter.verses) {
            verses.add(verse.copy());
        }
    }

    public QuranMeta.ChapterMeta getChapterMeta() {
        return chapterMeta;
    }

    public int getChapterNumber() {
        return chapterNumber;
    }

    public void setChapterNumber(int chapterNo, QuranMeta quranMeta) {
        chapterNumber = chapterNo;
        chapterMeta = quranMeta.getChapterMeta(chapterNo);

        setJuzs(quranMeta.getChapterJuzs(chapterNo));
    }

    public ArrayList<Integer> getJuzs() {
        return juzs;
    }

    private void setJuzs(ArrayList<Integer> juzs) {
        this.juzs = juzs;
    }

    public String getName() {
        return chapterMeta.getName();
    }

    public String getNameTranslation() {
        return chapterMeta.getNameTranslation();
    }

    public int getVerseCount() {
        return chapterMeta.verseCount;
    }

    public int getCurrentVerseNo() {
        return currentVerseNo;
    }

    public void setCurrentVerseNo(int verseNo) {
        currentVerseNo = verseNo;
    }

    public ArrayList<Verse> getVerses() {
        return verses;
    }

    public void setVerses(ArrayList<Verse> verses) {
        this.verses = verses;
    }

    public Verse getVerse(int verseNo) {
        try {
            return verses.get(verseNo - 1);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Pair<Integer, Integer> getPageRange() {
        return chapterMeta.pageRange;
    }

    public String getTags() {
        return chapterMeta.tags;
    }

    public boolean canShowBismillah() {
        return QuranMeta.canShowBismillah(chapterNumber);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Chapter)) return false;
        Chapter chapter = (Chapter) o;
        return getChapterNumber() == chapter.getChapterNumber();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getChapterNumber());
    }

    public Chapter copy() {
        return new Chapter(this);
    }
}
