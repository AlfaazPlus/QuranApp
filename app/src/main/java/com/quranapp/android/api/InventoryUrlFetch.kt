package com.quranapp.android.api

import okhttp3.ResponseBody
import retrofit2.Response

/**
 * Resolves inventory URLs for streaming download.
 *
 * - `ghraw://relative/path` - relative path after the scheme is fetched via [RetrofitInstance.githubLike]
 *   (mirror root from user settings).
 * - Any other string - treated as a full URL and fetched via [RetrofitInstance.any].
 */
suspend fun fetchInventoryStreamingResponse(url: String): Response<ResponseBody> {
    return if (url.startsWith("ghraw://")) {
        RetrofitInstance.githubLike.getRawContent(
            url.removePrefix("ghraw://").trimStart('/'),
        )
    } else {
        RetrofitInstance.any.downloadStreaming(url)
    }
}
