package com.quranapp.android.utils.reader.atlas

import android.content.Context
import com.quranapp.android.db.ExternalQuranDatabase
import com.quranapp.android.platform.PlatformServices
import com.quranapp.android.utils.reader.isPrebuiltAtlas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

object QuranAtlasLoader {
    private val bundleCache = ConcurrentHashMap<String, QuranAtlasBundle>()
    private val bundleMutex = Mutex()
    val isImporting = MutableStateFlow(false)

    internal fun decodePlacementsJson(placementsJson: String): List<AtlasGlyphPlacement> {
        return try {
            atlasJson.decodeFromString(atlasPlacementListSerializer, placementsJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun fetchShape(
        db: ExternalQuranDatabase,
        bundleKey: String,
        word: String,
        page: Int,
    ): List<AtlasGlyphPlacement>? {
        val entity = db.atlasWordShapeDao().getShape(bundleKey, word, page) ?: return null

        return decodePlacementsJson(entity.placementsJson)
    }

    suspend fun getBundle(
        context: Context,
        db: ExternalQuranDatabase,
        bundleKey: String,
    ): QuranAtlasBundle? =
    // Single-flight so LocalQuranAtlasBundle and page builders share one instance.
    // Parallel loads used to install a second twin and clear the first's textures,
        // leaving reserved glyph advance with no ink.
        bundleMutex.withLock {
            withContext(Dispatchers.IO) {
                bundleCache[bundleKey]?.let { return@withContext it }

                var ent = db.atlasWordShapeDao().getBundleByKey(bundleKey)

                if (ent == null && bundleKey.isPrebuiltAtlas()) {
                    try {
                        withContext(Dispatchers.Main) { isImporting.value = true }

                        val assetPath = AtlasManager.getPrebuiltAtlasAssetPath(bundleKey)

                        PlatformServices.assets
                            .readAssetBytes(assetPath)
                            .inputStream()
                            .use {
                                ent = AtlasManager.importAtlasFromZip(it, bundleKey, db)
                            }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        withContext(Dispatchers.Main) { isImporting.value = false }
                    }
                }

                val entity = ent
                if (entity == null) return@withContext null

                try {
                    val meta = atlasJson.decodeFromString<AtlasMetaRoot>(entity.metaJson)
                    val layer = atlasJson.decodeFromString<AtlasLayerJson>(entity.layerJson)

                    val textureFiles = linkedMapOf<Int, java.io.File>()

                    for (t in layer.textures.sortedBy { it.index }) {
                        val imageFile = AtlasManager.atlasTextureFile(bundleKey, t.index)

                        if (!imageFile.isFile || imageFile.length() == 0L) {
                            return@withContext null
                        }

                        textureFiles[t.index] = imageFile
                    }

                    if (textureFiles.isEmpty()) {
                        return@withContext null
                    }

                    val bundle = QuranAtlasBundle(
                        db,
                        key = bundleKey,
                        meta = meta,
                        layer = layer,
                        textureStore = QuranAtlasTextureStore(textureFiles),
                    )

                    // Release bitmaps for other scripts only — never clear the bundle we
                    // are installing (or a cache-hit of this key).
                    bundleCache.forEach { (key, cached) ->
                        if (key != bundleKey) {
                            cached.clearTextureCache()
                        }
                    }
                    bundleCache[bundleKey] = bundle
                    bundle
                } catch (e: Exception) {
                    null
                }
            }
        }

    fun clearCache() {
        bundleCache.values.forEach { it.clearTextureCache() }
        bundleCache.clear()
    }
}
