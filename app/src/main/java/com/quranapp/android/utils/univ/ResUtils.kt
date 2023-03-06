package com.quranapp.android.utils.univ

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import androidx.annotation.StringRes
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.*

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

    @JvmStatic
    fun readAssetsTextFile(context: Context, filename: String): String {
        var text = ""

        try {
            context.assets.open(filename).use { stream ->
                ByteArrayOutputStream().use { os ->
                    val buf = ByteArray(1024)
                    var len: Int
                    while (stream.read(buf).also { len = it } != -1) {
                        os.write(buf, 0, len)
                    }
                    text = os.toString()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return text
    }
}
