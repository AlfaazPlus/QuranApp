package com.quranapp.android.utils.share

import coil3.Image
import com.quranapp.android.api.ApiClient
import com.quranapp.android.api.downloadTo
import com.quranapp.android.api.fetchInventoryStreamingResponse
import com.quranapp.android.api.resolveInventoryUrl
import com.quranapp.android.platform.Platform
import com.quranapp.android.utils.app.AppFiles
import com.quranapp.android.utils.app.Logger
import com.quranapp.platform.AppImageRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path

object VerseTemplateManager {
    private const val DIR_NAME = "templates"
    private const val IMAGES_SUBDIR = "images"
    private const val MANIFEST_FILENAME = "available_templates.json"

    @Volatile
    private var cachedCatalog: VerseTemplateCatalog? = null

    private val manifestLock = Mutex()

    private val imageLocks = mutableMapOf<String, Mutex>()
    private val imageLocksMutex = Mutex()

    private fun getRootDir(): Path = AppFiles.savedDir(DIR_NAME)

    private fun getImagesDir(): Path = getRootDir() / IMAGES_SUBDIR

    private fun getManifestFile(): Path = getRootDir() / MANIFEST_FILENAME

    fun getCachedImageFile(templateId: String): Path {
        val safeName = templateId.replace(Regex("[^a-zA-Z0-9_.-]"), "_")
        return getImagesDir() / "$safeName.webp"
    }

    fun resolveTemplateImageModel(template: VerseTemplate): Any {
        val local = getCachedImageFile(template.id)

        if (AppFiles.size(local) > 0L) {
            return local
        }

        return resolveInventoryUrl(template.bgSrc)
    }

    /**
     * Picker/list thumb: prefer inline catalog WebP (`thumbBase64`), else full bg.
     * Never use this for export/render backgrounds.
     */
    fun resolveTemplateThumbModel(template: VerseTemplate): Any {
        template.thumbBytes?.let { return it }
        return resolveTemplateImageModel(template)
    }

    suspend fun getAvailable(
        forceRefresh: Boolean = false,
    ): VerseTemplateCatalog? {
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
                loadLocal()?.let { local ->
                    cachedCatalog = local
                    return@withLock local
                }
            }

            val network = loadNetwork()
            if (network != null) {
                cachedCatalog = network
                return@withLock network
            }

            // Fallback to local if a forced refresh failed (e.g. transient network issue)
            cachedCatalog ?: loadLocal()?.also { cachedCatalog = it }
        }
    }

    suspend fun ensureBackgroundFile(
        template: VerseTemplate,
    ): Path? = withContext(Dispatchers.IO) {
        val target = getCachedImageFile(template.id)
        if (AppFiles.size(target) > 0L) {
            return@withContext target
        }

        val lock = imageLocksMutex.withLock {
            imageLocks.getOrPut(template.id) { Mutex() }
        }

        lock.withLock {
            if (AppFiles.size(target) > 0L) {
                return@withLock target
            }

            if (!Platform.networkMonitor.isConnected) {
                return@withLock null
            }

            val tmpFile =
                Platform.files.cacheDirectory / "${target.name}.${System.currentTimeMillis()}.tmp"

            try {
                val response = fetchInventoryStreamingResponse(template.bgSrc)

                response.downloadTo(
                    tmpFile,
                    onFailed = {
                        throw IllegalStateException("Template bg download failed: HTTP ${response.code}")
                    },
                )


                if (AppFiles.size(tmpFile) <= 0L) {
                    AppFiles.delete(tmpFile)
                    return@withLock null
                }

                if (AppFiles.exists(target)) {
                    AppFiles.delete(target)
                }

                AppFiles.move(tmpFile, target)

                target
            } catch (e: CancellationException) {
                AppFiles.delete(tmpFile)
                throw e
            } catch (e: Exception) {
                AppFiles.delete(tmpFile)
                Logger.saveError(e, "VerseTemplateManager.ensureBackgroundFile")
                null
            }
        }
    }

    /**
     * Decodes the template background at about [reqWidth] x [reqHeight].
     * Preview loads stay in a small memory cache. Export loads do not.
     */
    suspend fun decodeBackground(
        template: VerseTemplate,
        reqWidth: Int,
        reqHeight: Int,
        cache: Boolean,
    ): Image? {
        val file = ensureBackgroundFile(template) ?: return null

        if (AppFiles.size(file) <= 0L) return null

        return Platform.files.imageLoader.load(
            AppImageRequest(
                data = file,
                width = reqWidth,
                height = reqHeight,
                cache = cache,
            )
        )
    }

    fun clearPreviewCache() {
        Platform.files.imageLoader.clear()
        VerseTemplateRenderer.clearLogoCache()
    }


    private suspend fun loadLocal(): VerseTemplateCatalog? =
        withContext(Dispatchers.IO) {
            val file = getManifestFile()
            if (AppFiles.size(file) <= 0L) return@withContext null

            return@withContext try {
                VerseTemplateParser.parseCatalog(AppFiles.readText(file))
            } catch (e: Exception) {
                Logger.saveError(e, "VerseTemplateManager.loadLocal")
                null
            }
        }

    private suspend fun loadNetwork(): VerseTemplateCatalog? =
        withContext(Dispatchers.IO) {
            if (!Platform.networkMonitor.isConnected) {
                return@withContext null
            }

            try {
                val path = Platform.files.cacheDirectory / "available_templates.json.gz"
                val body = ApiClient.getAvailableTemplates()
                body.downloadTo(
                    path,
                    onFailed = {
                        throw IllegalStateException("Templates download failed: HTTP ${body.code}")
                    },
                )

                val jsonText = AppFiles.readTextGzipped(path)

                val catalog = VerseTemplateParser.parseCatalog(jsonText)
                    ?: throw IllegalStateException("Failed to parse templates catalog")

                val manifestFile = getManifestFile()
                val tmp = manifestFile.parent!! / "$MANIFEST_FILENAME.tmp"
                FileSystem.SYSTEM.createDirectories(manifestFile.parent!!)

                AppFiles.writeText(tmp, jsonText)

                AppFiles.move(tmp, manifestFile)

                pruneOrphanedImages(catalog.templates)

                catalog
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.saveError(e, "VerseTemplateManager.loadNetwork")
                null
            }
        }

    private fun pruneOrphanedImages(activeTemplates: List<VerseTemplate>) {
        val activeFiles = activeTemplates
            .mapTo(HashSet(activeTemplates.size)) {
                getCachedImageFile(it.id).name
            }

        val fs = FileSystem.SYSTEM
        val dir = getImagesDir()

        if (!fs.exists(dir)) return

        for (file in fs.list(dir)) {
            if (
                fs.metadata(file).isRegularFile &&
                !file.name.endsWith(".tmp") &&
                file.name !in activeFiles
            ) {
                fs.delete(file)
            }
        }
    }
}
