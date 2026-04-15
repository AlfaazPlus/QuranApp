package com.quranapp.android.utils.verse

import android.content.Context
import com.quranapp.android.compose.utils.preferences.ReaderPreferences
import com.quranapp.android.compose.utils.preferences.VersePreferences
import com.quranapp.android.db.QuranRepository
import com.quranapp.android.db.relations.VerseWithDetails
import com.quranapp.android.utils.others.ShortcutUtils
import com.quranapp.android.utils.quran.QuranMeta
import com.quranapp.android.utils.reader.TranslUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

object VerseUtils {

    private const val VOTD_RESET_MILLIS = 24L * 60L * 60L * 1000L // 24 hours
    private const val MAX_VOTD_ATTEMPTS = 20

    @Volatile
    private var votdChapNo: Int = -1

    @Volatile
    private var votdVerseNo: Int = -1

    suspend fun getVOTD(
        ctx: Context,
        repository: QuranRepository,
    ): VerseWithDetails? = withContext(Dispatchers.IO) {

        // 1) ALWAYS try to reuse existing VOTD first
        val savedVerse = VersePreferences.getVotd()
        val savedTimestamp = VersePreferences.getVotdTimestamp()

        if (savedVerse != null && !isExpired(savedTimestamp)) {
            val existing = buildVerseWithDetails(
                repository = repository,
                chapterNo = savedVerse.chapterNo,
                verseNo = savedVerse.verseNo
            )

            if (existing != null) {
                votdChapNo = savedVerse.chapterNo
                votdVerseNo = savedVerse.verseNo
                return@withContext existing
            } else {
                // Stored verse is corrupted/invalid -> clear it so we can regenerate
                VersePreferences.removeVotd()
            }
        }

        // 2) Generate a new one ONLY if none exists or it expired
        repeat(MAX_VOTD_ATTEMPTS) {
            val chapterNo = Random.nextInt(1, QuranMeta.chapterRange.last + 1)
            val verseNo = Random.nextInt(1, repository.getChapterVerseCount(chapterNo) + 1)

            val vwd = buildVerseWithDetails(repository, chapterNo, verseNo)

            if (vwd != null && vwd.isIdealForVOTD()) {
                val createdAtMillis = System.currentTimeMillis()

                VersePreferences.saveVotd(
                    chapterNo = chapterNo,
                    verseNo = verseNo,
                    timestamp = createdAtMillis
                )

                ShortcutUtils.pushVOTDShortcut(ctx, chapterNo, verseNo)

                votdChapNo = chapterNo
                votdVerseNo = verseNo

                return@withContext vwd
            }
        }

        null
    }

    private suspend fun buildVerseWithDetails(
        repository: QuranRepository,
        chapterNo: Int,
        verseNo: Int
    ): VerseWithDetails? {
        if (!QuranMeta.isChapterValid(chapterNo)) return null
        if (!repository.isVerseValid4Chapter(chapterNo, verseNo)) return null

        return repository.getVerseWithDetails(chapterNo, verseNo)
    }

    private fun isExpired(timestamp: Long): Boolean {
        if (timestamp <= 0L) return true

        val now = System.currentTimeMillis()
        val age = now - timestamp

        return age < 0L || age >= VOTD_RESET_MILLIS
    }

    fun isVOTD(chapterNo: Int, verseNo: Int): Boolean {
        if (votdChapNo <= 0 || votdVerseNo <= 0) {
            val votd = VersePreferences.getVotd() ?: return false
            votdChapNo = votd.chapterNo
            votdVerseNo = votd.verseNo
        }

        return chapterNo == votdChapNo && verseNo == votdVerseNo
    }

    fun obtainOptimalSlugForVotd(): String {
        val savedTranslations = ReaderPreferences.getTranslations()

        return savedTranslations.firstOrNull { !TranslUtils.isTransliteration(it) }
            ?: TranslUtils.TRANSL_SLUG_DEFAULT
    }
}