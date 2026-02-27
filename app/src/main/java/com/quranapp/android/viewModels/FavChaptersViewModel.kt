package com.quranapp.android.viewModels

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.alfaazplus.sunnah.ui.utils.shared_preference.DataStoreManager
import com.quranapp.android.R
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.sharedPrefs.SPFavouriteChapters
import com.quranapp.android.utils.univ.Keys
import com.quranapp.android.utils.univ.MessageUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class FavChaptersViewModel : ViewModel() {
    private val _favChapters = MutableLiveData<List<Int>>()
    val favChapters: LiveData<List<Int>> = _favChapters

    @Composable
    fun getFavouriteChapters(): List<Int> {
        val raw = DataStoreManager.observe(
            KEY,
            Json.encodeToString(emptyList<Int>())
        )

        return try {
            Json.decodeFromString<List<Int>>(raw)
        } catch (e: Exception) {
            Log.saveError(
                e,
                "getFavouriteChapters",
            )
            emptyList()
        }
    }

    suspend fun addToFavourites(ctx: Context, chapterNo: Int, curr: List<Int>) {
        DataStoreManager.write(
            KEY,
            Json.encodeToString(curr.toMutableList().apply { add(0, chapterNo) })
        )

        MessageUtils.showRemovableToast(
            ctx,
            R.string.msgChapterAddedToFavourites,
            Toast.LENGTH_SHORT
        )
    }

    suspend fun removeFromFavourites(ctx: Context, chapterNo: Int, curr: List<Int>) {
        DataStoreManager.write(
            KEY,
            Json.encodeToString(curr - chapterNo)
        )


        MessageUtils.showRemovableToast(
            ctx,
            R.string.msgChapterRemovedFromFavourites,
            Toast.LENGTH_SHORT
        )
    }

    companion object {
        private val KEY = stringPreferencesKey(Keys.FAVOURITE_CHAPTERS)

        fun migrate(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                val old = SPFavouriteChapters.getFavouriteChapters(context)

                try {
                    DataStoreManager.write(
                        KEY,
                        Json.encodeToString(old)
                    )
                } catch (e: Exception) {
                    Log.saveError(
                        e,
                        "migrateFavouriteChapters",
                    )
                }
            }
        }

    }
}