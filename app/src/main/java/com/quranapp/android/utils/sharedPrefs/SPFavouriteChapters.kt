package com.quranapp.android.utils.sharedPrefs

import android.content.Context
import androidx.core.content.edit

object SPFavouriteChapters {
    private const val SP_FAVOURITE_CHAPTERS = "sp_favourite_chapters"
    private const val KEY_FAVOURITE_CHAPTERS = "key.favourite_chapters"

    private fun sp(ctx: Context) =
        ctx.getSharedPreferences(SP_FAVOURITE_CHAPTERS, Context.MODE_PRIVATE)

    fun getFavouriteChapters(ctx: Context): List<Int> {
        val favChapters = sp(ctx).getStringSet(KEY_FAVOURITE_CHAPTERS, setOf())
        return favChapters?.map { it.toInt() }?.toList() ?: listOf()
    }
}