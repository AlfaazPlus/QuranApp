package com.quranapp.android.utils.mediaplayer

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.quranapp.android.utils.quran.QuranMeta
import java.io.File

@OptIn(UnstableApi::class)
object RecitationStreamCache {
    private const val CACHE_DIR_NAME = "exo_recitation_cache"
    private const val CACHE_MAX_BYTES = 512L * 1024 * 1024

    @Volatile
    private var simpleCache: SimpleCache? = null

    private val lock = Any()

    fun get(context: Context): SimpleCache {
        synchronized(lock) {
            simpleCache?.let { return it }

            val appCtx = context.applicationContext
            val dir = File(appCtx.cacheDir, CACHE_DIR_NAME)
            if (!dir.exists()) {
                dir.mkdirs()
            }

            val evictor = LeastRecentlyUsedCacheEvictor(CACHE_MAX_BYTES)
            val dbProvider = StandaloneDatabaseProvider(appCtx)
            return SimpleCache(dir, evictor, dbProvider).also { simpleCache = it }
        }
    }

    fun clearForReciter(context: Context, urlTemplate: String) {
        val cache = get(context)

        for (chapterNo in QuranMeta.chapterRange) {
            val url = RecitationAudioRepository.prepareAudioUrl(urlTemplate, chapterNo) ?: continue
            runCatching { cache.removeResource(url) }
        }
    }
}
