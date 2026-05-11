package com.quranapp.android.utils.reader.atlas

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.db.ExternalQuranDatabase
import com.quranapp.android.utils.reader.isPrebuiltAtlas
import com.quranapp.android.utils.reader.isQuranAtlasScript
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

object QuranAtlasLoader {
    private val bundleCache = ConcurrentHashMap<String, QuranAtlasBundle>()
    val isImporting = mutableStateOf(false)

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
        word: String
    ): List<AtlasGlyphPlacement>? {
        val entity = db.atlasWordShapeDao().getShape(bundleKey, word) ?: return null
        return decodePlacementsJson(entity.placementsJson)
    }

    suspend fun getBundle(
        context: Context,
        db: ExternalQuranDatabase,
        bundleKey: String,
    ): QuranAtlasBundle? =
        withContext(Dispatchers.IO) {
            bundleCache[bundleKey]?.let { return@withContext it }

            var ent = db.atlasWordShapeDao().getBundleByKey(bundleKey)

            if (ent == null && bundleKey.isPrebuiltAtlas()) {
                try {
                    withContext(Dispatchers.Main) { isImporting.value = true }

                    val assetPath = AtlasManager.getPrebuiltAtlasAssetPath(bundleKey)

                    context.assets.open(assetPath).use {
                        ent = AtlasManager.importAtlasFromZip(context, it, bundleKey, db)
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

                val pngFile = AtlasManager.getBundlePngFile(context, bundleKey)
                val options = BitmapFactory.Options().apply {
                    inScaled = false
                    // Since the atlas is "L" (Luminance), ALPHA_8 is the most efficient.
                    // It uses 1 byte per pixel instead of 4, saving ~48MB for a 4096px atlas.
                    inPreferredConfig = Bitmap.Config.ALPHA_8
                }
                val bitmap = BitmapFactory.decodeFile(pngFile.path, options)
                    ?: return@withContext null

                val bundle = QuranAtlasBundle(
                    db,
                    key = bundleKey,
                    meta = meta,
                    layer = layer,
                    bitmap = bitmap.asImageBitmap()
                )

                bundleCache[bundleKey] = bundle
                bundle
            } catch (e: Exception) {
                null
            }
        }

    fun clearCache() {
        bundleCache.clear()
    }
}

val LocalQuranAtlasBundle = staticCompositionLocalOf<QuranAtlasBundle?> { null }

@Composable
fun rememberQuranAtlasBundle(db: ExternalQuranDatabase): QuranAtlasBundle? {
    val context = LocalContext.current
    val script = ReaderPreferences.observeQuranScript()

    if (!script.isQuranAtlasScript()) return null

    val bundleState = produceState<QuranAtlasBundle?>(initialValue = null, script, context) {
        value = QuranAtlasLoader.getBundle(context, db, script)
    }

    return bundleState.value
}
