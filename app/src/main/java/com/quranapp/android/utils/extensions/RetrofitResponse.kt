package com.quranapp.android.utils.extensions

import com.google.common.net.HttpHeaders
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.InputStream

@Throws(Exception::class)
fun Response<ResponseBody>.getContentLengthAndStream(): Pair<Long, InputStream> {
    return Pair(
        headers()[HttpHeaders.CONTENT_LENGTH]?.toLong() ?: 0L,
        body()?.byteStream() ?: throw Exception("No content")
    )
}

@Throws(Exception::class)
fun ResponseBody.getContentLengthAndStream(): Pair<Long, InputStream> {
    return Pair(
        contentLength(),
        byteStream()
    )
}