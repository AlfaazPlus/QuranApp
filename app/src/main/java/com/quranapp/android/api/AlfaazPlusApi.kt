package com.quranapp.android.api

import com.quranapp.android.api.models.tafsir.TafsirResponseModel
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AlfaazPlusApi {
    @GET("/quran/tafsirs")
    suspend fun getAvailableTafsirs(): ResponseBody

    @GET("/quran/tafsirs/by_verse")
    suspend fun getTafsirsByVerse(
        @Query("keys") tafsirKeys: String,
        @Query("verse_key") verseKey: String,
    ): TafsirResponseModel

    @GET("/quran/tafsirs/by_surah")
    suspend fun getTafsirsByChapter(
        @Query("key") tafsirKey: String,
        @Query("surahs") surahRange: String,
    ): TafsirResponseModel
}
