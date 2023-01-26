package com.quranapp.android.components.search;

public class SearchHistoryModel extends SearchResultModelBase {
    private final int id;
    private final CharSequence text;
    private final String date;

    public SearchHistoryModel(int id, CharSequence text, String date) {
        this.id = id;
        this.text = text;
        this.date = date;
    }

    public int getId() {
        return id;
    }

    public CharSequence getText() {
        return text;
    }

    public String getDate() {
        return date;
    }
}
