package com.quranapp.android.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

abstract class AppViewModel(
    application: Application,
) : AndroidViewModel(application) {
    // Distinct JVM name so this does not clash with AndroidViewModel.getApplication().
    @get:JvmName("viewModelApplication")
    val application: Application
        get() = getApplication()
}

class AppViewModelFactory(
    private val application: Application,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val constructor = modelClass.getDeclaredConstructor(Application::class.java)
        constructor.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        return constructor.newInstance(application) as T
    }
}
