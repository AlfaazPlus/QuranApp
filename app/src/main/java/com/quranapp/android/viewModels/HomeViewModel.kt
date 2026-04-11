package com.quranapp.android.viewModels

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.quranapp.android.utils.app.UpdateManager

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private var didCheckForUpdates = false
    private var updateManager: UpdateManager? = null

    fun attachUpdateManager(context: Context) {
        if (updateManager == null) {
            updateManager = UpdateManager(context)
        }
        if (!didCheckForUpdates) {
            didCheckForUpdates = true
            updateManager?.showUpdateDialogsIfNeeded()
        }
    }

    fun detachUpdateManager() {
        updateManager = null
    }

    fun onPause() {
        updateManager?.onPause()
    }

    fun onResume() {
        updateManager?.onResume()
    }
}
