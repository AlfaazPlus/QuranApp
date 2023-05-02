package com.quranapp.android.api

import com.quranapp.android.api.models.AppUpdate
import com.quranapp.android.api.models.AppUrls
import com.quranapp.android.api.models.ResourcesVersions
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming

interface GithubApi {
    @GET("inventory/versions/app_updates.json")
    suspend fun getAppUpdates(): List<AppUpdate>

    @GET("inventory/versions/resources_versions.json")
    suspend fun getResourcesVersions(): ResourcesVersions

    @GET("inventory/other/urls.json")
    suspend fun getAppUrls(): AppUrls

    @GET("inventory/translations/available_translations_info.json")
    suspend fun getAvailableTranslations(): ResponseBody

    @GET("{path}")
    @Streaming
    suspend fun getTranslation(@Path("path") path: String):  Response<ResponseBody>

    @GET("inventory/quran_scripts/{filename}")
    @Streaming
    suspend fun getQuranScript(@Path("filename") filename: String): Response<ResponseBody>

    @GET("inventory/fonts/{scriptKey}/{part}")
    @Streaming
    suspend fun getKFQPCFont(
        @Path("scriptKey") scriptKey: String,
        @Path("part") part: String
    ): Response<ResponseBody>

    @GET("inventory/recitations/available_recitations_info.json")
    suspend fun getAvailableRecitations(): ResponseBody

    @GET("inventory/recitations/available_recitation_translations_info.json")
    suspend fun getAvailableRecitationTranslations(): ResponseBody

    @GET("inventory/tafsirs/available_tafsirs_info.json")
    suspend fun getAvailableTafsirs(): ResponseBody
}
