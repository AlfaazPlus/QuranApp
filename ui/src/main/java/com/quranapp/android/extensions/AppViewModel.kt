package com.quranapp.android.extensions

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel

class AppViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val constructor = modelClass.getDeclaredConstructor()
        constructor.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        return constructor.newInstance() as T
    }
}

@Composable
inline fun <reified VM : ViewModel> appViewModel(
    key: String? = null,
): VM = viewModel(key = key, factory = AppViewModelFactory())
