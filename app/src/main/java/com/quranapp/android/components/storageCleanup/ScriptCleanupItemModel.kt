/*
 * Created by Faisal Khan on (c) 16/8/2021.
 */
package com.quranapp.android.components.storageCleanup

data class ScriptCleanupItemModel(
    val scriptKey: String,
    val fontDownloadsCount: Int,
    var isCleared: Boolean = false
)
