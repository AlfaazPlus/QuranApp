package com.quranapp.android.models

sealed interface DataLoadError {
    object NoConnection : DataLoadError
    object NoData : DataLoadError
    object Failed : DataLoadError
}
