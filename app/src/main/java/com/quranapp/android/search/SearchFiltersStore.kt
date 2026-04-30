package com.quranapp.android.search

import androidx.datastore.preferences.core.stringPreferencesKey
import com.alfaazplus.sunnah.ui.utils.shared_preference.DataStoreManager
import com.alfaazplus.sunnah.ui.utils.shared_preference.PrefKey

object SearchFiltersStore {
    private val KEY_SLUGS =
        PrefKey(stringPreferencesKey("search_filter_slugs"), "")

    fun read(): SearchFilters {
        val slugsCsv = DataStoreManager.read(KEY_SLUGS)

        val slugs = slugsCsv
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
            .takeIf { it.isNotEmpty() }

        return SearchFilters(
            selectedSlugs = slugs,
        )
    }

    suspend fun write(filters: SearchFilters) {
        DataStoreManager.edit {
            this[KEY_SLUGS.key] = filters.selectedSlugs
                ?.joinToString(",")
                ?: ""
        }
    }
}
