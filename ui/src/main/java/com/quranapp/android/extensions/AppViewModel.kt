package com.quranapp.android.extensions

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quranapp.android.viewmodel.AppViewModelFactory

@Composable
inline fun <reified VM : ViewModel> appViewModel(
    key: String? = null,
): VM {
    val application = LocalContext.current.applicationContext as Application
    val factory = remember(application) { AppViewModelFactory(application) }
    return viewModel(key = key, factory = factory)
}
