package com.quranapp.android.utils.reader.atlas

import androidx.compose.ui.graphics.ImageBitmap
import com.quranapp.android.db.ExternalQuranDatabase
import com.quranapp.android.db.entities.quran.AyahWordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.ConcurrentHashMap

private const val ATLAS_PREFETCH_CHUNK = 400

class QuranAtlasBundle(
    val db: ExternalQuranDatabase,
    val key: String,
    val meta: AtlasMetaRoot,
    val layer: AtlasLayerJson,
    val bitmap: ImageBitmap
) {
    private val shapeCache = ConcurrentHashMap<String, List<AtlasGlyphPlacement>>()

    suspend fun getPlacements(word: String): List<AtlasGlyphPlacement> {
        return shapeCache.getOrPut(word) {
            QuranAtlasLoader.fetchShape(db, key, word) ?: emptyList()
        }
    }

    suspend fun prefetchShapes(wordTexts: Collection<String>) {
        val pending = wordTexts.asSequence()
            .filter { it.isNotEmpty() }
            .distinct()
            .filter { !shapeCache.containsKey(it) }
            .toList()

        if (pending.isEmpty()) return

        coroutineScope {
            pending.chunked(ATLAS_PREFETCH_CHUNK).map { chunk ->
                async(Dispatchers.IO) {
                    val rows = db.atlasWordShapeDao().getShapesForWords(key, chunk)

                    val byWord = rows.associateBy { it.word }

                    for (w in chunk) {
                        val list = byWord[w]?.let { entity ->
                            QuranAtlasLoader.decodePlacementsJson(entity.placementsJson)
                        } ?: emptyList()

                        shapeCache.putIfAbsent(w, list)
                    }
                }
            }.awaitAll()
        }
    }

    suspend fun getPlacementsForWords(words: List<AyahWordEntity>): Map<Int, List<AtlasGlyphPlacement>> {
        if (words.isEmpty()) return emptyMap()

        prefetchShapes(words.map { it.text })

        return buildMap(words.size) {
            for (word in words) {
                put(word.atlasPlacementMapKey(), shapeCache[word.text] ?: emptyList())
            }
        }
    }
}

fun Map<Int, List<AtlasGlyphPlacement>>.getForWord(word: AyahWordEntity): List<AtlasGlyphPlacement>? {
    return this[word.atlasPlacementMapKey()]
}

private fun AyahWordEntity.atlasPlacementMapKey(): Int =
    ayahId * 4096 + wordIndex
