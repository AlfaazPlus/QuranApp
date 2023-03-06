/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 25/2/2022.
 * All rights reserved.
 */
package com.quranapp.android.utils.univ

object DBUtils {
    @JvmStatic
    fun createDBSelection(vararg cols: String?): String {
        val selection = StringBuilder()
        val l = cols.size
        val lastIndex = l - 1
        for (i in 0 until l) {
            val col = cols[i]
            selection.append(col).append("=?")
            if (i < lastIndex) {
                selection.append(" AND ")
            }
        }
        return selection.toString()
    }
}
