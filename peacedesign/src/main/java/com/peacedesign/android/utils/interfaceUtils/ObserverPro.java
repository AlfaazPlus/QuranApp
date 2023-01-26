package com.peacedesign.android.utils.interfaceUtils;

import androidx.annotation.NonNull;

public interface ObserverPro<O> {
    void onUpdate(@NonNull O observable, @NonNull UpdateType updateType);

    enum UpdateType {
        /**
         * Denotes that new data has been added.
         */
        ADD,
        /**
         * Denotes that data has been removed.
         */
        REMOVE,
        /**
         * Denotes that value of data has been changed.
         */
        CHANGE,
        /**
         * Denotes that order of data has been changed.
         */
        SORT,
        /**
         * Denotes that no change has occurred.
         */
        UNCHANGED,
        ADD_OR_CHANGE
    }
}
