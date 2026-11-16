package com.quranapp.android.api

import com.quranapp.android.api.models.AppUpdate
import com.quranapp.android.api.models.AppUrls
import com.quranapp.android.api.models.ResourcesVersions
import com.quranapp.android.api.models.tafsir.ChapterInfoApiResponse
import com.quranapp.android.api.models.tafsir.TafsirResponseModel
import com.quranapp.android.api.models.wbw.AvailableWbwInfoModel
import com.quranapp.android.core.BuildConfig
import com.quranapp.android.utils.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.http.encodeURLParameter
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.serialization.ExperimentalSerializationApi

/**
 * HTTP access for inventory and AlfaazPlus. Android engine today; the call
 * surface does not use Retrofit types so it can move to commonMain later.
 */
@OptIn(ExperimentalSerializationApi::class)
object ApiClient {
    var githubProxyBaseUrl: String = ApiConfig.GH_PROXY_BASE_URL
    var githubLikeProxyBaseUrl: String = ApiConfig.GH_PROXY_ROOT

    private val client = HttpClient(OkHttp) {
        expectSuccess = false

        // OkHttp's default read timeout is 10s. gh-proxy often takes longer than
        // that to send response headers, which shows up as socket_timeout=unknown.
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 60_000
            requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
        }
    }

    suspend fun getAppUpdates(): List<AppUpdate> =
        getJson(github("inventory/versions/app_updates.json"))

    suspend fun getResourcesVersions(): ResourcesVersions =
        getJson(github("inventory/versions/resources_versions.json"))

    suspend fun getAppUrls(): AppUrls =
        getJson(github("inventory/other/urls.json"))

    suspend fun getAvailableWbwInfo(): AvailableWbwInfoModel =
        getJson(github("inventory/wbw/available_wbw_info_v2.json"))

    suspend fun getAvailableTemplates(): HttpBody =
        open(github("inventory/templates/available_templates.json.gz"))

    suspend fun getAvailableTranslations(): String =
        getText(github("inventory/translations/available_translations_info.json"))

    suspend fun getTranslation(path: String): HttpBody =
        open(github(path))

    suspend fun getAvailableRecitations(): String =
        getText(github("inventory/recitations/available_recitations_info_v2.json"))

    suspend fun getAvailableRecitationTranslations(): String =
        getText(github("inventory/recitations/available_recitation_translations_info_v2.json"))

    suspend fun downloadStreaming(url: String): HttpBody = open(url)

    suspend fun getRawContent(path: String): HttpBody {
        val base = githubLikeProxyBaseUrl.trimEnd('/')
        return open("$base/${path.trimStart('/')}")
    }

    suspend fun getChapterInfo(
        chapterNo: Int,
        language: String,
        id: Int?,
    ): ChapterInfoApiResponse {
        val idQuery = if (id == null) "" else "&id=$id"
        return getJson(
            "${ApiConfig.ALFAAZPLUS_API_ROOT_URL}/quran/chapters/$chapterNo/info?language=${language.encodeURLParameter()}$idQuery"
        )
    }

    suspend fun getAvailableTafsirs(): String =
        getText("${ApiConfig.ALFAAZPLUS_API_ROOT_URL}/quran/tafsirs")

    suspend fun getTafsirsByVerse(
        tafsirKeys: String,
        verseKey: String,
    ): TafsirResponseModel = getJson(
        "${ApiConfig.ALFAAZPLUS_API_ROOT_URL}/quran/tafsirs/by_verse?keys=${tafsirKeys.encodeURLParameter()}&verse_key=${verseKey.encodeURLParameter()}"
    )

    suspend fun getTafsirsByChapter(
        tafsirKey: String,
        surahRange: String,
    ): TafsirResponseModel = getJson(
        "${ApiConfig.ALFAAZPLUS_API_ROOT_URL}/quran/tafsirs/by_surah?key=${tafsirKey.encodeURLParameter()}&surahs=${surahRange.encodeURLParameter()}"
    )

    fun resetGithubApi() {
        // Base URLs are read on each request.
    }

    private fun github(path: String): String {
        val base = githubProxyBaseUrl.trimEnd('/')
        val relative = path.trimStart('/')
        return "$base/$relative"
    }

    private suspend inline fun <reified T> getJson(url: String): T {
        val text = getText(url)
        return JsonHelper.json.decodeFromString(text)
    }

    private suspend fun getText(url: String): String {
        open(url).use { body ->
            if (!body.isSuccessful) error("HTTP ${body.code}")
            return body.string()
        }
    }

    private suspend fun open(url: String): HttpBody {
        Logger.print(url)
        val response = client.get(url) {
            header("X-QuranApp-Version", BuildConfig.VERSION_CODE.toString())
        }
        return response.toHttpBody()
    }
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
