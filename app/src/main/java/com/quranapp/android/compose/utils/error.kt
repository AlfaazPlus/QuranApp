package com.quranapp.android.compose.utils

sealed interface DataLoadError {
    object NoConnection : DataLoadError
    object NoData : DataLoadError
    object Failed : DataLoadError
}
