package com.quranapp.android.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey

object AppActionsPreferences {
    val KEY_TRANSLATIONS_FETCH_FORCE =
        PrefKey(booleanPreferencesKey("app.action.translations.fetch_force"), false)
    val KEY_RECITATIONS_FETCH_FORCE =
        PrefKey(booleanPreferencesKey("app.action.recitations.fetch_force"), false)
    val KEY_RECITATION_TRANSLATIONS_FETCH_FORCE =
        PrefKey(booleanPreferencesKey("app.action.recitation_translations.fetch_force"), false)
    val KEY_TAFSIRS_FETCH_FORCE =
        PrefKey(booleanPreferencesKey("app.action.tafsirs.fetch_force"), false)
    val KEY_URLS_FETCH_FORCE =
        PrefKey(booleanPreferencesKey("app.action.urls.fetch_force"), false)
    val KEY_ONBOARDING_REQUIRED =
        PrefKey(booleanPreferencesKey("app.action.onboarding_required"), true)
    val KEY_LEGACY_MIGRATED =
        PrefKey(booleanPreferencesKey("app_actions.legacy_migrated_v1"), false)

    fun getFetchTranslationsForce(): Boolean {
        return DataStoreManager.read(KEY_TRANSLATIONS_FETCH_FORCE)
    }

    suspend fun setFetchTranslationsForce(fetchForce: Boolean) {
        DataStoreManager.write(KEY_TRANSLATIONS_FETCH_FORCE, fetchForce)
    }

    fun getFetchRecitationsForce(): Boolean {
        return DataStoreManager.read(KEY_RECITATIONS_FETCH_FORCE)
    }

    suspend fun setFetchRecitationsForce(fetchForce: Boolean) {
        DataStoreManager.write(KEY_RECITATIONS_FETCH_FORCE, fetchForce)
    }

    fun getFetchRecitationTranslationsForce(): Boolean {
        return DataStoreManager.read(KEY_RECITATION_TRANSLATIONS_FETCH_FORCE)
    }

    suspend fun setFetchRecitationTranslationsForce(fetchForce: Boolean) {
        DataStoreManager.write(KEY_RECITATION_TRANSLATIONS_FETCH_FORCE, fetchForce)
    }

    fun getFetchTafsirsForce(): Boolean {
        return DataStoreManager.read(KEY_TAFSIRS_FETCH_FORCE)
    }

    suspend fun setFetchTafsirsForce(fetchForce: Boolean) {
        DataStoreManager.write(KEY_TAFSIRS_FETCH_FORCE, fetchForce)
    }

    fun getFetchUrlsForce(): Boolean {
        return DataStoreManager.read(KEY_URLS_FETCH_FORCE)
    }

    suspend fun setFetchUrlsForce(fetchForce: Boolean) {
        DataStoreManager.write(KEY_URLS_FETCH_FORCE, fetchForce)
    }

    fun getRequireOnboarding(): Boolean {
        return DataStoreManager.read(KEY_ONBOARDING_REQUIRED)
    }

    suspend fun setRequireOnboarding(require: Boolean) {
        DataStoreManager.write(KEY_ONBOARDING_REQUIRED, require)
    }
}
