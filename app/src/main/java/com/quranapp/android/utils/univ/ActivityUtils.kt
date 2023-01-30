package com.quranapp.android.utils.univ

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object ActivityUtils {
    fun openAppDetailsActivity(context: Context) {
        val settingIntent = Intent()
        settingIntent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        settingIntent.addCategory(Intent.CATEGORY_DEFAULT)
        settingIntent.data = Uri.parse("package:" + context.packageName)
        settingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        settingIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        settingIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        context.startActivity(settingIntent)
    }
}