package com.quranapp.android.api

import com.quranapp.android.api.models.tafsir.v2.TafsirModelV2
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path

interface AlfaazPlusApi {
    @GET("/quran/tafsirs/{tafsir_key}/by_verse/{verse_key}")
    suspend fun getTafsir(
        @Path("tafsir_key") key: String,
        @Path("verse_key") verseKey: String
    ): TafsirModelV2

    @GET("/quran/tafsirs")
    suspend fun getAvailableTafsirs(): ResponseBody
}
