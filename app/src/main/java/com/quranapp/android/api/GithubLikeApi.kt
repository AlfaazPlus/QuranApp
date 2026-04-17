package com.quranapp.android.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming

/** Gh-proxy root with relative paths for arbitrary GitHub raw content. */
interface GithubLikeApi {
    @GET("{path}")
    @Streaming
    suspend fun getRawContent(
        @Path(value = "path", encoded = true) path: String,
    ): Response<ResponseBody>
}
