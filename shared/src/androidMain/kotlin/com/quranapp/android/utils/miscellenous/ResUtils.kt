package com.quranapp.android.utils.miscellenous

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import androidx.annotation.StringRes
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.Locale

object ResUtils {
    @JvmStatic
    fun getLocalizedResources(context: Context, locale: Locale): Resources {
        var conf = context.resources.configuration
        conf = Configuration(conf)
        conf.setLocale(locale)
        val localizedContext = context.createConfigurationContext(conf)
        return localizedContext.resources
    }

    @JvmStatic
    fun getLocalizedString(context: Context, @StringRes resId: Int, locale: Locale): String? {
        return try {
            getLocalizedResources(context, locale).getString(resId)
        } catch (e: java.lang.Exception) {
            null
        }
    }

    @JvmStatic
    fun getBitmapInputStream(bitmap: Bitmap): InputStream {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return ByteArrayInputStream(stream.toByteArray())
    }
}
