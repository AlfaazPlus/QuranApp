package com.quranapp.android.utils.mediaplayer

import androidx.room3.withWriteTransaction
import com.apollographql.apollo.api.json.BufferedSourceJsonReader
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.db.entities.wbw.WbwAudioTimingEntity
import com.quranapp.android.utils.mediaplayer.WbwAudioRepository.AUDIO_ID
import com.quranapp.android.utils.miscellenous.isGzip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.GzipSource
import okio.Path
import okio.buffer

object WbwTimingImporter {
    private const val INSERT_CHUNK = 750

    suspend fun importTimingFromFile(file: Path) =
        withContext(Dispatchers.IO) {
            val db = DatabaseProvider.getExternalQuranDatabase()
            val dao = db.wbwDao()

            val fileSource = FileSystem.SYSTEM.source(file)
            val source = if (file.isGzip()) GzipSource(fileSource) else fileSource

            BufferedSourceJsonReader(source.buffer()).use { reader ->
                db.withWriteTransaction {
                    dao.deleteTimingByAudioId(AUDIO_ID)

                    val buffer = ArrayList<WbwAudioTimingEntity>(INSERT_CHUNK)

                    reader.beginObject()

                    while (reader.hasNext()) {
                        val key = reader.nextName()
                        reader.beginArray()

                        val startMs = reader.nextLong()
                        val endMs = reader.nextLong()

                        reader.endArray()

                        val triple = parseTimingKey(key) ?: continue

                        val (chapterNo, verseNo, wordIdx) = triple

                        if (endMs <= startMs || startMs < 0L) continue

                        val ayahId = chapterNo * 1000 + verseNo

                        buffer.add(
                            WbwAudioTimingEntity(
                                audioId = AUDIO_ID,
                                ayahId = ayahId,
                                wordIndex = wordIdx,
                                startMillis = startMs,
                                endMillis = endMs,
                            ),
                        )

                        if (buffer.size >= INSERT_CHUNK) {
                            dao.upsertTimings(ArrayList(buffer))
                            buffer.clear()
                        }
                    }

                    reader.endObject()

                    if (buffer.isNotEmpty()) {
                        dao.upsertTimings(buffer)
                    }
                }
            }
        }

    private fun parseTimingKey(key: String): Triple<Int, Int, Int>? {
        val parts = key.split('_')
        if (parts.size < 3) return null

        val chapter = parts[0].toIntOrNull() ?: return null
        val verse = parts[1].toIntOrNull() ?: return null
        val wordIndex = parts[2].toIntOrNull() ?: return null

        if (chapter <= 0 || verse <= 0) return null

        return Triple(chapter, verse, wordIndex)
    }
}
