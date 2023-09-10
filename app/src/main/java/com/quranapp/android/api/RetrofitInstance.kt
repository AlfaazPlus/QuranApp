package com.quranapp.android.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.quranapp.android.utils.Logger
import kotlinx.serialization.ExperimentalSerializationApi
import okhttp3.MediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

@OptIn(ExperimentalSerializationApi::class)
object RetrofitInstance {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            Logger.print(chain.request().url())
            return@addInterceptor chain.proceed(chain.request())
        }
        .cache(null)
        .build()

    private var githubApi: GithubApi? = null
    var githubResDownloadUrl: String = ApiConfig.JS_DELIVR_ROOT_URL

    val github: GithubApi
        get() {
            if (githubApi == null) {
                githubApi = Retrofit.Builder()
                    .baseUrl(githubResDownloadUrl)
                    .addConverterFactory(
                        JsonHelper.json.asConverterFactory(MediaType.get("application/json"))
                    )
                    .client(client)
                    .build()
                    .create(GithubApi::class.java)
            }

            return githubApi!!
        }

    val quran: QuranApi by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConfig.QURAN_API_ROOT_URL)
            .addConverterFactory(
                JsonHelper.json.asConverterFactory(MediaType.get("application/json"))
            )
            .client(client)
            .build()
            .create(QuranApi::class.java)
    }

    fun resetGithubApi() {
        githubApi = null
    }
}
