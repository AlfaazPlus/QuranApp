package com.quranapp.android.utils.reader.wbw

import android.content.Context
import androidx.core.content.edit

class WbwVersionStore(
    context: Context
) {
    companion object {
        private const val KEY_MANIFEST_VERSION = "wbw.manifest.version"
        private const val KEY_ITEM_VERSION_PREFIX = "wbw.item.version."
    }

    private val appContext = context.applicationContext

    private fun sp() = appContext.getSharedPreferences("sp_wbw_versions", Context.MODE_PRIVATE)

    fun getManifestVersion(): Int = sp().getInt(KEY_MANIFEST_VERSION, 0)

    fun setManifestVersion(version: Int?) {
        sp().edit { putInt(KEY_MANIFEST_VERSION, version ?: 1) }
    }

    fun getItemVersion(id: String): Int = sp().getInt(KEY_ITEM_VERSION_PREFIX + id, 0)

    fun setItemVersion(id: String, version: Int) {
        sp().edit { putInt(KEY_ITEM_VERSION_PREFIX + id, version) }
    }
}
