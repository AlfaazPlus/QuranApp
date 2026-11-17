package com.quranapp.android.api

import com.quranapp.android.shared.BuildConfig
import com.quranapp.android.utils.app.Logger
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.utils.io.jvm.javaio.toInputStream

internal actual fun platformApiEngine(): HttpClientEngine = OkHttp.create()

internal actual fun appVersionCode(): String = BuildConfig.VERSION_CODE.toString()

suspend fun ApiClient.getAvailableTemplates(): HttpBody =
    openStream(githubUrl("inventory/templates/available_templates.json.gz"))

suspend fun ApiClient.getTranslation(path: String): HttpBody = openStream(githubUrl(path))

suspend fun ApiClient.downloadStreaming(url: String): HttpBody = openStream(url)

suspend fun ApiClient.getRawContent(path: String): HttpBody = openStream(rawUrl(path))

private suspend fun openStream(url: String): HttpBody {
    Logger.d(url)
    val response = ApiClient.client.get(url) {
        header("X-QuranApp-Version", appVersionCode())
    }
    return response.toHttpBody()
}

private suspend fun HttpResponse.toHttpBody(): HttpBody {
    val length = contentLength() ?: -1L
    return HttpBody(
        code = status.value,
        contentLength = length,
        stream = bodyAsChannel().toInputStream(),
        response = this,
    )
}
