package com.quranapp.android.components.quran.subcomponents;

import android.content.Context;
import androidx.annotation.NonNull;

import com.quranapp.android.utils.verse.VerseUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Verse implements Serializable {
    public final int id;
    public final int pageNo;
    public final int chapterNo;
    public final int verseNo;
    public final String arabicText;
    private List<Translation> translations = new ArrayList<>();
    private boolean includeChapterNameInSerial;
    private transient CharSequence mTranslTextSpannable;

    public Verse(int id, int chapterNo, int verseNo, int pageNo, String arabicText) {
        this.id = id;
        this.chapterNo = chapterNo;
        this.verseNo = verseNo;
        this.pageNo = pageNo;
        this.arabicText = arabicText;
    }

    public Verse(Verse verse) {
        id = verse.id;
        chapterNo = verse.chapterNo;
        verseNo = verse.verseNo;
        pageNo = verse.pageNo;
        arabicText = verse.arabicText;
        // not copying
        translations = verse.translations;
        includeChapterNameInSerial = verse.includeChapterNameInSerial;
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
        return VerseUtils.isVOTD(ctx, chapterNo, verseNo);
    }

    public boolean isIdealForVOTD() {
        int l = arabicText.length();
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
