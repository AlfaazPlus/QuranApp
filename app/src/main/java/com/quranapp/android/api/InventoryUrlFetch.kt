package com.quranapp.android.api

import com.quranapp.android.utils.app.DownloadSourceUtils
import okhttp3.ResponseBody
import retrofit2.Response

/**
 * Resolves inventory URLs based on current download source preference.
 *
 * - `ghraw://relative/path` - converted into a fully qualified URL with the
 *   active mirror root from [DownloadSourceUtils.getDownloadSourceRoot].
 * - Any other string - returned as-is.
 */
fun resolveInventoryUrl(url: String): String {
    return if (url.startsWith("ghraw://")) {
        DownloadSourceUtils.getDownloadSourceRoot() + url.removePrefix("ghraw://").trimStart('/')
    } else {
        url
    }
}

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
