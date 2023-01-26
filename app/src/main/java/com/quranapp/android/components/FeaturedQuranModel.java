package com.quranapp.android.components;

import androidx.annotation.NonNull;

import com.peacedesign.android.utils.ComponentBase;

public class FeaturedQuranModel extends ComponentBase {
    private int chapterNo;
    private int[] verseRange;
    private String name;
    private String miniInfo;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMiniInfo() {
        return miniInfo;
    }

    public void setMiniInfo(String miniInfo) {
        this.miniInfo = miniInfo;
    }

    public int getChapterNo() {
        return chapterNo;
    }

    public void setChapterNo(int chapterNo) {
        this.chapterNo = chapterNo;
    }

    public int[] getVerseRange() {
        return verseRange;
    }

    public void setVerseRange(int[] verseRange) {
        this.verseRange = verseRange;
    }

    @NonNull
    @Override
    public String toString() {
        return "FeaturedQuranModel{" +
                "name='" + name + '\'' +
                ", miniInfo='" + miniInfo + '\'' +
                '}';
    }
}
