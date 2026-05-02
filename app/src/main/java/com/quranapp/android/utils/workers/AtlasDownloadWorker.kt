package com.quranapp.android.utils.workers

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.quranapp.android.R
import com.quranapp.android.activities.ActivitySettings
import com.quranapp.android.compose.navigation.SettingRoutes
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.db.ExternalQuranDatabase
import com.quranapp.android.db.entities.atlas.AtlasBundleEntity
import com.quranapp.android.db.entities.atlas.AtlasWordShapeEntity
import com.quranapp.android.utils.app.NotificationUtils
import com.quranapp.android.utils.app.NotificationUtils.createForegroundInfoFallback
import com.quranapp.android.utils.reader.atlas.AtlasGlyphPlacement
import com.quranapp.android.utils.reader.atlas.AtlasLayerJson
import com.quranapp.android.utils.reader.atlas.AtlasManager
import com.quranapp.android.utils.reader.atlas.AtlasMetaRoot
import com.quranapp.android.utils.reader.atlas.atlasJson
import com.quranapp.android.utils.reader.atlas.atlasPlacementListSerializer
import com.quranapp.android.utils.univ.Keys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.zip.ZipInputStream

class AtlasDownloadWorker(
    private val ctx: Context,
    params: WorkerParameters,
) : CoroutineWorker(ctx, params) {
    companion object {
        private const val INSERT_CHUNK = 500
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val scriptKey = inputData.getString("scriptKey") ?: return createForegroundInfoFallback(ctx)

        return createForegroundInfo(scriptKey, 0)
    }

    override suspend fun doWork(): Result {
        val scriptKey = inputData.getString("scriptKey")
        val densityLevel = inputData.getInt("densityLevel", -1)

        if (scriptKey == null || densityLevel == -1) {
            return Result.failure()
        }

        setForeground(createForegroundInfo(scriptKey, 0))

        return try {
            downloadAndStore(scriptKey, densityLevel)
            Result.success()
        } catch (e: Exception) {
            val msg = e.message ?: if (e is IOException) {
                "Atlas download failed"
            } else {
                "Atlas import failed"
            }
            Result.failure(workDataOf("error" to msg))
        }
    }

    private suspend fun downloadAndStore(
        scriptKey: String,
        densityLevel: Int,
    ) = withContext(Dispatchers.IO) {
        val tmpFile = AtlasManager.getTempDownloadFile(ctx, scriptKey)

        try {
            downloadGithubRawContentToFile(
                url = "ghraw://AlfaazPlus/QuranAppInventory/master/atlas/$scriptKey/${densityLevel}x.zip",
                dest = tmpFile,
            ) { progress ->
                if (!isStopped) {
                    setProgressAsync(workDataOf("progress" to (progress ?: 0)))
                    setForeground(createForegroundInfo(scriptKey, progress))
                }
            }

            val db = DatabaseProvider.getExternalQuranDatabase(ctx)
            importFromZipFile(tmpFile, scriptKey, db)
        } finally {
            if (tmpFile.exists()) {
                tmpFile.delete()
            }
        }
    }

    private fun createForegroundInfo(
        scriptKey: String,
        progress: Int?,
    ): ForegroundInfo {
        val channelId = NotificationUtils.CHANNEL_ID_DOWNLOADS
        val builder = NotificationCompat.Builder(ctx, channelId).apply {
            setAutoCancel(false)
            setOngoing(true)
            setShowWhen(false)
            setSmallIcon(R.drawable.dr_logo)
            setContentTitle(ctx.getString(R.string.textDownloading))
            setContentText(scriptKey)
            setCategory(NotificationCompat.CATEGORY_PROGRESS)
            setProgress(100, progress ?: 0, progress == null)
        }

        var flag = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flag = flag or PendingIntent.FLAG_IMMUTABLE
        }

        val activityIntent = Intent(ctx, ActivitySettings::class.java).apply {
            putExtra(Keys.NAV_DESTINATION, SettingRoutes.SCRIPT)
        }

        builder.setContentIntent(
            PendingIntent.getActivity(
                ctx,
                scriptKey.hashCode(),
                activityIntent,
                flag,
            ),
        )

        builder.addAction(
            R.drawable.dr_icon_close,
            ctx.getString(R.string.strLabelCancel),
            WorkManager.getInstance(applicationContext).createCancelPendingIntent(id),
        )

        return ForegroundInfo(
            scriptKey.hashCode(),
            builder.build(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    private suspend fun importFromZipFile(
        file: File,
        bundleKey: String,
        db: ExternalQuranDatabase
    ) {
        val files = readZipEntries(
            file, setOf(
                "meta.json",
                "atlas.json",
                "atlas.png",
                "words.json",
            )
        )

        val metaBytes = files["meta.json"] ?: error("atlas zip missing meta.json")
        val layerBytes = files["atlas.json"] ?: error("atlas zip missing atlas.json")
        val pngBytes = files["atlas.png"] ?: error("atlas zip missing atlas.png")
        val wordsBytes = files["words.json"] ?: error("atlas zip missing words.json")

        val metaJson = metaBytes.decodeToString()
        val layerJson = layerBytes.decodeToString()
        val wordsJson = wordsBytes.decodeToString()

        // verify
        atlasJson.decodeFromString<AtlasMetaRoot>(metaJson)
        atlasJson.decodeFromString<AtlasLayerJson>(layerJson)


        val words = atlasJson.decodeFromString<Map<String, List<AtlasGlyphPlacement>>>(
            wordsJson
        )

        val dao = db.atlasWordShapeDao()

        db.withTransaction {
            dao.deleteShapesForBundle(bundleKey)

            for (chunk in words.entries.chunked(INSERT_CHUNK)) {
                val rows = chunk.map { (word, placements) ->
                    AtlasWordShapeEntity(
                        bundleKey = bundleKey,
                        word = word,
                        placementsJson = atlasJson.encodeToString(
                            atlasPlacementListSerializer,
                            placements
                        ),
                    )
                }

                dao.insertShapes(rows)
            }

            dao.upsertBundle(
                AtlasBundleEntity(
                    bundleKey = bundleKey,
                    metaJson = metaJson,
                    layerJson = layerJson,
                    imagePng = pngBytes,
                ),
            )
        }
    }

    private fun readZipEntries(file: File, entryNames: Set<String>): Map<String, ByteArray> {
        FileInputStream(file).use { raw ->
            val wanted = entryNames.map { normalizeZipPath(it) }.toSet()

            val out = LinkedHashMap<String, ByteArray>()

            val skipBuf = ByteArray(8192)

            BufferedInputStream(raw).use { buffered ->
                ZipInputStream(buffered).use { zis ->
                    while (true) {
                        val entry = zis.nextEntry ?: break

                        try {
                            if (entry.isDirectory) continue

                            val name = normalizeZipPath(entry.name)

                            if (name in wanted) {
                                out[name] = zis.readBytes()
                            } else {
                                while (zis.read(skipBuf) != -1) {
                                }
                            }
                        } finally {
                            zis.closeEntry()
                        }
                    }
                }
            }

            return out
        }
    }

    private fun normalizeZipPath(path: String): String {
        return path.trimStart('/').replace('\\', '/')
    }
}
