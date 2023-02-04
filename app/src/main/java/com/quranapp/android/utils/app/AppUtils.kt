package com.quranapp.android.utils.app

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.telephony.TelephonyManager
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.peacedesign.android.utils.WindowUtils
import com.quranapp.android.BuildConfig
import com.quranapp.android.R
import com.quranapp.android.utils.sp.SPAppConfigs
import com.quranapp.android.utils.univ.FileUtils
import java.util.*

object AppUtils {
    @JvmField
    val BASE_APP_DOWNLOADED_SAVED_DATA_DIR: String = FileUtils.createPath("downloaded", "saved_data")!!

    @JvmField
    val APP_OTHER_DIR: String = FileUtils.createPath(BASE_APP_DOWNLOADED_SAVED_DATA_DIR, "other")!!

    @JvmStatic
    fun resolveThemeTextFromMode(context: Context): String {
        return context.getString(
            when (SPAppConfigs.getThemeMode(context)) {
                SPAppConfigs.THEME_MODE_DARK -> R.string.strLabelThemeDark
                SPAppConfigs.THEME_MODE_LIGHT -> R.string.strLabelThemeLight
                SPAppConfigs.THEME_MODE_DEFAULT -> R.string.strLabelSystemDefault
                else -> R.string.strLabelSystemDefault
            }
        )
    }

    @JvmStatic
    fun resolveThemeIdFromMode(context: Context?): Int {
        return when (SPAppConfigs.getThemeMode(context)) {
            SPAppConfigs.THEME_MODE_DARK -> R.id.themeDark
            SPAppConfigs.THEME_MODE_LIGHT -> R.id.themeLight
            SPAppConfigs.THEME_MODE_DEFAULT -> R.id.systemDefault
            else -> R.id.systemDefault
        }
    }

    @JvmStatic
    fun resolveThemeModeFromId(id: Int): String {
        return when (id) {
            R.id.themeDark -> {
                SPAppConfigs.THEME_MODE_DARK
            }
            R.id.themeLight -> {
                SPAppConfigs.THEME_MODE_LIGHT
            }
            else -> {
                SPAppConfigs.THEME_MODE_DEFAULT
            }
        }
    }

    @JvmStatic
    fun resolveThemeModeFromSP(context: Context?): Int {
        return when (SPAppConfigs.getThemeMode(context)) {
            SPAppConfigs.THEME_MODE_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            SPAppConfigs.THEME_MODE_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            SPAppConfigs.THEME_MODE_DEFAULT -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
    }

    @JvmStatic
    fun getAppConfigs(ctx: Context?): Map<String, Any> {
        return HashMap<String, Any>().apply {
            this["locale"] = Locale.getDefault().toString()
            this["theme"] = SPAppConfigs.getThemeMode(ctx)
            this["orientation"] = if (WindowUtils.isLandscapeMode(ctx!!)) "landscape" else "portrait"
        }
    }

    @JvmStatic
    fun getDeviceInformation(ctx: Context): Map<String, Any?> {
        return HashMap<String, Any?>().apply {
            this["appVersionCode"] = BuildConfig.VERSION_CODE
            this["appVersionName"] = BuildConfig.VERSION_NAME
            this["screenSize"] = getScreenSize(ctx)
            this["timeZone"] = TimeZone.getDefault().apply { "${getDisplayName(false, TimeZone.SHORT)} $id" }
            this["country"] = ContextCompat.getSystemService(ctx, TelephonyManager::class.java)?.networkCountryIso
            this["model"] = Build.MODEL
            this["id"] = Build.ID
            this["manufacturer"] = Build.MANUFACTURER
            this["brand"] = Build.BRAND
            this["sdkInt"] = Build.VERSION.SDK_INT
            this["versionRelease"] = Build.VERSION.RELEASE
        }
    }

    private fun getScreenSize(ctx: Context): String? {
        val wm = ContextCompat.getSystemService(
            ctx,
            WindowManager::class.java
        )
            ?: return null
        val out = Point()
        wm.defaultDisplay.getRealSize(out)
        return out.x.toString() + "*" + out.y
    }
}