package com.quranapp.android.components.search;

import android.view.View;
import androidx.annotation.NonNull;

import com.quranapp.android.components.quran.subcomponents.Translation;

import java.util.List;
import java.util.Set;

public class VerseResultModel extends SearchResultModelBase {
    public int chapterNo;
    public int verseNo;
    public String chapterName;
    public String chapterNameSansPrefix;
    public String verseSerial;
    public Set<String> translSlugs;
    public List<String> translDisplayNames;
    public List<Integer> startIndices;
    public List<Translation> translations;
    public List<Integer> endIndices;
    public View translationsView;

    @NonNull
    @Override
    public String toString() {
        return verseSerial + "";
    }
}
