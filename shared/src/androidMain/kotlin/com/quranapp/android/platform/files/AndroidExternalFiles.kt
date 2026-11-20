package com.quranapp.android.platform.files

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.net.toUri
import kotlinx.io.files.FileNotFoundException
import okio.Buffer
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.Sink
import okio.Source
import okio.Timeout
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class AndroidExternalFiles(
    context: Context,
) : ExternalFiles {
    private val appContext = context.applicationContext ?: context
    private val resolver get() = appContext.contentResolver

    override fun displayName(location: String): String? {
        if (!location.isDocumentUri()) {
            return location.toPath().name.takeIf { it.isNotBlank() }
        }

        return try {
            val cursor = resolver.query(
                location.toUri(),
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null,
            ) ?: return null

            cursor.use {
                if (!it.moveToFirst()) return null
                it.getString(0)?.takeIf { name -> name.isNotBlank() }
            }
        } catch (error: Exception) {
            when (error) {
                is FileNotFoundException, is SecurityException, is IllegalArgumentException -> null
                else -> location.toUri().lastPathSegment
            }
        }
    }

    override fun source(location: String): Source {
        if (!location.isDocumentUri()) return FileSystem.SYSTEM.source(location.toPath())

        return openLocation(location) {
            resolver.openInputStream(location.toUri())?.source()
        }
    }

    override fun sink(location: String): Sink {
        if (!location.isDocumentUri()) return FileSystem.SYSTEM.sink(location.toPath())

        val uri = location.toUri()
        return openLocation(location) {
            val stream = try {
                resolver.openOutputStream(uri, "wt")
            } catch (_: IllegalArgumentException) {
                resolver.openOutputStream(uri)
            }

            stream?.sink()
        }
    }

    override fun readText(location: String): String {
        return source(location).buffer().use { it.readUtf8() }
    }

    override fun retain(location: String): Boolean {
        if (!location.isDocumentUri()) return true

        val uri = location.toUri()
        return try {
            resolver.takePersistableUriPermission(uri, PERSISTABLE_URI_FLAGS)
            true
        } catch (_: SecurityException) {
            try {
                resolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
                true
            } catch (_: SecurityException) {
                false
            }
        }
    }

    override fun release(location: String) {
        if (!location.isDocumentUri()) return

        runCatching {
            resolver.releasePersistableUriPermission(location.toUri(), PERSISTABLE_URI_FLAGS)
        }
    }

    override fun view(location: String, mimeType: String): Boolean {
        val data = if (location.isDocumentUri()) {
            location.toUri()
        } else {
            Uri.fromFile(File(location))
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(data, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return runCatching { appContext.startActivity(intent) }.isSuccess
    }

    override fun insert(
        directory: PublicDirectory,
        album: String,
        displayName: String,
        mimeType: String,
    ): Sink {
        return when (directory) {
            PublicDirectory.Pictures -> insertPicture(album, displayName, mimeType)
        }
    }

    private fun insertPicture(album: String, displayName: String, mimeType: String): Sink {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/" + album,
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw FileUnavailableException()

            val stream = resolver.openOutputStream(uri) ?: run {
                resolver.delete(uri, null, null)
                throw FileUnavailableException()
            }

            return PublishingSink(stream) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
        }

        @Suppress("DEPRECATION")
        val pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val dir = File(pictures, album).apply { mkdirs() }
        val file = File(dir, displayName)
        val stream = FileOutputStream(file)

        return PublishingSink(stream) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DATA, file.absolutePath)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            }
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        }
    }

    private fun <T> openLocation(location: String, open: () -> T?): T {
        try {
            return open() ?: throw FileNotFoundException(location)
        } catch (error: Exception) {
            when (error) {
                is FileNotFoundException, is SecurityException, is IllegalArgumentException ->
                    throw FileUnavailableException()

                else -> throw error
            }
        }
    }
}

private class PublishingSink(
    private val stream: OutputStream,
    private val onPublished: () -> Unit,
) : Sink {
    private val delegate = stream.sink()
    private var closed = false

    override fun write(source: Buffer, byteCount: Long) {
        delegate.write(source, byteCount)
    }

    override fun flush() {
        delegate.flush()
    }

    override fun timeout(): Timeout = delegate.timeout()

    override fun close() {
        if (closed) return
        closed = true

        try {
            delegate.close()
            onPublished()
        } catch (error: Exception) {
            runCatching { stream.close() }
            throw error
        }
    }
}

private fun String.isDocumentUri(): Boolean {
    return startsWith("content:") || startsWith("file:")
}

private const val PERSISTABLE_URI_FLAGS =
    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
