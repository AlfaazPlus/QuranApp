package com.quranapp.android.utils.reader.importing

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream

private const val MAX_BYTES = 20 * 1024 * 1024

fun readImportText(context: Context, uri: Uri): String {
    val input = context.contentResolver.openInputStream(uri)
        ?: throw ImportException(ImportFailure.UNREADABLE)

    return input.use { stream ->
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val read = stream.read(buffer)
            if (read < 0) break
            total += read
            if (total > MAX_BYTES) throw ImportException(ImportFailure.TOO_LARGE)
            output.write(buffer, 0, read)
        }
        output.toString(Charsets.UTF_8.name())
    }
}

fun readImportFileName(context: Context, uri: Uri): String {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            if (cursor.moveToFirst()) {
                val name = cursor.getString(0)
                if (!name.isNullOrBlank()) return name
            }
        }
    return uri.lastPathSegment ?: "import.json"
}
