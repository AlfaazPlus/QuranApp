package com.peacedesign.android.utils;

import androidx.annotation.NonNull;

public abstract class ArrayUtils {
    public static boolean contains(@NonNull int[] array, int o) {
        boolean bool = false;
        for (int a : array) {
            if (a == o) {
                bool = true;
                break;
            }
        }
        return bool;
    }

    public static boolean contains(@NonNull Object[] array, @NonNull Object o) {
        boolean bool = false;
        for (Object a : array) {
            if (a == o) {
                bool = true;
                break;
            }
        }
        return bool;
    }
}