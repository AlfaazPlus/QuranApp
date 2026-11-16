/*
 * (c) Faisal Khan. Created on 20/11/2021.
 */

package com.quranapp.android.callbacks;

import com.quranapp.android.components.readHistory.ReadHistoryModel;

public interface ReadHistoryCallbacks {
    void onReadHistoryRemoved(ReadHistoryModel model);

    void onReadHistoryAdded(ReadHistoryModel model);
}
