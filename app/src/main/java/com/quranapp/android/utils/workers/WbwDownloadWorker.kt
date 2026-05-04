package com.quranapp.android.utils.workers

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.quranapp.android.R
import com.quranapp.android.activities.ActivitySettings
import com.quranapp.android.api.JsonHelper
import com.quranapp.android.api.RetrofitInstance
import com.quranapp.android.api.models.wbw.WbwLanguageInfo
import com.quranapp.android.api.models.wbw.WbwPayloadModel
import com.quranapp.android.compose.navigation.SettingRoutes
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.db.entities.wbw.WbwWordEntity
import com.quranapp.android.utils.app.NotificationUtils
import com.quranapp.android.utils.app.NotificationUtils.createForegroundInfoFallback
import com.quranapp.android.utils.extensions.isGzip
import com.quranapp.android.utils.reader.wbw.WbwManager
import com.quranapp.android.utils.univ.Keys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.GZIPInputStream

class WbwDownloadWorker(
    private val ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {


    override suspend fun getForegroundInfo(): ForegroundInfo {
        val wbwInfoJson = inputData.getString("wbwInfo") ?: return createForegroundInfoFallback(ctx)
        val info = Json.decodeFromString<WbwLanguageInfo>(wbwInfoJson)

        return createForegroundInfo(info, 0)
    }

    override suspend fun doWork(): Result {
        val wbwInfoJson = inputData.getString("wbwInfo") ?: return Result.failure()
        val info = Json.decodeFromString<WbwLanguageInfo>(wbwInfoJson)

        setForeground(createForegroundInfo(info, 0))

        return try {
            downloadAndStore(info)
            Result.success()
        } catch (e: Exception) {
            Result.failure(workDataOf("error" to (e.message ?: "WBW download failed")))
        }
    }

    private suspend fun downloadAndStore(
        info: WbwLanguageInfo
    ) = withContext(Dispatchers.IO) {
        val tmpFile = WbwManager.getTempDownloadFile(ctx, info.id)

        try {
            downloadGithubRawContentToFile(
                url = info.url,
                dest = tmpFile
            ) { progress ->
                if (!isStopped) {
                    setProgressAsync(workDataOf("progress" to (progress ?: 0)))
                    setForeground(createForegroundInfo(info, progress))
                }
            }

            val payload = decodePayload(tmpFile)
            val entities = toEntities(payload, info.id)

            val db = DatabaseProvider.getExternalQuranDatabase(ctx)
            db.wbwDao().replaceByWbwId(info.id, entities)
            ReaderPreferences.bumpWbwContentEpoch()

            WbwManager.markResourceVersion(
                context = ctx,
                id = info.id,
                version = payload.version
            )
        } finally {
            if (tmpFile.exists()) {
                tmpFile.delete()
            }
        }
    }

    private suspend fun decodePayload(source: File): WbwPayloadModel = withContext(Dispatchers.IO) {
        if (!source.exists()) throw IOException("Source file does not exist")

        val inputFactory: () -> InputStream = if (source.isGzip()) {
            { GZIPInputStream(source.inputStream().buffered()) }
        } else {
            { source.inputStream().buffered() }
        }

        inputFactory().use { stream ->
            val content = stream.reader().use { it.readText() }
            JsonHelper.json.decodeFromString(content)
        }
    }

    private fun createForegroundInfo(
        info: WbwLanguageInfo,
        progress: Int?
    ): ForegroundInfo {
        val channelId = NotificationUtils.CHANNEL_ID_DOWNLOADS
        val builder = NotificationCompat.Builder(ctx, channelId).apply {
            setAutoCancel(false)
            setOngoing(true)
            setShowWhen(false)
            setSmallIcon(R.drawable.dr_logo)
            setContentTitle(ctx.getString(R.string.textDownloading))
            setContentText(info.langName)
            setCategory(NotificationCompat.CATEGORY_PROGRESS)
            setProgress(100, progress ?: 0, progress == null)
        }

        var flag = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flag = flag or PendingIntent.FLAG_IMMUTABLE
        }

        val activityIntent = Intent(ctx, ActivitySettings::class.java).apply {
            putExtra(Keys.NAV_DESTINATION, SettingRoutes.WWB)
        }

        builder.setContentIntent(
            PendingIntent.getActivity(
                ctx,
                info.id.hashCode(),
                activityIntent,
                flag
            )
        )

        builder.addAction(
            R.drawable.dr_icon_close,
            ctx.getString(R.string.strLabelCancel),
            WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)
        )

        return ForegroundInfo(
            info.id.hashCode(),
            builder.build(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    private fun toEntities(payload: WbwPayloadModel, wbwId: String): List<WbwWordEntity> {
        if (payload.verses.isEmpty()) return emptyList()

        val out = ArrayList<WbwWordEntity>()
        for ((ayahId, words) in payload.verses) {
            if (ayahId <= 0) continue

            for ((wordIndex, pair) in words) {
                val translation = pair.getOrNull(0)?.takeIf { it.isNotBlank() }
                val transliteration = pair.getOrNull(1)?.takeIf { it.isNotBlank() }

                out.add(
                    WbwWordEntity(
                        ayahId = ayahId,
                        wordIndex = wordIndex,
                        wbwId = wbwId,
                        translation = translation,
                        transliteration = transliteration,
                    )
                )
            }
        }

        return out
    }
}


suspend fun downloadGithubRawContentToFile(
    url: String,
    dest: File,
    setProgress: suspend (Int?) -> Unit,
) = withContext(Dispatchers.IO) {
    val response = if (url.startsWith("ghraw://")) {
        RetrofitInstance.githubLike.getRawContent(
            url.removePrefix("ghraw://").trimStart('/')
        )
    } else {
        RetrofitInstance.any.downloadStreaming(url)
    }

    if (!response.isSuccessful) {
        throw IOException("WBW download failed: HTTP ${response.code()}")
    }

    val body = response.body() ?: throw IOException("WBW response body is null")
    val totalBytes = body.contentLength()
    var downloaded = 0L

    body.byteStream().use { input ->
        dest.outputStream().buffered().use { output ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var lastUpdateTime = 0L

            while (true) {
                ensureActive()

                val bytes = input.read(buffer)
                if (bytes <= 0) break

                output.write(buffer, 0, bytes)
                downloaded += bytes

                val now = System.currentTimeMillis()
                val isFinished = totalBytes > 0L && downloaded == totalBytes

                if (now - lastUpdateTime >= 2000L || isFinished) {
                    lastUpdateTime = now

                    val progress = if (totalBytes > 0) {
                        ((downloaded * 100) / totalBytes).toInt()
                    } else {
                        null
                    }

                    setProgress(progress)
                }
            }

            output.flush()
        }
    }
}
