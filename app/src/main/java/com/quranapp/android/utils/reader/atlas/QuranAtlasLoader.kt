package com.quranapp.android.utils.reader.atlas

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.db.ExternalQuranDatabase
import com.quranapp.android.utils.reader.isQuranAtlasScript
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

object QuranAtlasLoader {
    private val bundleCache = ConcurrentHashMap<String, QuranAtlasBundle>()

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

            val entity = db.atlasWordShapeDao().getBundleByKey(bundleKey) ?: return@withContext null

            try {
                val meta = atlasJson.decodeFromString<AtlasMetaRoot>(entity.metaJson)
                val layer = atlasJson.decodeFromString<AtlasLayerJson>(entity.layerJson)

                val pngFile = AtlasManager.getBundlePngFile(context, bundleKey)
                val bitmap = BitmapFactory.decodeFile(pngFile.path)
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
