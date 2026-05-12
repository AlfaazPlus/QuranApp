package com.quranapp.android.utils.reader.atlas

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import com.quranapp.android.db.ExternalQuranDatabase
import com.quranapp.android.db.entities.atlas.AtlasWordShapeEntity
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
    private val textureStore: QuranAtlasTextureStore,
) {
    val textureCount: Int
        get() = textureStore.size

    fun textureForGlyph(glyph: AtlasGlyphJson): ImageBitmap? =
        textureStore.get(glyph.textureIndex)?.imageBitmap

    fun androidTextureForGlyph(glyph: AtlasGlyphJson): Bitmap? =
        textureStore.get(glyph.textureIndex)?.bitmap

    fun singleAndroidTexture(): Bitmap? =
        textureStore.onlyIndex?.let { textureStore.get(it)?.bitmap }

    private val shapeCache = ConcurrentHashMap<String, List<AtlasGlyphPlacement>>()

    private fun resolveQueryPage(readerPage: Int): Int =
        if (meta.isPageScopedGlyphAtlas() && readerPage > 0) {
            readerPage
        } else {
            AtlasWordShapeEntity.ATLAS_PAGE_NONE
        }

    private fun cacheKey(page: Int, word: String): String = "$page\u0000$word"

    suspend fun getPlacements(word: String, pageNo: Int): List<AtlasGlyphPlacement> {
        val queryPage = resolveQueryPage(pageNo)
        val ck = cacheKey(queryPage, word)

        val placements = shapeCache.getOrPut(ck) {
            QuranAtlasLoader.fetchShape(db, key, word, queryPage) ?: emptyList()
        }

        prefetchTexturesForPlacements(placements)

        return placements
    }

    suspend fun prefetchShapes(pairs: Collection<Pair<String, Int>>) {
        val distinct = pairs
            .asSequence()
            .filter { it.first.isNotEmpty() }
            .map { (w, rp) -> w to resolveQueryPage(rp) }
            .distinctBy { cacheKey(it.second, it.first) }
            .filter { !shapeCache.containsKey(cacheKey(it.second, it.first)) }
            .toList()

        if (distinct.isEmpty()) return

        coroutineScope {
            distinct.groupBy { it.second }.map { (page, group) ->
                async(Dispatchers.IO) {
                    val words = group.map { it.first }.distinct()

                    words.chunked(ATLAS_PREFETCH_CHUNK).forEach { chunk ->
                        val rows = db.atlasWordShapeDao().getShapesForWords(key, chunk, page)

                        val byWord = rows.associateBy { it.word }

                        for (w in chunk) {
                            val list = byWord[w]?.let { entity ->
                                QuranAtlasLoader.decodePlacementsJson(entity.placementsJson)
                            } ?: emptyList()

                            shapeCache.putIfAbsent(cacheKey(page, w), list)
                        }
                    }
                }
            }.awaitAll()
        }
    }

    suspend fun getPlacementsForWords(
        words: List<AyahWordEntity>,
        readerPage: Int,
    ): Map<Int, List<AtlasGlyphPlacement>> {
        if (words.isEmpty()) return emptyMap()

        prefetchShapes(words.map { it.text to readerPage })

        val placements = getPrefetchedPlacementsForWords(words, readerPage)

        prefetchTexturesForPlacementLists(placements.values)

        return placements
    }

    fun getPrefetchedPlacementsForWords(
        words: List<AyahWordEntity>,
        readerPage: Int,
    ): Map<Int, List<AtlasGlyphPlacement>> {
        if (words.isEmpty()) return emptyMap()

        val page = resolveQueryPage(readerPage)

        return buildMap(words.size) {
            for (word in words) {
                put(
                    word.atlasPlacementMapKey(),
                    shapeCache[cacheKey(page, word.text)] ?: emptyList()
                )
            }
        }
    }

    suspend fun prefetchTexturesForPlacementLists(
        placementLists: Collection<List<AtlasGlyphPlacement>>,
    ) {
        val textureIndices = placementLists
            .asSequence()
            .flatten()
            .mapNotNull { placement -> layer.glyphs[placement.gid.toString()]?.textureIndex }
            .toList()

        textureStore.prefetch(textureIndices)
    }

    private suspend fun prefetchTexturesForPlacements(
        placements: List<AtlasGlyphPlacement>,
    ) {
        val textureIndices = placements.mapNotNull { placement ->
            layer.glyphs[placement.gid.toString()]?.textureIndex
        }

        textureStore.prefetch(textureIndices)
    }

    fun clearTextureCache() {
        textureStore.clear()
    }
}

fun Map<Int, List<AtlasGlyphPlacement>>.getForWord(word: AyahWordEntity): List<AtlasGlyphPlacement>? {
    return this[word.atlasPlacementMapKey()]
}

private fun AyahWordEntity.atlasPlacementMapKey(): Int =
    ayahId * 4096 + wordIndex
