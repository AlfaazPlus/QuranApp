package com.quranapp.android.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ResourcesVersions(
    @SerialName("urls") val urlsVersion: Long,
    @Deprecated("Translation manifest are always loaded on demand on the download screen.")
    @SerialName("translations") val translationsVersion: Long,
    @SerialName("recitations") val recitationsVersion: Long,
    @SerialName("recitationTranslations") val recitationTranslationsVersion: Long,
    @SerialName("tafsirs") val tafsirsVersion: Long,
    @SerialName("wbw") val wbwVersion: Long
)
