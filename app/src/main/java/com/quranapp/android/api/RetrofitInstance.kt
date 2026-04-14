package com.quranapp.android.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.quranapp.android.BuildConfig
import com.quranapp.android.utils.Logger
import kotlinx.serialization.ExperimentalSerializationApi
import okhttp3.ConnectionSpec
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

@OptIn(ExperimentalSerializationApi::class)
object RetrofitInstance {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectionSpecs(
            listOf(
                ConnectionSpec.MODERN_TLS,
                ConnectionSpec.COMPATIBLE_TLS,
                ConnectionSpec.CLEARTEXT
            )
        )
        .addInterceptor { chain ->
            Logger.print(chain.request().url)

            val newRequest = chain.request().newBuilder()
                .addHeader("X-QuranApp-Version", BuildConfig.VERSION_CODE.toString())
                .build()

            return@addInterceptor chain.proceed(newRequest)
        }
        .cache(null)
        .build()

    private var githubApi: GithubApi? = null
    private var githubLikeApi: GithubLikeApi? = null
    var githubProxyBaseUrl: String = ApiConfig.GH_PROXY_BASE_URL
    var githubLikeProxyBaseUrl: String = ApiConfig.GH_PROXY_ROOT

    val github: GithubApi
        get() {
            if (githubApi == null) {
                githubApi = Retrofit.Builder()
                    .baseUrl(githubProxyBaseUrl)
                    .addConverterFactory(
                        JsonHelper.json.asConverterFactory("application/json".toMediaType())
                    )
                    .client(client)
                    .build()
                    .create(GithubApi::class.java)
            }

            return githubApi!!
        }

    val githubLike: GithubLikeApi
        get() {
            if (githubLikeApi == null) {
                githubLikeApi = Retrofit.Builder()
                    .baseUrl(githubLikeProxyBaseUrl)
                    .addConverterFactory(
                        JsonHelper.json.asConverterFactory("application/json".toMediaType())
                    )
                    .client(client)
                    .build()
                    .create(GithubLikeApi::class.java)
            }

            return githubLikeApi!!
        }

    val any: AnyApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://example.com")
            .addConverterFactory(
                JsonHelper.json.asConverterFactory("application/json".toMediaType())
            )
            .client(client)
            .build()
            .create(AnyApi::class.java)
    }


    val alfaazplus: AlfaazPlusApi by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConfig.ALFAAZPLUS_API_ROOT_URL)
            .addConverterFactory(
                JsonHelper.json.asConverterFactory("application/json".toMediaType())
            )
            .client(client)
            .build()
            .create(AlfaazPlusApi::class.java)
    }

    fun resetGithubApi() {
        githubApi = null
        githubLikeApi = null
    }
}
