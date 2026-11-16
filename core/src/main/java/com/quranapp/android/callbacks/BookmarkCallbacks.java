/*
 * (c) Faisal Khan. Created on 20/11/2021.
 */

package com.quranapp.android.callbacks;

import com.quranapp.android.components.bookmark.BookmarkModel;

public interface BookmarkCallbacks {
    void onBookmarkRemoved(BookmarkModel model);

    default void onBookmarkAdded(BookmarkModel model) {
    }

    default void onBookmarkUpdated(BookmarkModel model) {
    }
}
