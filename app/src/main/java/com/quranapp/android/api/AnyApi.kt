package com.quranapp.android.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

interface AnyApi {
    @Streaming
    @GET
    suspend fun downloadStreaming(@Url url: String): Response<ResponseBody>
}
