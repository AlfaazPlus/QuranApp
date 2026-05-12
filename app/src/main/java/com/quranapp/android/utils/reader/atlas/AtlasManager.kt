package com.quranapp.android.utils.reader.atlas

import android.content.Context
import android.util.JsonReader
import android.util.JsonToken
import androidx.room.withTransaction
import com.quranapp.android.db.ExternalQuranDatabase
import com.quranapp.android.db.entities.atlas.AtlasBundleEntity
import com.quranapp.android.db.entities.atlas.AtlasWordShapeEntity
import com.quranapp.android.utils.app.AppUtils
import com.quranapp.android.utils.univ.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.ZipEntry
import java.util.zip.ZipFile


object AtlasManager {
    private const val DIR_NAME = "atlas"
    private const val INSERT_CHUNK = 1000

    private val ROOT_DIR_PATH: String = FileUtils.createPath(
        AppUtils.BASE_APP_DOWNLOADED_SAVED_DATA_DIR,
        DIR_NAME,
    )

    private val lock = Mutex()

    private fun getRootDir(context: Context): File {
        val dir = File(context.applicationContext.filesDir, ROOT_DIR_PATH)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getTempDownloadFile(context: Context, id: String): File {
        return File(getRootDir(context), "${id}.tmp")
    }

    /**
     * On-disk file for one atlas texture page (`atlas.json` `textures[]` entry).
     */
    fun atlasTextureFile(
        context: Context,
        bundleKey: String,
        textureIndex: Int,
    ): File {
        return File(getRootDir(context), "${bundleKey}_tex${textureIndex}.png")
    }

    fun deleteBundleAtlasImages(context: Context, bundleKey: String) {
        val dir = getRootDir(context)

        File(dir, "$bundleKey.png").delete()

        dir.listFiles()?.forEach { f ->
            if (f.isFile && f.name.startsWith("${bundleKey}_tex")) {
                f.delete()
            }
        }
    }

    fun getPrebuiltAtlasAssetPath(scriptKey: String): String {
        return "atlas/$scriptKey/6x.zip"
    }

    suspend fun importAtlasFromZip(
        context: Context,
        zipFile: File,
        bundleKey: String,
        db: ExternalQuranDatabase,
    ): AtlasBundleEntity? = withContext(Dispatchers.IO) {
        lock.withLock {
            importFromZipFile(context, zipFile, bundleKey, db)
        }
    }

    suspend fun importAtlasFromZip(
        context: Context,
        zipStream: InputStream,
        bundleKey: String,
        db: ExternalQuranDatabase,
    ): AtlasBundleEntity? = withContext(Dispatchers.IO) {
        lock.withLock {
            val tempZip =
                File.createTempFile("${bundleKey}_atlas_import", ".zip", getRootDir(context))
            try {
                tempZip.outputStream().use { out ->
                    zipStream.use { input -> input.copyTo(out) }
                }

                importFromZipFile(context, tempZip, bundleKey, db)
            } finally {
                tempZip.delete()
            }
        }
    }

    private suspend fun importFromZipFile(
        context: Context,
        zipFile: File,
        bundleKey: String,
        db: ExternalQuranDatabase,
    ): AtlasBundleEntity {
        ZipFile(zipFile).use { zip ->
            val entries = normalizedZipEntries(zip)
            val metaJson = readZipEntryText(zip, entries, "meta.json")
            val meta = atlasJson.decodeFromString<AtlasMetaRoot>(metaJson)
            val layoutEntry = entries[normalizeZipPath(meta.layout.file)]
                ?: error("atlas zip missing layout file: ${meta.layout.file}")

            val atlasJsonPath = meta.sizes.firstOrNull()?.atlas ?: ""
            val layerJson = readZipEntryText(zip, entries, atlasJsonPath)
            val layer = atlasJson.decodeFromString<AtlasLayerJson>(layerJson).also {
                if (it.textures.isEmpty()) {
                    error("atlas.json missing textures[]")
                }
            }

            deleteBundleAtlasImages(context, bundleKey)

            for (texture in layer.textures.sortedBy { it.index }) {
                val textureEntry = entries[normalizeZipPath(texture.image)]
                    ?: error("atlas zip missing texture file: ${texture.image}")

                zip.getInputStream(textureEntry).use { input ->
                    atlasTextureFile(context, bundleKey, texture.index).outputStream().use { out ->
                        input.copyTo(out)
                    }
                }
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

    private fun normalizedZipEntries(zip: ZipFile): Map<String, ZipEntry> {
        val out = LinkedHashMap<String, ZipEntry>()

        val en = zip.entries()

        while (en.hasMoreElements()) {
            val e = en.nextElement()
            if (e.isDirectory) continue

            out[normalizeZipPath(e.name)] = e
        }

        return out
    }

    private fun readZipEntryText(
        zip: ZipFile,
        entries: Map<String, ZipEntry>,
        path: String,
    ): String {
        val entry = entries[normalizeZipPath(path)] ?: error("atlas zip missing $path")

        return zip.getInputStream(entry).use { input ->
            InputStreamReader(input, Charsets.UTF_8).readText()
        }
    }

    private suspend fun processImport(
        zip: ZipFile,
        layoutEntry: ZipEntry,
        bundleKey: String,
        db: ExternalQuranDatabase,
        meta: AtlasMetaRoot,
        metaJson: String,
        layerJson: String,
    ): AtlasBundleEntity = withContext(Dispatchers.IO) {
        val dao = db.atlasWordShapeDao()

        db.withTransaction {
            dao.deleteShapesForBundle(bundleKey)

            zip.getInputStream(layoutEntry).use { input ->
                streamWordShapesFromLayout(
                    input = input,
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
        input: InputStream,
        bundleKey: String,
        requirePage: Boolean,
        insertRows: suspend (List<AtlasWordShapeEntity>) -> Unit,
    ) {
        val rows = ArrayList<AtlasWordShapeEntity>(INSERT_CHUNK)

        suspend fun flushRows() {
            if (rows.isEmpty()) return
            insertRows(ArrayList(rows))
            rows.clear()
        }

        JsonReader(InputStreamReader(input, Charsets.UTF_8)).use { reader ->
            reader.isLenient = true
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "documents" -> {
                        reader.beginObject()

                        while (reader.hasNext()) {
                            reader.nextName()
                            rows.add(readLayoutDocumentShape(reader, bundleKey, requirePage))

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
                "text" -> text = reader.nextString()
                "page" -> page = reader.nextNullableInt()
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

    private fun JsonReader.readGlyphPlacementsJson(): String {
        val out = StringBuilder()
        var firstGlyph = true

        out.append('[')
        beginArray()
        while (hasNext()) {
            if (firstGlyph) {
                firstGlyph = false
            } else {
                out.append(',')
            }

            readGlyphPlacementJson(out)
        }
        endArray()
        out.append(']')

        return out.toString()
    }

    private fun JsonReader.readGlyphPlacementJson(out: StringBuilder) {
        var firstField = true

        fun appendNumberField(name: String, value: String) {
            if (firstField) {
                firstField = false
            } else {
                out.append(',')
            }

            out.append('"').append(name).append("\":").append(value)
        }

        out.append('{')
        beginObject()
        while (hasNext()) {
            when (val name = nextName()) {
                "g", "xa", "ya", "xo", "yo" -> {
                    val value = nextNumberLiteralOrNull()
                    if (value != null) {
                        appendNumberField(name, value)
                    }
                }

                else -> skipValue()
            }
        }
        endObject()
        out.append('}')
    }

    private fun JsonReader.nextNullableInt(): Int? {
        return if (peek() == JsonToken.NULL) {
            nextNull()
            null
        } else {
            nextInt()
        }
    }

    private fun JsonReader.nextNumberLiteralOrNull(): String? {
        return when (peek()) {
            JsonToken.NUMBER,
            JsonToken.STRING -> nextString()

            JsonToken.NULL -> {
                nextNull()
                null
            }

            else -> {
                skipValue()
                null
            }
        }
    }

    private fun normalizeZipPath(path: String): String {
        return path.trimStart('/').replace('\\', '/')
    }
}
