package com.quranapp.android.repository

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.quranapp.android.db.DatabaseProvider
import com.quranapp.android.api.models.translation.TranslationBookInfoModel
import com.quranapp.android.components.quran.subcomponents.Footnote
import com.quranapp.android.components.quran.subcomponents.Translation
import com.quranapp.android.db.ExternalQuranDatabase
import com.quranapp.android.db.dao.TranslationDao
import com.quranapp.android.db.dao.TranslationSlice
import com.quranapp.android.db.dao.TranslationVerseText
import com.quranapp.android.db.entities.translation.TranslationBookEntity
import com.quranapp.android.db.entities.translation.TranslationFootnoteEntity
import com.quranapp.android.db.entities.translation.TranslationVerseEntity
import com.quranapp.android.utils.quran.QuranMeta
import com.quranapp.android.db.translation.TranslationLegacyCopy
import com.quranapp.android.utils.Log
import com.quranapp.android.utils.quran.QuranConstants
import com.quranapp.android.utils.reader.TranslUtils
import com.quranapp.android.utils.univ.FileUtils
import com.quranapp.android.utils.univ.StringUtils
import org.json.JSONObject
import java.io.File

class TranslationRepository(
    private val database: ExternalQuranDatabase,
) {
    companion object {
        private const val AYAH_QUERY_CHUNK = 400

        @Composable
        fun remember(context: Context): TranslationRepository {
            return remember(context) {
                DatabaseProvider.getTranslationRepository(context)
            }
        }
    }

    private val dao: TranslationDao = database.translationDao()

    suspend fun prepare(context: Context) {
        val appContext = context.applicationContext

        TranslationLegacyCopy(database).copyIfPresent(appContext)

        importLegacyFiles(appContext)

        syncPrebuilt(appContext)
    }

    suspend fun store(bookInfo: TranslationBookInfoModel, translData: String) {
        val parsed = try {
            parseTranslation(bookInfo.slug, translData)
        } catch (error: Exception) {
            Log.saveError(error, "TranslationRepository.store")
            return
        }

        dao.replaceSlug(bookInfo.toEntity(), parsed.verses, parsed.footnotes)
    }

    suspend fun delete(slug: String) {
        dao.deleteSlug(slug)
    }

    suspend fun isDownloaded(slug: String): Boolean {
        return dao.bookExists(slug)
    }

    suspend fun getDownloadedSlugs(): Set<String> {
        return dao.getSlugs().toSet()
    }

    suspend fun getBook(slug: String): TranslationBookInfoModel {
        return dao.getBook(slug)?.toModel() ?: TranslationBookInfoModel("")
    }

    suspend fun getBooks(slugs: Set<String>? = null): Map<String, TranslationBookInfoModel> {
        if (slugs?.isEmpty() == true) return emptyMap()

        val books = if (slugs == null) {
            dao.getAllBooks()
        } else {
            dao.getBooks(slugs.toList())
        }

        return books.associate { it.slug to it.toModel() }
    }

    suspend fun getDownloadedBooks(): Map<String, TranslationBookInfoModel> {
        return getBooks().filterKeys { !TranslUtils.isPrebuilt(it) }
    }

    suspend fun getTranslation(
        slug: String,
        chapterNo: Int,
        verseNo: Int,
    ): Translation? {
        return getTranslations(listOf(slug), chapterNo, verseNo).firstOrNull()
    }

    suspend fun getTranslations(
        slugs: Collection<String>,
        chapterNo: Int,
        verseNo: Int,
    ): List<Translation> {
        val ordered = orderedSlugs(slugs)

        if (ordered.isEmpty()) return emptyList()

        val slice = dao.getAyahSlice(ordered, QuranMeta.getAyahId(chapterNo, verseNo))

        return orderBySlug(ordered, join(slice))
    }

    suspend fun getTranslations(
        slugs: Collection<String>,
        chapterNo: Int,
        fromVerse: Int,
        toVerse: Int,
    ): List<List<Translation>> {
        val slots = List((toVerse - fromVerse + 1).coerceAtLeast(0)) { ArrayList<Translation>() }
        val ordered = orderedSlugs(slugs)

        if (ordered.isEmpty() || fromVerse > toVerse) return slots

        val slice = dao.getRangeSlice(
            ordered,
            QuranMeta.getAyahId(chapterNo, fromVerse),
            QuranMeta.getAyahId(chapterNo, toVerse),
        )

        for (translation in orderBySlug(ordered, join(slice))) {
            val index = translation.verseNo - fromVerse
            if (index !in slots.indices) continue
            slots[index].add(translation)
        }

        return slots
    }

    suspend fun getTranslations(
        slugs: Collection<String>,
        chapterNo: Int,
        verses: IntArray,
    ): List<List<Translation>> {
        val slots = List(verses.size) { ArrayList<Translation>() }
        val ordered = orderedSlugs(slugs)

        if (ordered.isEmpty() || verses.isEmpty()) return slots

        val orderedVerses = verses.sorted()
        val slice = dao.getAyahsSlice(
            ordered,
            orderedVerses.map { QuranMeta.getAyahId(chapterNo, it) },
        )

        val byVerse = orderBySlug(ordered, join(slice)).groupBy { it.verseNo }

        orderedVerses.forEachIndexed { index, verseNo ->
            if (index !in slots.indices) return@forEachIndexed
            slots[index].addAll(byVerse[verseNo].orEmpty())
        }

        return slots
    }

    suspend fun getTranslationsByAyahId(
        slugs: Collection<String>,
        ayahIds: Collection<Int>,
    ): Map<Int, List<Translation>> {
        val ordered = orderedSlugs(slugs)

        if (ordered.isEmpty() || ayahIds.isEmpty()) return emptyMap()

        val grouped = LinkedHashMap<Int, MutableList<Translation>>()

        ayahIds.distinct().chunked(AYAH_QUERY_CHUNK).forEach { chunk ->
            val slice = dao.getAyahsSlice(ordered, chunk)
            for (translation in orderBySlug(ordered, join(slice))) {
                val ayahId = QuranMeta.getAyahId(translation.chapterNo, translation.verseNo)
                grouped.getOrPut(ayahId) { ArrayList() }.add(translation)
            }
        }

        return grouped
    }

    suspend fun getFootnotes(
        slug: String,
        chapterNo: Int,
        verseNo: Int,
    ): Map<Int, Footnote> {
        val ayahId = QuranMeta.getAyahId(chapterNo, verseNo)

        return dao.getFootnotes(slug, ayahId).toFootnoteMap()
    }

    suspend fun countVerses(slug: String): Int {
        return dao.countVerses(slug)
    }

    suspend fun getVerseTexts(slug: String): List<TranslationVerseText> {
        return dao.getVerseTexts(slug)
    }

    private suspend fun syncPrebuilt(context: Context) {
        for (bookInfo in TranslUtils.preBuiltTranslBooksInfo(context)) {
            val required = if (bookInfo.version > 0) bookInfo.version else 1L
            val current = dao.getBook(bookInfo.slug)?.version

            if (current != null && current >= required) continue

            val path = TranslUtils.getPrebuiltTranslPath(bookInfo.slug) ?: continue
            val text = StringUtils.readInputStream(context.assets.open(path))

            store(bookInfo, text)
        }
    }

    private suspend fun importLegacyFiles(context: Context) {
        val fileUtils = FileUtils.newInstance(context)
        val translDir = File(fileUtils.appFilesDirectory, TranslUtils.DIR_NAME)

        if (!translDir.exists()) return

        val pairs = TranslUtils.getTranslInfosAndFilesForMigration(fileUtils, translDir) ?: return

        for (pair in pairs) {
            store(pair.first, pair.second.readText())
        }

        translDir.deleteRecursively()
    }

    private fun join(slice: TranslationSlice): List<Translation> {
        val notes = slice.footnotes.groupBy { it.slug to it.ayahId }

        return slice.verses.map { verse ->
            verse.toTranslation(notes[verse.slug to verse.ayahId].orEmpty())
        }
    }

    private fun orderedSlugs(slugs: Collection<String>): List<String> {
        if (slugs.size <= 1) return slugs.toList()

        val transliterations = ArrayList<String>()
        val others = ArrayList<String>()

        for (slug in slugs) {
            if (TranslUtils.isTransliteration(slug)) {
                transliterations.add(slug)
            } else {
                others.add(slug)
            }
        }

        return transliterations + others
    }

    private fun orderBySlug(
        slugs: List<String>,
        translations: List<Translation>
    ): List<Translation> {
        if (slugs.size <= 1) return translations
        val order = slugs.withIndex().associate { it.value to it.index }

        return translations.sortedWith(
            compareBy(
                { it.chapterNo },
                { it.verseNo },
                { order[it.bookSlug] ?: Int.MAX_VALUE })
        )
    }

    private fun parseTranslation(slug: String, translData: String): ParsedTranslation {
        val root = JSONObject(translData)
        val chapters = root.optJSONArray(QuranConstants.KEY_CHAPTER_LIST)
            ?: return ParsedTranslation(emptyList(), emptyList())

        val verses = ArrayList<TranslationVerseEntity>()
        val footnotes = ArrayList<TranslationFootnoteEntity>()

        for (chapterIndex in 0 until chapters.length()) {
            val chapter = chapters.optJSONObject(chapterIndex) ?: continue
            val chapterNo = chapter.optInt(QuranConstants.KEY_NUMBER, -1)
            val verseList = chapter.optJSONArray(QuranConstants.KEY_VERSE_LIST) ?: continue

            for (verseIndex in 0 until verseList.length()) {
                val verse = verseList.optJSONObject(verseIndex) ?: continue
                val verseNo = verse.optInt(QuranConstants.KEY_NUMBER, -1)
                if (chapterNo <= 0 || verseNo <= 0) continue

                val ayahId = QuranMeta.getAyahId(chapterNo, verseNo)
                verses.add(
                    TranslationVerseEntity(
                        slug = slug,
                        ayahId = ayahId,
                        text = verse.optString(QuranConstants.KEY_TRANSLATION_TEXT, ""),
                    )
                )

                val rawFootnotes = verse.optJSONArray(QuranConstants.KEY_FOOTNOTE_LIST)
                if (rawFootnotes != null) {
                    footnotes.addAll(
                        TranslationLegacyCopy.footnotesFromJson(
                            slug,
                            ayahId,
                            rawFootnotes.toString(),
                        )
                    )
                }
            }
        }

        return ParsedTranslation(verses, footnotes)
    }

    private data class ParsedTranslation(
        val verses: List<TranslationVerseEntity>,
        val footnotes: List<TranslationFootnoteEntity>,
    )
}

fun TranslationBookInfoModel.toEntity(): TranslationBookEntity {
    val storedVersion = TranslationLegacyCopy.normalizeVersion(
        if (version > 0) version else lastUpdated,
    )

    return TranslationBookEntity(
        slug = slug,
        langCode = langCode,
        langName = langName,
        bookName = bookName.orEmpty(),
        authorName = authorName.orEmpty(),
        displayName = displayName.orEmpty(),
        version = storedVersion,
    )
}

fun TranslationBookEntity.toModel(): TranslationBookInfoModel {
    return TranslationBookInfoModel(slug).apply {
        langCode = this@toModel.langCode
        langName = this@toModel.langName
        bookName = this@toModel.bookName
        authorName = this@toModel.authorName
        displayName = this@toModel.displayName
        version = this@toModel.version
        lastUpdated = this@toModel.version
    }
}

private fun TranslationVerseEntity.toTranslation(
    footnotes: List<TranslationFootnoteEntity>,
): Translation {
    val (chapterNo, verseNo) = QuranMeta.getVerseNoFromAyahId(ayahId)

    return Translation().apply {
        this.chapterNo = chapterNo
        this.verseNo = verseNo
        text = this@toTranslation.text
        bookSlug = slug
        isUrdu = TranslUtils.isUrdu(slug)
        this.footnotes = footnotes.toFootnoteMap()
    }
}

private fun List<TranslationFootnoteEntity>.toFootnoteMap(): HashMap<Int, Footnote> {
    val map = HashMap<Int, Footnote>(size)

    for (row in this) {
        val (chapterNo, verseNo) = QuranMeta.getVerseNoFromAyahId(row.ayahId)

        map[row.number] = Footnote().apply {
            this.chapterNo = chapterNo
            this.verseNo = verseNo
            number = row.number
            text = row.text
            bookSlug = row.slug
        }
    }

    return map
}
