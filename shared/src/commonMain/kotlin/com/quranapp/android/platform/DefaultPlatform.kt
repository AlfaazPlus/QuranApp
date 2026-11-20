package com.quranapp.android.platform

import com.quranapp.android.generated.resources.Res

class DefaultAppAssets() : AppAssets {
    private fun bundledFilePath(relativePath: String): String =
        "files/${relativePath.trimStart('/')}"

    override suspend fun readAssetBytes(relativePath: String): ByteArray {
        return Res.readBytes(bundledFilePath(relativePath))

    }

    override suspend fun readAssetText(relativePath: String): String {
        return readAssetBytes(relativePath).decodeToString()
    }

    override suspend fun readAssetTextOrNull(relativePath: String): String? {
        return try {
            readAssetText(relativePath).takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun assetUri(relativePath: String): String {
        return Res.getUri(bundledFilePath(relativePath))
    }
}
