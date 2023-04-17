package com.quranapp.android.viewModels

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.quranapp.android.R
import com.quranapp.android.utils.sharedPrefs.SPFavouriteChapters
import com.quranapp.android.utils.univ.MessageUtils

class FavChaptersViewModel : ViewModel() {
    private val _favChapters = MutableLiveData<List<Int>>()
    val favChapters: LiveData<List<Int>> = _favChapters

    fun refreshFavChapters(ctx: Context) {
        _favChapters.value = SPFavouriteChapters.getFavouriteChapters(ctx)
    }

    fun isAddedToFavorites(ctx: Context, chapterNo: Int): Boolean {
        return SPFavouriteChapters.isAddedToFavorites(ctx, chapterNo)
    }

    fun addToFavourites(ctx: Context, chapterNo: Int) {
        SPFavouriteChapters.addToFavorites(ctx, chapterNo)
        refreshFavChapters(ctx)

        MessageUtils.showRemovableToast(ctx, R.string.msgChapterAddedToFavourites, Toast.LENGTH_SHORT)
    }

    fun removeFromFavourites(ctx: Context, chapterNo: Int) {
        SPFavouriteChapters.removeFromFavorites(ctx, chapterNo)
        refreshFavChapters(ctx)

        MessageUtils.showRemovableToast(ctx, R.string.msgChapterRemovedFromFavourites, Toast.LENGTH_SHORT)
    }
}