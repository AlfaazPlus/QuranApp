package com.quranapp.android.viewModels

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.peacedesign.android.utils.AppBridge
import com.quranapp.android.utils.app.UpdateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val _showInlineUpdateBanner = MutableStateFlow(false)
    val showInlineUpdateBanner: StateFlow<Boolean> = _showInlineUpdateBanner.asStateFlow()

    private var didCheckForUpdates = false
    private var updateManager: UpdateManager? = null

    fun attachUpdateManager(context: Context) {
        if (updateManager == null) {
            updateManager = UpdateManager(context)
        }

        if (!didCheckForUpdates) {
            didCheckForUpdates = true
            updateManager?.check4Update { showInlineBanner ->
                _showInlineUpdateBanner.value = showInlineBanner
            }
        }
    }

    fun detachUpdateManager() {
        updateManager = null
    }

    fun openUpdateInPlayStore() {
        updateManager?.openPlayStore()
            ?: AppBridge.newOpener(getApplication()).openPlayStore(null)
    }

    fun onPause() {
        updateManager?.onPause()
    }

    fun onResume() {
        updateManager?.onResume()
    }
}
