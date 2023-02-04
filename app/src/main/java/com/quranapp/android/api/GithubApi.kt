package com.quranapp.android.api

import com.quranapp.android.api.models.ResourcesVersions
import com.quranapp.android.components.AppUpdate
import retrofit2.http.GET

interface GithubApi {
    @GET("inventory/versions/app_updates.json")
    suspend fun getAppUpdates(): List<AppUpdate>

    @GET("inventory/versions/resources_versions.json")
    suspend fun getResourcesVersions(): ResourcesVersions
}