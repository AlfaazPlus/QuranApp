package com.quranapp.android.utils.wbw

import androidx.datastore.preferences.core.intPreferencesKey
import com.quranapp.android.preferences.DataStoreManager

object WbwVersionStore {
    internal const val KEY_PREFIX = "wbw.item.version."

    fun getItemVersion(id: String): Int {
        return DataStoreManager.read(intPreferencesKey(KEY_PREFIX + id), 0)
    }

    suspend fun setItemVersion(id: String, version: Int) {
        DataStoreManager.write(intPreferencesKey(KEY_PREFIX + id), version)
    }
}
