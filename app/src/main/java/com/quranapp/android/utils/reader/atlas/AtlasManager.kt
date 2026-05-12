package com.quranapp.android.utils.reader.atlas

import android.content.Context
import androidx.room.withTransaction
import com.quranapp.android.db.ExternalQuranDatabase
import com.quranapp.android.db.entities.atlas.AtlasBundleEntity
import com.quranapp.android.db.entities.atlas.AtlasWordShapeEntity
import com.quranapp.android.utils.app.AppUtils
import com.quranapp.android.utils.univ.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

object AtlasManager {
    private const val DIR_NAME = "atlas"
    private const val INSERT_CHUNK = 1000

    private val ROOT_DIR_PATH: String = FileUtils.createPath(
        AppUtils.BASE_APP_DOWNLOADED_SAVED_DATA_DIR,
        DIR_NAME
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

    fun getBundlePngFile(context: Context, bundleKey: String): File {
        return File(getRootDir(context), "${sanitize(bundleKey)}.png")
    }

    private fun sanitize(bundleKey: String): String =
        buildString(bundleKey.length) {
            for (c in bundleKey) {
                append(
                    when {
                        c.isLetterOrDigit() || c == '-' || c == '_' -> c
                        else -> '_'
                    },
                )
            }
        }

    fun getPrebuiltAtlasAssetPath(scriptKey: String): String {
        return "atlas/$scriptKey/6x.zip"
    }

    suspend fun importAtlasFromZip(
        context: Context,
        zipFile: File,
        bundleKey: String,
        db: ExternalQuranDatabase
    ): AtlasBundleEntity? = withContext(Dispatchers.IO) {
        lock.withLock {
            var metaJson: String? = null
            var layerJson: String? = null
            var words: Map<String, JsonElement>? = null
            val pngFile = getBundlePngFile(context, bundleKey)

            ZipFile(zipFile).use { zip ->
                zip.getEntry("meta.json")?.let {
                    metaJson = zip.getInputStream(it).bufferedReader().use { r -> r.readText() }
                }
                zip.getEntry("atlas.json")?.let {
                    layerJson = zip.getInputStream(it).bufferedReader().use { r -> r.readText() }
                }
                zip.getEntry("words.json")?.let {
                    val text = zip.getInputStream(it).bufferedReader().use { r -> r.readText() }
                    words = atlasJson.decodeFromString<Map<String, JsonElement>>(text)
                }
                zip.getEntry("atlas.png")?.let { entry ->
                    zip.getInputStream(entry).use { input ->
                        pngFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }

            processImport(
                bundleKey,
                db,
                metaJson ?: error("atlas zip missing meta.json"),
                layerJson ?: error("atlas zip missing atlas.json"),
                words ?: error("atlas zip missing words.json")
            )
        }
    }

    suspend fun importAtlasFromZip(
        context: Context,
        zipStream: InputStream,
        bundleKey: String,
        db: ExternalQuranDatabase
    ): AtlasBundleEntity? = withContext(Dispatchers.IO) {
        lock.withLock {
            var metaJson: String? = null
            var layerJson: String? = null
            var words: Map<String, JsonElement>? = null
            val pngFile = getBundlePngFile(context, bundleKey)

            ZipInputStream(zipStream.buffered()).use { zis ->
                while (true) {
                    val entry = zis.nextEntry ?: break
                    val name = normalizeZipPath(entry.name)
                    when (name) {
                        "meta.json" -> metaJson = zis.readBytes().decodeToString()
                        "atlas.json" -> layerJson = zis.readBytes().decodeToString()
                        "words.json" -> words =
                            atlasJson.decodeFromString<Map<String, JsonElement>>(
                                zis.readBytes().decodeToString()
                            )

                        "atlas.png" -> {
                            pngFile.outputStream().use { fos ->
                                zis.copyTo(fos)
                            }
                        }
                    }
                    zis.closeEntry()
                }
            }

            processImport(
                bundleKey,
                db,
                metaJson ?: error("atlas zip missing meta.json"),
                layerJson ?: error("atlas zip missing atlas.json"),
                words ?: error("atlas zip missing words.json")
            )
        }
    }

    private suspend fun processImport(
        bundleKey: String,
        db: ExternalQuranDatabase,
        metaJson: String,
        layerJson: String,
        words: Map<String, JsonElement>
    ): AtlasBundleEntity = withContext(Dispatchers.IO) {
        // verify
        atlasJson.decodeFromString<AtlasMetaRoot>(metaJson)
        atlasJson.decodeFromString<AtlasLayerJson>(layerJson)

        val dao = db.atlasWordShapeDao()

        db.withTransaction {
            dao.deleteShapesForBundle(bundleKey)

            // Parallelize JSON string conversion of word placements
            val rowChunks = words.entries.chunked(INSERT_CHUNK).map { chunk ->
                async(Dispatchers.Default) {
                    chunk.map { (word, element) ->
                        AtlasWordShapeEntity(
                            bundleKey = bundleKey,
                            word = word,
                            placementsJson = element.toString(),
                        )
                    }
                }
            }.awaitAll()

            rowChunks.forEach { rows ->
                dao.insertShapes(rows)
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


    private fun normalizeZipPath(path: String): String {
        return path.trimStart('/').replace('\\', '/')
    }
}
