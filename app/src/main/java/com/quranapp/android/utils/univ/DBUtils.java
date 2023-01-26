/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 25/2/2022.
 * All rights reserved.
 */

package com.quranapp.android.utils.univ;

public final class DBUtils {
    public static String createDBSelection(String... cols) {
        StringBuilder selection = new StringBuilder();
        int l = cols.length;
        int lastIndex = l - 1;
        for (int i = 0; i < l; i++) {
            String col = cols[i];
            selection.append(col).append("=?");

            if (i < lastIndex) {
                selection.append(" AND ");
            }
        }
        return selection.toString();
    }
}
