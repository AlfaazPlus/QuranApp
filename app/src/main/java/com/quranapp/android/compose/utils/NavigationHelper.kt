package com.quranapp.android.compose.utils

import android.content.Context
import android.content.Intent
import com.quranapp.android.utils.Log

object NavigationHelper {
    fun openAppSettings(context: Context) {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.saveError(e, "openAppSettings")
        }
    }
}