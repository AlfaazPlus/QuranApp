package com.quranapp.android.viewModels

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.quranapp.android.utils.sharedPrefs.SPFavouriteChapters

class FavChaptersViewModel : ViewModel() {
    private val _favChapters = MutableLiveData<List<Int>>()
    val favChapters: LiveData<List<Int>> = _favChapters

    private fun getFavChapters(ctx: Context) {
        _favChapters.value = SPFavouriteChapters.getFavouriteChapters(ctx)
    }

    fun addToFavourites(ctx: Context, chapterNo: Int) {
        SPFavouriteChapters.addToFavorites(ctx, chapterNo)
        getFavChapters(ctx)
    }

    fun removeFromFavourites(ctx: Context, chapterNo: Int) {
        SPFavouriteChapters.removeFromFavorites(ctx, chapterNo)
        getFavChapters(ctx)
    }
}