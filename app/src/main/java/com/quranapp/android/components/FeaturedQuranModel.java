package com.quranapp.android.components;

import androidx.annotation.NonNull;

import kotlin.Pair;

public class FeaturedQuranModel extends ComponentBase {
    public int chapterNo;
    public Pair<Integer, Integer> verseRange;
    public String name;
    public String miniInfo;

    @NonNull
    @Override
    public String toString() {
        return "FeaturedQuranModel{" +
            "name='" + name + '\'' +
            ", miniInfo='" + miniInfo + '\'' +
            '}';
    }
}
