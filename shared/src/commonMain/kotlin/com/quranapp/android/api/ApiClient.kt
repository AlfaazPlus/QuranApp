package com.quranapp.android.api

import com.quranapp.android.api.models.AppUpdate
import com.quranapp.android.api.models.AppUrls
import com.quranapp.android.api.models.ResourcesVersions
import com.quranapp.android.api.models.tafsir.ChapterInfoApiResponse
import com.quranapp.android.api.models.tafsir.TafsirResponseModel
import com.quranapp.android.api.models.wbw.AvailableWbwInfoModel
import com.quranapp.android.utils.app.APP_VERSION_CODE
import com.quranapp.android.utils.app.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.contentLength
import io.ktor.http.encodeURLParameter
import io.ktor.http.isSuccess
import kotlinx.serialization.ExperimentalSerializationApi

@OptIn(ExperimentalSerializationApi::class)
object ApiClient {
    var githubProxyBaseUrl: String = ApiConfig.GH_PROXY_BASE_URL
    var githubLikeProxyBaseUrl: String = ApiConfig.GH_PROXY_ROOT

    internal val client: HttpClient = createApiHttpClient(OkHttp.create())


    suspend fun getAppUpdates(): List<AppUpdate> =
        getJson(ghRepoUrl("inventory/versions/app_updates.json"))

    suspend fun getResourcesVersions(): ResourcesVersions =
        getJson(ghRepoUrl("inventory/versions/resources_versions.json"))

    suspend fun getAppUrls(): AppUrls =
        getJson(ghRepoUrl("inventory/other/urls.json"))

    suspend fun getAvailableWbwInfo(): AvailableWbwInfoModel =
        getJson(ghRepoUrl("inventory/wbw/available_wbw_info_v2.json"))

    suspend fun getAvailableTranslations(): String =
        getText(ghRepoUrl("inventory/translations/available_translations_info.json"))

    suspend fun getTranslation(path: String): HttpBody = openStream(ghRepoUrl(path))

    suspend fun getAvailableRecitations(): String =
        getText(ghRepoUrl("inventory/recitations/available_recitations_info_v2.json"))

    suspend fun getAvailableRecitationTranslations(): String =
        getText(ghRepoUrl("inventory/recitations/available_recitation_translations_info_v2.json"))

    suspend fun getAvailableTemplates(): HttpBody =
        openStream(ghRepoUrl("inventory/templates/available_templates.json.gz"))

    suspend fun getChapterInfo(
        chapterNo: Int,
        language: String,
        id: Int?,
    ): ChapterInfoApiResponse {
        val idQuery = if (id == null) "" else "&id=$id"
        return getJson(
            "${ApiConfig.ALFAAZPLUS_API_ROOT_URL}/quran/chapters/$chapterNo/info?language=${language.encodeURLParameter()}$idQuery",
        )
    }

    suspend fun getAvailableTafsirs(): String =
        getText("${ApiConfig.ALFAAZPLUS_API_ROOT_URL}/quran/tafsirs")

    suspend fun getTafsirsByVerse(
        tafsirKeys: String,
        verseKey: String,
    ): TafsirResponseModel = getJson(
        "${ApiConfig.ALFAAZPLUS_API_ROOT_URL}/quran/tafsirs/by_verse?keys=${tafsirKeys.encodeURLParameter()}&verse_key=${verseKey.encodeURLParameter()}",
    )

    suspend fun getTafsirsByChapter(
        tafsirKey: String,
        surahRange: String,
    ): TafsirResponseModel = getJson(
        "${ApiConfig.ALFAAZPLUS_API_ROOT_URL}/quran/tafsirs/by_surah?key=${tafsirKey.encodeURLParameter()}&surahs=${surahRange.encodeURLParameter()}",
    )

    fun resetGithubApi() {
        // Base URLs are read on each request.
    }

    fun ghRepoUrl(path: String): String {
        val base = githubProxyBaseUrl.trimEnd('/')
        return "$base/${path.trimStart('/')}"
    }

    fun ghRawUrl(path: String): String {
        val base = githubLikeProxyBaseUrl.trimEnd('/')
        return "$base/${path.trimStart('/')}"
    }

    private suspend inline fun <reified T> getJson(url: String): T {
        return JsonHelper.json.decodeFromString(getText(url))
    }

    private suspend fun getText(url: String): String {
        Logger.d(url)
        val response = client.get(url) {
            header("X-QuranApp-Version", APP_VERSION_CODE)
        }
        if (!response.status.isSuccess()) error("HTTP ${response.status.value}")
        return response.bodyAsText()
    }

    suspend fun downloadStreaming(url: String): HttpBody = openStream(url)

    suspend fun getRawContent(path: String): HttpBody = openStream(ghRawUrl(path))

    suspend fun openStream(url: String): HttpBody {
        Logger.d(url)

        val response = ApiClient.client.get(url) {
            header("X-QuranApp-Version", APP_VERSION_CODE)
        }

        return HttpBody(
            code = response.status.value,
            contentLength = response.contentLength(),
            channel = response.bodyAsChannel(),
            response = response
        )
    }

}
