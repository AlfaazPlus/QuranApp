package com.quranapp.android.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.quranapp.android.ApiConfig
import kotlinx.serialization.ExperimentalSerializationApi
import okhttp3.MediaType
import retrofit2.Retrofit


@OptIn(ExperimentalSerializationApi::class)
object RetrofitInstance {
    val github: GithubApi by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConfig.GITHUB_ROOT_URL)
            .addConverterFactory(
                JsonHelper.json.asConverterFactory(MediaType.get("application/json"))
            )
            .build()
            .create(GithubApi::class.java)
    }
}