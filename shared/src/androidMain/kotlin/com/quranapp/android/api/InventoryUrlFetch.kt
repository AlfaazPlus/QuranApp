package com.quranapp.android.api

import com.quranapp.android.utils.app.DownloadSourceUtils

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
 * - `ghraw://relative/path` - fetched from the active mirror root.
 * - Any other string - fetched as a full URL.
 */
suspend fun fetchInventoryStreamingResponse(url: String): HttpBody {
    return if (url.startsWith("ghraw://")) {
        ApiClient.getRawContent(url.removePrefix("ghraw://").trimStart('/'))
    } else {
        ApiClient.downloadStreaming(url)
    }
}
