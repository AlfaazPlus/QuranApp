package com.quranapp.android.backup

import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.quranapp.android.compose.utils.preferences.DataStoreManager
import com.quranapp.android.compose.utils.preferences.PrefKey

object BackupPreferences {
    private val enabledKey = PrefKey(booleanPreferencesKey("auto_backup_enabled"), false)
    private val documentUriKey = PrefKey(stringPreferencesKey("auto_backup_document_uri"), "")

    fun documentUri(): String = DataStoreManager.read(documentUriKey)

    suspend fun isEnabled(): Boolean = DataStoreManager.readFirst(enabledKey)

    suspend fun readDocumentUri(): String = DataStoreManager.readFirst(documentUriKey)

    @Composable
    fun observeEnabled(): Boolean = DataStoreManager.observe(enabledKey)

    @Composable
    fun observeDocumentUri(): String = DataStoreManager.observe(documentUriKey)

    suspend fun setEnabled(enabled: Boolean) {
        DataStoreManager.write(enabledKey, enabled)
    }

    suspend fun setDocumentUri(uri: String) {
        DataStoreManager.edit {
            this[enabledKey.key] = true
            this[documentUriKey.key] = uri
        }
    }
}
