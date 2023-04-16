/*
 * Created by Faisal Khan on (c) 16/8/2021.
 */
package com.quranapp.android.api.models.recitation

import java.io.Serializable

data class RecitationManageAudioInfoModel(
    val slug: String,
    val reciter: String,
    val urlHost: String,
    val urlPath: String,
    val langCode: String?,
    val langName: String?,
    val book: String?,
    val isTranslation: Boolean,
) : Serializable
