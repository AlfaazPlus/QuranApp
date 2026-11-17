package com.quranapp.android.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.quranapp.android.db.DatabaseProvider

abstract class AppViewModel : ViewModel() {
    protected val application: Context
        get() = DatabaseProvider.appContext()
}
