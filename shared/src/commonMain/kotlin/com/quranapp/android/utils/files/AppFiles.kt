package com.quranapp.android.utils.files

import com.quranapp.android.platform.PlatformServices
import okio.FileSystem
import okio.Path

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

    fun createFile(file: Path): Boolean = files.createFile(file)

    fun readText(file: Path): String = files.readText(file)

    fun writeText(file: Path, text: String) {
        files.writeText(file, text)
    }

    fun deleteRecursively(file: Path) {
        if (metadata(file) == null) return
        files.deleteRecursively(file)
    }

    fun exists(path: Path): Boolean = metadata(path) != null

    fun size(path: Path): Long = metadata(path)?.size ?: 0L

    private fun metadata(path: Path) = FileSystem.SYSTEM.metadataOrNull(path)

    private fun relative(vararg parts: String): String {
        return parts.filter { it.isNotEmpty() }.joinToString("/")
    }
}
