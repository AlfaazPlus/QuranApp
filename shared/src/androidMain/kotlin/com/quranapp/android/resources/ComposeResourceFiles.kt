package com.quranapp.android.resources

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import kotlinx.coroutines.runBlocking
import okio.Source
import okio.source
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.FontResource
import org.jetbrains.compose.resources.getDrawableResourceBytes
import org.jetbrains.compose.resources.getFontResourceBytes
import org.jetbrains.compose.resources.getSystemResourceEnvironment
import org.jetbrains.compose.resources.setResourceReaderAndroidContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream

/**
 * Reads compose resources for Android APIs that still need a file, typeface, or bitmap.
 * Font bytes are written once into the app cache so later reads do not copy them again.
 */
object ComposeResourceFiles {
    private lateinit var appContext: Context
    private val fontFiles = HashMap<String, File>()

    @OptIn(ExperimentalResourceApi::class)
    fun install(context: Context) {
        appContext = context.applicationContext ?: context
        setResourceReaderAndroidContext(appContext)
    }

    fun fontFile(resource: FontResource, name: String): File {
        fontFiles[name]?.let { cached ->
            if (cached.exists() && cached.length() > 0L) return cached
        }

        val dir = File(appContext.cacheDir, "compose-fonts")
        val file = File(dir, name)
        if (!file.exists() || file.length() == 0L) {
            val bytes = runBlocking {
                getFontResourceBytes(getSystemResourceEnvironment(), resource)
            }
            dir.mkdirs()
            val tmp = File(dir, "$name.tmp")
            tmp.writeBytes(bytes)
            if (!tmp.renameTo(file)) {
                file.writeBytes(bytes)
                tmp.delete()
            }
        }
        fontFiles[name] = file
        return file
    }

    fun fontSource(resource: FontResource, name: String): Source =
        fontFile(resource, name).source()

    fun fontStream(resource: FontResource, name: String): InputStream =
        fontFile(resource, name).inputStream()

    fun typeface(resource: FontResource, name: String): Typeface =
        Typeface.createFromFile(fontFile(resource, name))

    fun drawableBytes(resource: DrawableResource): ByteArray = runBlocking {
        getDrawableResourceBytes(getSystemResourceEnvironment(), resource)
    }

    fun drawableStream(resource: DrawableResource): InputStream =
        ByteArrayInputStream(drawableBytes(resource))

    fun drawableBitmap(resource: DrawableResource): Bitmap? {
        val bytes = drawableBytes(resource)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
}
