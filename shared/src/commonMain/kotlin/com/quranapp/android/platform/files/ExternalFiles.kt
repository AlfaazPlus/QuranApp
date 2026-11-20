package com.quranapp.android.platform.files

import okio.Sink
import okio.Source

/**
 * Files outside app-private storage: documents the user picks, and public media
 * the app inserts (for example Pictures).
 */
interface ExternalFiles {
    fun displayName(location: String): String?

    fun source(location: String): Source

    fun sink(location: String): Sink

    fun readText(location: String): String

    fun retain(location: String): Boolean

    fun release(location: String)

    fun view(location: String, mimeType: String): Boolean

    fun insert(
        directory: PublicDirectory,
        album: String,
        displayName: String,
        mimeType: String,
    ): Sink
}

enum class PublicDirectory {
    Pictures,
}

/** The location is missing or this process cannot access it. */
class FileUnavailableException : Exception()
