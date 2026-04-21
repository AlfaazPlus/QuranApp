package com.quranapp.android.utils.mediaplayer

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import com.quranapp.android.api.RetrofitInstance
import com.quranapp.android.utils.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

object WbwAudioPlayer {

    private const val BASE = "https://audio.qurancdn.com/wbw/"
    private const val CACHE_SUBDIR = "wbw_audio"

    private val mutex = Mutex()
    private var player: ExoPlayer? = null

    private fun segment(n: Int): String = String.format("%03d", n)

    private fun buildUrl(chapterNo: Int, verseNo: Int, urlWordIndex: Int): String =
        "$BASE${segment(chapterNo)}_${segment(verseNo)}_${segment(urlWordIndex)}.mp3"

    private fun cacheFile(context: Context, chapterNo: Int, verseNo: Int, urlWordIndex: Int): File {
        val dir = File(context.cacheDir, CACHE_SUBDIR).apply { mkdirs() }
        return File(dir, "${segment(chapterNo)}_${segment(verseNo)}_${segment(urlWordIndex)}.mp3")
    }

    private suspend fun downloadIfMissing(file: File, url: String) {
        if (file.exists() && file.length() > 0L) return
        val dir = file.parentFile ?: return
        dir.mkdirs()
        val tmp = File(dir, "${file.name}.tmp")
        withContext(Dispatchers.IO) {
            val response = RetrofitInstance.any.downloadStreaming(url)
            if (!response.isSuccessful) {
                throw IOException("Wbw audio failed: HTTP ${response.code()}")
            }
            val body = response.body() ?: throw IOException("Wbw audio body is null")
            body.byteStream().use { input ->
                tmp.outputStream().buffered().use { output ->
                    input.copyTo(output)
                }
            }
        }
        if (!tmp.renameTo(file)) {
            tmp.delete()
            throw IOException("Wbw audio could not finalize cache file")
        }
    }

    private fun getOrCreatePlayer(context: Context): ExoPlayer {
        player?.let { return it }
        val app = context.applicationContext

        return ExoPlayer.Builder(app).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                true,
            )
            repeatMode = Player.REPEAT_MODE_OFF
            addListener(
                object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        Log.saveError(error, "WbwWordAudioPlayer")
                    }
                },
            )
        }.also { player = it }
    }

    suspend fun play(
        context: Context,
        chapterNo: Int,
        verseNo: Int,
        appWordIndex: Int,
    ) {
        val urlWordIndex = appWordIndex + 1
        val file = cacheFile(context.applicationContext, chapterNo, verseNo, urlWordIndex)
        val url = buildUrl(chapterNo, verseNo, urlWordIndex)

        mutex.withLock {
            try {
                downloadIfMissing(file, url)
            } catch (e: Exception) {
                Log.saveError(e, "WbwWordAudioPlayer.download")
                return
            }

            val p = getOrCreatePlayer(context)
            p.stop()
            p.clearMediaItems()
            p.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            p.prepare()
            p.playWhenReady = true
        }
    }
}
