package com.quranapp.android.error

sealed interface DataLoadError {
    object NoConnection : DataLoadError
    object NoData : DataLoadError
    object Failed : DataLoadError
}
