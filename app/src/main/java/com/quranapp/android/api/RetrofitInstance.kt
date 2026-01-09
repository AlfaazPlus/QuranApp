package com.quranapp.android.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.quranapp.android.BuildConfig
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

            val newRequest = chain.request().newBuilder()
                .addHeader("X-QuranApp-Version", BuildConfig.VERSION_CODE.toString())
                .build()

            return@addInterceptor chain.proceed(newRequest)
        }
        .cache(null)
        .build()

    private var githubApi: GithubApi? = null
    var githubResDownloadUrl: String = ApiConfig.GH_PROXY_ROOT_URL

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


    val alfaazplus: AlfaazPlusApi by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConfig.ALFAAZPLUS_API_ROOT_URL)
            .addConverterFactory(
                JsonHelper.json.asConverterFactory(MediaType.get("application/json"))
            )
            .client(client)
            .build()
            .create(AlfaazPlusApi::class.java)
    }

    fun resetGithubApi() {
        githubApi = null
    }
}
