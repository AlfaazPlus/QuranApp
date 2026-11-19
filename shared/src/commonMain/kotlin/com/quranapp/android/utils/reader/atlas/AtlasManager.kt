package com.quranapp.android.utils.reader.atlas

import androidx.room3.withWriteTransaction
import com.apollographql.apollo.api.json.BufferedSourceJsonReader
import com.apollographql.apollo.api.json.JsonReader
import com.apollographql.apollo.api.json.JsonReader.Token
import com.quranapp.android.db.ExternalQuranDatabase
import com.quranapp.android.db.entities.atlas.AtlasBundleEntity
import com.quranapp.android.db.entities.atlas.AtlasWordShapeEntity
import com.quranapp.android.platform.AppZipArchive
import com.quranapp.android.platform.PlatformServices
import com.quranapp.android.utils.app.AppFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.Source
import okio.buffer

object AtlasManager {
    private const val DIR_NAME = "atlas"
    private const val INSERT_CHUNK = 1_000

    private val lock = Mutex()
    private val fileSystem = FileSystem.SYSTEM

    private fun getRootDir(): Path = AppFiles.savedDir(DIR_NAME)

    fun getTempDownloadFile(id: String): Path = getRootDir() / "$id.tmp"

    /**
     * On-disk file for one atlas texture page (`atlas.json` `textures[]` entry).
     */
    fun atlasTextureFile(
        bundleKey: String,
        textureIndex: Int,
    ): Path {
        return getRootDir() / "${bundleKey}_tex${textureIndex}.png"
    }

    fun deleteBundleAtlasImages(bundleKey: String) {
        val dir = getRootDir()
        if (!fileSystem.exists(dir)) return

        fileSystem.delete(dir / "$bundleKey.png")

        // Only delete this bundle's texture pages; do not delete other bundles' files.
        val texturePrefix = "${bundleKey}_tex"
        for (file in fileSystem.list(dir)) {
            if (file.name.startsWith(texturePrefix)) {
                fileSystem.delete(file)
            }
        }
    }

    fun getPrebuiltAtlasAssetPath(scriptKey: String): String {
        return "atlas/$scriptKey/6x.zip"
    }

    suspend fun importAtlasFromZip(
        zipFile: Path,
        bundleKey: String,
        db: ExternalQuranDatabase,
    ): AtlasBundleEntity? = withContext(Dispatchers.IO) {
        lock.withLock {
            importFromZipFile(zipFile, bundleKey, db)
        }
    }

    suspend fun importAtlasFromZip(
        zipByteArray: ByteArray,
        bundleKey: String,
        db: ExternalQuranDatabase,
    ): AtlasBundleEntity? = withContext(Dispatchers.IO) {
        lock.withLock {
            val tmpFile = PlatformServices.files.cacheDirectory / "${bundleKey}_atlas_import.zip"

            try {
                FileSystem.SYSTEM.sink(tmpFile).buffer().use { sink ->
                    sink.write(zipByteArray)
                }

                importFromZipFile(tmpFile, bundleKey, db)
            } finally {
                AppFiles.delete(tmpFile)
            }
        }
    }


    private suspend fun importFromZipFile(
        zipFile: Path,
        bundleKey: String,
        db: ExternalQuranDatabase,
    ): AtlasBundleEntity {
        PlatformServices.files.zipReader.open(zipFile).use { zip ->
            val entries = zip.entries()
                .associateBy { normalizeZipPath(it) }

            val metaJson = readZipEntryText(
                zip = zip,
                entries = entries,
                path = "meta.json",
            )

            val meta = atlasJson.decodeFromString<AtlasMetaRoot>(metaJson)
            val layoutEntry = entries[normalizeZipPath(meta.layout.file)]
                ?: error("atlas zip missing layout file: ${meta.layout.file}")

            val atlasJsonPath = meta.sizes.firstOrNull()?.atlas ?: ""

            val layerJson = readZipEntryText(
                zip = zip,
                entries = entries,
                path = atlasJsonPath,
            )

            val layer = atlasJson.decodeFromString<AtlasLayerJson>(layerJson).also {
                if (it.textures.isEmpty()) {
                    error("atlas.json missing textures[]")
                }
            }

            deleteBundleAtlasImages(bundleKey)

            for (texture in layer.textures.sortedBy { it.index }) {
                val textureEntry = entries[normalizeZipPath(texture.image)]
                    ?: error("atlas zip missing texture file: ${texture.image}")

                zip.copyEntry(
                    path = textureEntry,
                    destination = atlasTextureFile(bundleKey, texture.index),
                )
            }

            return processImport(
                zip = zip,
                layoutEntry = layoutEntry,
                bundleKey = bundleKey,
                db = db,
                meta = meta,
                metaJson = metaJson,
                layerJson = layerJson,
            )
        }
    }

    private fun readZipEntryText(
        zip: AppZipArchive,
        entries: Map<String, String>,
        path: String,
    ): String {

        val entry = entries[normalizeZipPath(path)]
            ?: error("atlas zip missing $path")

        return zip.readEntry(entry).decodeToString()
    }

    private suspend fun processImport(
        zip: AppZipArchive,
        layoutEntry: String,
        bundleKey: String,
        db: ExternalQuranDatabase,
        meta: AtlasMetaRoot,
        metaJson: String,
        layerJson: String,
    ): AtlasBundleEntity = withContext(Dispatchers.IO) {
        val dao = db.atlasWordShapeDao()

        db.withWriteTransaction {
            dao.deleteShapesForBundle(bundleKey)

            zip.openEntry(layoutEntry).use { source ->
                streamWordShapesFromLayout(
                    input = source,
                    bundleKey = bundleKey,
                    requirePage = meta.isPageScopedGlyphAtlas(),
                ) { rows ->
                    dao.insertShapes(rows)
                }
            }

            val bundle = AtlasBundleEntity(
                bundleKey = bundleKey,
                metaJson = metaJson,
                layerJson = layerJson,
            )

            dao.upsertBundle(bundle)

            bundle
        }
    }


    private suspend fun streamWordShapesFromLayout(
        input: Source,
        bundleKey: String,
        requirePage: Boolean,
        insertRows: suspend (List<AtlasWordShapeEntity>) -> Unit,
    ) {
        val rows = ArrayList<AtlasWordShapeEntity>(INSERT_CHUNK)

        suspend fun flushRows() {
            if (rows.isEmpty()) return
            insertRows(rows.toList())
            rows.clear()
        }

        BufferedSourceJsonReader(input.buffer()).use { reader ->
            reader.beginObject()

            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "documents" -> {
                        reader.beginObject()

                        while (reader.hasNext()) {
                            reader.nextName() // Document ID is not needed.

                            rows += readLayoutDocumentShape(
                                reader = reader,
                                bundleKey = bundleKey,
                                requirePage = requirePage,
                            )

                            if (rows.size >= INSERT_CHUNK) {
                                flushRows()
                            }
                        }

                        reader.endObject()
                    }

                    else -> reader.skipValue()
                }
            }

            reader.endObject()
        }

        flushRows()
    }

    private fun readLayoutDocumentShape(
        reader: JsonReader,
        bundleKey: String,
        requirePage: Boolean,
    ): AtlasWordShapeEntity {
        var text: String? = null
        var page: Int? = null
        var placementsJson = "[]"

        reader.beginObject()

        while (reader.hasNext()) {
            when (reader.nextName()) {
                "text" -> text = reader.readStringOrNull()
                "page" -> page = reader.readIntOrNull()
                "glyphs" -> placementsJson = reader.readGlyphPlacementsJson()
                else -> reader.skipValue()
            }
        }

        reader.endObject()

        val word = text ?: error("atlas layout document missing text")

        val resolvedPage = if (requirePage) {
            page ?: error("page_glyph_atlas layout document missing page: $word")
        } else {
            AtlasWordShapeEntity.ATLAS_PAGE_NONE
        }

        return AtlasWordShapeEntity(
            bundleKey = bundleKey,
            word = word,
            page = resolvedPage,
            placementsJson = placementsJson,
        )
    }

    private fun JsonReader.readStringOrNull(): String? =
        when (peek()) {
            Token.STRING -> nextString()
            Token.NULL -> {
                nextNull()
                null
            }

            else -> {
                skipValue()
                null
            }
        }

    private fun JsonReader.readIntOrNull(): Int? =
        when (peek()) {
            Token.NULL -> {
                nextNull()
                null
            }

            Token.NUMBER, Token.LONG, Token.STRING -> {
                val value = nextLong()

                require(value in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) {
                    "Page number is outside the Int range: $value"
                }

                value.toInt()
            }

            else -> error("Expected an integer or null for page")
        }

    private fun JsonReader.readGlyphPlacementsJson(): String {
        val out = StringBuilder("[")
        var first = true

        beginArray()

        while (hasNext()) {
            if (!first) out.append(',')
            first = false

            out.append(readGlyphPlacementJson())
        }

        endArray()

        return out.append(']').toString()
    }

    private fun JsonReader.readGlyphPlacementJson(): String {
        val out = StringBuilder("{")
        var firstField = true

        beginObject()

        while (hasNext()) {
            val name = nextName()

            when (name) {
                "g", "xa", "ya", "xo", "yo" -> {
                    val value = readNumberLiteralOrNull() ?: continue
                    if (!firstField) out.append(',')
                    firstField = false

                    out.append('"').append(name).append("\":").append(value)
                }

                else -> skipValue()
            }
        }

        endObject()

        return out.append('}').toString()
    }

    private fun JsonReader.readNumberLiteralOrNull(): String? =
        when (peek()) {
            Token.NULL -> {
                nextNull()
                null
            }

            Token.NUMBER, Token.LONG -> nextString()

            Token.STRING -> nextString()?.takeIf(JSON_NUMBER::matches)

            else -> {
                skipValue()
                null
            }
        }

    private val JSON_NUMBER = Regex(
        """-?(?:0|[1-9]\d*)(?:\.\d+)?(?:[eE][+-]?\d+)?"""
    )

    private fun normalizeZipPath(path: String): String {
        return path.trimStart('/').replace('\\', '/')
    }
}
