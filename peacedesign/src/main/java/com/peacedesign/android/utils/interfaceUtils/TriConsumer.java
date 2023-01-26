package com.peacedesign.android.utils.interfaceUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * This interface is intended for loop that may work with three parameters which are index, key and value respectively.
 *
 * @param <Index> Index of the loop
 * @param <Key>   Key
 * @param <Value> Value
 */
@FunctionalInterface
public interface TriConsumer<Index, Key, Value> {
    void accept(@NonNull Index index, @NonNull Key key, @Nullable Value value);
}
