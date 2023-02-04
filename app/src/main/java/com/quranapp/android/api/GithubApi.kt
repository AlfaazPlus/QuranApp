package com.quranapp.android.api

import com.quranapp.android.api.models.ResourcesVersions
import retrofit2.http.GET

interface GithubApi {
    @GET("inventory/versions/app_updates.json")
    suspend fun getAppUpdates(): String

    @GET("inventory/versions/resources_versions.json")
    suspend fun getResourcesVersions(): ResourcesVersions
}