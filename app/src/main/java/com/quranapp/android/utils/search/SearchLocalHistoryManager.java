package com.quranapp.android.utils.search;

import android.text.TextUtils;

import java.util.LinkedList;

public class SearchLocalHistoryManager {
    private final LinkedList<String> mQueries = new LinkedList<>();
    private String mLastQuery = "";

    public void onTravelForward(String query) {
        if (!TextUtils.isEmpty(mLastQuery) && !mLastQuery.equalsIgnoreCase(query)) {
            mQueries.remove(mLastQuery);
            mQueries.add(mLastQuery);
        }

        setLastQuery(query);
    }

    public String onTravelBackward() {
        if (!hasHistories()) {
            return null;
        }

        return mQueries.removeLast();
    }

    public boolean hasHistories() {
        return !mQueries.isEmpty();
    }

    public boolean isCurrentQuery(String query) {
        return query.equalsIgnoreCase(getLastQuery());
    }

    public String getLastQuery() {
        return mLastQuery;
    }

    public void setLastQuery(String query) {
        mLastQuery = query;
    }
}
