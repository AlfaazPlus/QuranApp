package com.quranapp.android.utils.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import com.quranapp.android.api.ApiConfig
import com.quranapp.android.api.RetrofitInstance
import com.quranapp.android.api.fetchInventoryStreamingResponse
import com.quranapp.android.api.resolveInventoryUrl
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.app.AppUtils
import com.quranapp.android.utils.receivers.NetworkStateReceiver
import com.quranapp.android.utils.univ.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.GZIPInputStream
import kotlin.math.max

object VerseTemplateManager {
    private const val DIR_NAME = "templates"
    private const val IMAGES_SUBDIR = "images"
    private const val MANIFEST_FILENAME = "available_templates.json"

    private const val PREVIEW_CACHE_MAX_KB = 4 * 1024

    private val ROOT_DIR_PATH: String = FileUtils.createPath(
        AppUtils.BASE_APP_DOWNLOADED_SAVED_DATA_DIR,
        DIR_NAME,
    )

    @Volatile
    private var cachedCatalog: VerseTemplateCatalog? = null

    private val manifestLock = Mutex()
    private val imageLocks = ConcurrentHashMap<String, Mutex>()

    private val previewBitmapCache = object : LruCache<String, Bitmap>(PREVIEW_CACHE_MAX_KB) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return (value.allocationByteCount / 1024).coerceAtLeast(1)
        }
    }

    private fun getRootDir(context: Context): File {
        val dir = File(context.applicationContext.filesDir, ROOT_DIR_PATH)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun getImagesDir(context: Context): File {
        val dir = File(getRootDir(context), IMAGES_SUBDIR)

        if (!dir.exists()) {
            dir.mkdirs()
        }

        return dir
    }

    private fun getManifestFile(context: Context): File {
        return File(getRootDir(context), MANIFEST_FILENAME)
    }

    fun getCachedImageFile(context: Context, templateId: String): File {
        val safeName = templateId.replace(Regex("[^a-zA-Z0-9_.-]"), "_")
        return File(getImagesDir(context), "$safeName.webp")
    }

    fun resolveTemplateImageModel(context: Context, template: VerseTemplate): Any {
        val local = getCachedImageFile(context, template.id)

        if (local.isFile && local.length() > 0L) {
            return local
        }

        return resolveInventoryUrl(template.bgSrc)
    }

    /**
     * Picker/list thumb: prefer inline catalog WebP (`thumbBase64`), else full bg.
     * Never use this for export/render backgrounds.
     */
    fun resolveTemplateThumbModel(context: Context, template: VerseTemplate): Any {
        template.thumbBytes?.let { return it }
        return resolveTemplateImageModel(context, template)
    }

    suspend fun getAvailable(
        context: Context,
        forceRefresh: Boolean = false,
    ): VerseTemplateCatalog? {
        val appContext = context.applicationContext

        val inMemory = cachedCatalog
        if (!forceRefresh && inMemory != null) {
            return inMemory
        }

        return manifestLock.withLock {
            val recheck = cachedCatalog
            if (!forceRefresh && recheck != null) {
                return@withLock recheck
            }

            if (!forceRefresh) {
                loadLocal(appContext)?.let { local ->
                    cachedCatalog = local
                    return@withLock local
                }
            }

            val network = loadNetwork(appContext)
            if (network != null) {
                cachedCatalog = network
                return@withLock network
            }

            // Fallback to local if a forced refresh failed (e.g. transient network issue)
            cachedCatalog ?: loadLocal(appContext)?.also { cachedCatalog = it }
        }
    }

    suspend fun ensureBackgroundFile(
        context: Context,
        template: VerseTemplate,
    ): File? = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext

        val target = getCachedImageFile(appContext, template.id)
        if (target.isFile && target.length() > 0L) {
            return@withContext target
        }

        val lock = imageLocks.getOrPut(template.id) { Mutex() }
        lock.withLock {
            if (target.isFile && target.length() > 0L) {
                return@withLock target
            }

            if (!NetworkStateReceiver.isNetworkConnected(appContext)) {
                return@withLock null
            }

            val tmpFile = File(
                target.parentFile,
                "${target.name}.${System.currentTimeMillis()}.tmp",
            )

            try {
                val response = fetchInventoryStreamingResponse(template.bgSrc)
                if (!response.isSuccessful) {
                    throw IOException("Template bg download failed: HTTP ${response.code()}")
                }

                val body = response.body() ?: throw IOException("Template bg body is null")

                body.byteStream().use { input ->
                    tmpFile.outputStream().buffered().use { output ->
                        input.copyTo(output)
                    }
                }

                if (tmpFile.length() <= 0L) {
                    tmpFile.delete()
                    return@withLock null
                }

                if (target.exists()) {
                    target.delete()
                }

                if (!tmpFile.renameTo(target)) {
                    tmpFile.copyTo(target, overwrite = true)
                    tmpFile.delete()
                }

                target
            } catch (e: Exception) {
                tmpFile.delete()
                Log.saveError(e, "VerseTemplateManager.ensureBackgroundFile")
                null
            }
        }
    }

    suspend fun loadPreviewBitmap(
        context: Context,
        template: VerseTemplate,
        reqWidth: Int,
        reqHeight: Int,
    ): Bitmap? = withContext(Dispatchers.IO) {
        val cacheKey = previewCacheKey(template.id, reqWidth, reqHeight)
        synchronized(previewBitmapCache) {
            previewBitmapCache.get(cacheKey)?.takeIf { !it.isRecycled }?.let {
                return@withContext it
            }
        }

        val file = ensureBackgroundFile(context, template) ?: return@withContext null
        val decoded = decodeSampledBitmap(
            file = file,
            reqWidth = reqWidth,
            reqHeight = reqHeight,
            config = Bitmap.Config.RGB_565,
        ) ?: return@withContext null

        synchronized(previewBitmapCache) {
            val existing = previewBitmapCache.get(cacheKey)?.takeIf { !it.isRecycled }

            if (existing != null) {
                if (existing !== decoded) decoded.recycle()
                return@withContext existing
            }

            previewBitmapCache.put(cacheKey, decoded)
        }

        decoded
    }

    suspend fun decodeExportBitmap(
        context: Context,
        template: VerseTemplate,
        reqWidth: Int,
        reqHeight: Int,
    ): Bitmap? = withContext(Dispatchers.IO) {
        val file = ensureBackgroundFile(context, template) ?: return@withContext null
        decodeSampledBitmap(
            file = file,
            reqWidth = reqWidth,
            reqHeight = reqHeight,
            config = Bitmap.Config.ARGB_8888,
        )
    }

    private fun previewCacheKey(id: String, reqWidth: Int, reqHeight: Int): String {
        return "$id@$reqWidth@$reqHeight"
    }

    fun clearPreviewCache() {
        synchronized(previewBitmapCache) {
            previewBitmapCache.evictAll()
        }

        VerseTemplateRenderer.clearLogoCache()
    }

    private fun decodeSampledBitmap(
        file: File,
        reqWidth: Int,
        reqHeight: Int,
        config: Bitmap.Config,
    ): Bitmap? {
        if (!file.isFile || file.length() <= 0L) return null

        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, bounds)

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val sampleSize = calculateInSampleSize(
            srcWidth = bounds.outWidth,
            srcHeight = bounds.outHeight,
            reqWidth = max(1, reqWidth),
            reqHeight = max(1, reqHeight),
        )

        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = config
            inScaled = false
        }

        return try {
            BitmapFactory.decodeFile(file.absolutePath, decodeOpts)
        } catch (_: OutOfMemoryError) {
            decodeOpts.inSampleSize = max(2, sampleSize * 2)
            try {
                BitmapFactory.decodeFile(file.absolutePath, decodeOpts)
            } catch (_: OutOfMemoryError) {
                null
            }
        }
    }

    private fun calculateInSampleSize(
        srcWidth: Int,
        srcHeight: Int,
        reqWidth: Int,
        reqHeight: Int,
    ): Int {
        var inSampleSize = 1

        if (srcHeight > reqHeight || srcWidth > reqWidth) {
            val halfHeight = srcHeight / 2
            val halfWidth = srcWidth / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    private suspend fun loadLocal(context: Context): VerseTemplateCatalog? =
        withContext(Dispatchers.IO) {
            val file = getManifestFile(context)
            if (!file.isFile || file.length() <= 0L) return@withContext null

            return@withContext try {
                VerseTemplateParser.parseCatalog(file.readText(StandardCharsets.UTF_8))
            } catch (e: Exception) {
                Log.saveError(e, "VerseTemplateManager.loadLocal")
                null
            }
        }

    private suspend fun loadNetwork(context: Context): VerseTemplateCatalog? =
        withContext(Dispatchers.IO) {
            if (!NetworkStateReceiver.isNetworkConnected(context)) {
                return@withContext null
            }

            try {
                val response = RetrofitInstance.github.getAvailableTemplates()
                if (!response.isSuccessful) {
                    throw IOException("Templates download failed: HTTP ${response.code()}")
                }

                val body = response.body() ?: throw IOException("Templates body is null")

                val jsonText = body.byteStream().use { rawStream ->
                    val buffered = BufferedInputStream(rawStream)
                    buffered.mark(2)
                    val b1 = buffered.read()
                    val b2 = buffered.read()
                    buffered.reset()

                    val effectiveStream = if (b1 == 0x1f && b2 == 0x8b) {
                        GZIPInputStream(buffered)
                    } else {
                        buffered
                    }

                    effectiveStream.bufferedReader(StandardCharsets.UTF_8).readText()
                }

                val catalog = VerseTemplateParser.parseCatalog(jsonText)
                    ?: throw IOException("Failed to parse templates catalog")

                val manifestFile = getManifestFile(context)
                manifestFile.parentFile?.mkdirs()

                val tmp = File(manifestFile.parentFile, "$MANIFEST_FILENAME.tmp")
                tmp.writeText(jsonText, StandardCharsets.UTF_8)

                if (!tmp.renameTo(manifestFile)) {
                    tmp.copyTo(manifestFile, overwrite = true)
                    tmp.delete()
                }

                pruneOrphanedImages(context, catalog.templates)

                catalog
            } catch (e: Exception) {
                Log.saveError(e, "VerseTemplateManager.loadNetwork")
                null
            }
        }

    private fun pruneOrphanedImages(context: Context, activeTemplates: List<VerseTemplate>) {
        val activeFiles = activeTemplates
            .mapTo(HashSet(activeTemplates.size)) { getCachedImageFile(context, it.id).name }

        val files = getImagesDir(context).listFiles() ?: return
        for (file in files) {
            if (file.isFile && !file.name.endsWith(".tmp") && file.name !in activeFiles) {
                file.delete()
            }
        }
    }
}
