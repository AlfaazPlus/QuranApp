package com.quranapp.android.components.quran.subcomponents;

import android.content.Context;

import androidx.annotation.NonNull;

import com.quranapp.android.utils.verse.VerseUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Verse implements Cloneable, Serializable {
    private int id;
    private int chapterNo;
    private int verseNo;
    private String arabicText;
    private List<Translation> translations = new ArrayList<>();
    private boolean includeChapterNameInSerial;
    private transient CharSequence mTranslTextSpannable;

    public Verse(int chapterNo, int verseNo) {
        setChapterNo(chapterNo);
        setVerseNo(verseNo);
    }

    public Verse(Verse verse) {
        id = verse.id;
        chapterNo = verse.chapterNo;
        verseNo = verse.verseNo;
        arabicText = verse.arabicText;
        // not copying
        translations = verse.translations;
        includeChapterNameInSerial = verse.includeChapterNameInSerial;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getVerseNo() {
        return verseNo;
    }

    private void setVerseNo(int verseNo) {
        this.verseNo = verseNo;
    }

    public int getChapterNo() {
        return chapterNo;
    }

    public void setChapterNo(int chapterNo) {
        this.chapterNo = chapterNo;
    }

    public String getArabicText() {
        return arabicText;
    }

    public void setArabicText(String arabicText) {
        this.arabicText = arabicText;
    }

    public List<Translation> getTranslations() {
        return translations;
    }

    public void setTranslations(List<Translation> translations) {
        this.translations = translations;
    }

    public int getTranslationCount() {
        return translations.size();
    }

    public CharSequence getTranslTextSpannable() {
        return mTranslTextSpannable;
    }

    public void setTranslTextSpannable(CharSequence translationTextSpannable) {
        mTranslTextSpannable = translationTextSpannable;
    }

    public boolean getIncludeChapterNameInSerial() {
        return includeChapterNameInSerial;
    }

    public void setIncludeChapterNameInSerial(boolean include) {
        includeChapterNameInSerial = include;
    }

    public boolean isVOTD(Context ctx) {
        return VerseUtils.isVOTD(ctx, getChapterNo(), getVerseNo());
    }

    public boolean isIdealForVOTD() {
        int l = getArabicText().length();
        return l > 5 && l <= 300;
    }

    public Verse copy() {
        return new Verse(this);
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(Locale.getDefault(), "VERSE: ChapterNo - %d, VerseNo - %d\n", chapterNo, verseNo);
    }
}
