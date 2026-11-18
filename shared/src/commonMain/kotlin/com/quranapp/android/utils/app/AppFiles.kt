package com.quranapp.android.utils.app

import com.quranapp.android.platform.PlatformServices
import okio.FileSystem
import okio.GzipSource
import okio.IOException
import okio.Path
import okio.buffer

object AppFiles {
    private const val SAVED_DATA_ROOT = "downloaded/saved_data"
    private const val LOGS_ROOT = "logs"

    private val files get() = PlatformServices.files

    fun savedDir(name: String, vararg nested: String): Path {
        return files.resourceDirectory(relative(SAVED_DATA_ROOT, name, *nested))
    }

    fun savedFile(dirName: String, vararg relativeParts: String): Path {
        require(relativeParts.isNotEmpty())

        val parentParts = ArrayList<String>(relativeParts.size + 1)
        parentParts.add(SAVED_DATA_ROOT)
        parentParts.add(dirName)
        for (index in 0 until relativeParts.lastIndex) {
            parentParts.add(relativeParts[index])
        }

        return files.resourceDirectory(parentParts.joinToString("/")) / relativeParts.last()
    }

    fun logDir(name: String): Path {
        return files.resourceDirectory(relative(LOGS_ROOT, name))
    }

    fun createFile(path: Path): Boolean {
        val fs = FileSystem.SYSTEM

        return try {
            if (fs.exists(path)) return true

            path.parent?.let { fs.createDirectories(it) }

            fs.sink(path, mustCreate = true).close()
            true
        } catch (_: IOException) {
            false
        }
    }

    fun readText(path: Path): String {
        return FileSystem.SYSTEM.source(path).buffer().use { source ->
            source.readUtf8()
        }
    }

    fun readTextGzipped(path: Path): String {
        return FileSystem.SYSTEM.source(path).buffer().use { source ->
            GzipSource(source).buffer().use { it.readUtf8() }
        }
    }

    fun writeText(path: Path, text: String) {
        FileSystem.SYSTEM.write(path) {
            writeUtf8(text)
        }
    }

    fun deleteRecursively(path: Path) {
        if (metadata(path) == null) return
        FileSystem.SYSTEM.deleteRecursively(path, mustExist = false)
    }

    fun delete(path: Path) {
        if (metadata(path) == null) return
        FileSystem.SYSTEM.delete(path, mustExist = false)
    }

    fun move(src: Path, dst: Path) {
        try {
            FileSystem.SYSTEM.atomicMove(src, dst)
        } catch (e: Exception) {
            FileSystem.SYSTEM.copy(src, dst)
            delete(src)
        }
    }

    fun exists(path: Path): Boolean = metadata(path) != null

    fun size(path: Path): Long = metadata(path)?.size ?: 0L

    fun metadata(path: Path) = FileSystem.SYSTEM.metadataOrNull(path)

    private fun relative(vararg parts: String): String {
        return parts.filter { it.isNotEmpty() }.joinToString("/")
    }
}
