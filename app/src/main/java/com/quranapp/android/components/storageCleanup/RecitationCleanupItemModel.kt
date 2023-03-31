/*
 * Created by Faisal Khan on (c) 16/8/2021.
 */
package com.quranapp.android.components.storageCleanup

import com.quranapp.android.api.models.recitation.RecitationModel

data class RecitationCleanupItemModel(
    val recitationModel: RecitationModel,
    val downloadsCount: Int,
    var isCleared: Boolean = false
)
