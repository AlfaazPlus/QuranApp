/*
 * (c) Faisal Khan. Created on 20/11/2021.
 */
package com.quranapp.android.interfaceUtils

import com.quranapp.android.components.readHistory.ReadHistoryModel

interface ReadHistoryCallbacks {
    fun onReadHistoryRemoved(model: ReadHistoryModel)
    fun onReadHistoryAdded(model: ReadHistoryModel)
}
