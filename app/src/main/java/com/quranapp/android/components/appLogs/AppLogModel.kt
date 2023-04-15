package com.quranapp.android.components.appLogs

import java.io.File

data class AppLogModel(
    val datetime: String,
    val place: String,
    val file: File,
    val log: String,
    val logShort: String
)
