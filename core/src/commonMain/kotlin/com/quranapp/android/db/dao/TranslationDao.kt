package com.quranapp.android.db.dao

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.quranapp.android.db.entities.translation.TranslationBookEntity
import com.quranapp.android.db.entities.translation.TranslationFootnoteEntity
import com.quranapp.android.db.entities.translation.TranslationVerseEntity

data class TranslationSlice(
    val verses: List<TranslationVerseEntity>,
    val footnotes: List<TranslationFootnoteEntity>,
)

data class TranslationVerseText(
    @ColumnInfo(name = "ayah_id")
    val ayahId: Int,

    @ColumnInfo(name = "text")
    val text: String,
)

@Dao
interface TranslationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBook(book: TranslationBookEntity)

    @Query("SELECT * FROM translation_books WHERE slug = :slug LIMIT 1")
    suspend fun getBook(slug: String): TranslationBookEntity?

    @Query("SELECT * FROM translation_books WHERE slug IN (:slugs) ORDER BY slug ASC")
    suspend fun getBooks(slugs: List<String>): List<TranslationBookEntity>

    @Query("SELECT * FROM translation_books ORDER BY slug ASC")
    suspend fun getAllBooks(): List<TranslationBookEntity>

    @Query("DELETE FROM translation_books WHERE slug = :slug")
    suspend fun deleteBook(slug: String)

    @Query("SELECT EXISTS(SELECT 1 FROM translation_books WHERE slug = :slug)")
    suspend fun bookExists(slug: String): Boolean

    @Query("SELECT slug FROM translation_books")
    suspend fun getSlugs(): List<String>

    @Query("DELETE FROM translation_verses WHERE slug = :slug")
    suspend fun deleteVerses(slug: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerses(verses: List<TranslationVerseEntity>)

    @Query(
        """
        SELECT * FROM translation_verses
        WHERE slug IN (:slugs) AND ayah_id = :ayahId
        """
    )
    suspend fun getVersesForAyah(slugs: List<String>, ayahId: Int): List<TranslationVerseEntity>

    @Query(
        """
        SELECT * FROM translation_verses
        WHERE slug IN (:slugs) AND ayah_id BETWEEN :fromAyahId AND :toAyahId
        ORDER BY ayah_id ASC
        """
    )
    suspend fun getVersesInRange(
        slugs: List<String>,
        fromAyahId: Int,
        toAyahId: Int,
    ): List<TranslationVerseEntity>

    @Query(
        """
        SELECT * FROM translation_verses
        WHERE slug IN (:slugs) AND ayah_id IN (:ayahIds)
        ORDER BY ayah_id ASC
        """
    )
    suspend fun getVersesForAyahs(
        slugs: List<String>,
        ayahIds: List<Int>,
    ): List<TranslationVerseEntity>

    @Query("SELECT COUNT(*) FROM translation_verses WHERE slug = :slug")
    suspend fun countVerses(slug: String): Int

    @Query(
        """
        SELECT ayah_id, text FROM translation_verses
        WHERE slug = :slug
        ORDER BY ayah_id ASC
        """
    )
    suspend fun getVerseTexts(slug: String): List<TranslationVerseText>

    @Query("DELETE FROM translation_footnotes WHERE slug = :slug")
    suspend fun deleteFootnotes(slug: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFootnotes(footnotes: List<TranslationFootnoteEntity>)

    @Query(
        """
        SELECT * FROM translation_footnotes
        WHERE slug = :slug AND ayah_id = :ayahId
        ORDER BY number ASC
        """
    )
    suspend fun getFootnotes(slug: String, ayahId: Int): List<TranslationFootnoteEntity>

    @Query(
        """
        SELECT * FROM translation_footnotes
        WHERE slug IN (:slugs) AND ayah_id = :ayahId
        ORDER BY number ASC
        """
    )
    suspend fun getFootnotesForAyah(
        slugs: List<String>,
        ayahId: Int,
    ): List<TranslationFootnoteEntity>

    @Query(
        """
        SELECT * FROM translation_footnotes
        WHERE slug IN (:slugs) AND ayah_id BETWEEN :fromAyahId AND :toAyahId
        ORDER BY ayah_id ASC, number ASC
        """
    )
    suspend fun getFootnotesInRange(
        slugs: List<String>,
        fromAyahId: Int,
        toAyahId: Int,
    ): List<TranslationFootnoteEntity>

    @Query(
        """
        SELECT * FROM translation_footnotes
        WHERE slug IN (:slugs) AND ayah_id IN (:ayahIds)
        ORDER BY ayah_id ASC, number ASC
        """
    )
    suspend fun getFootnotesForAyahs(
        slugs: List<String>,
        ayahIds: List<Int>,
    ): List<TranslationFootnoteEntity>

    suspend fun getAyahSlice(slugs: List<String>, ayahId: Int): TranslationSlice {
        return TranslationSlice(
            verses = getVersesForAyah(slugs, ayahId),
            footnotes = getFootnotesForAyah(slugs, ayahId),
        )
    }

    suspend fun getRangeSlice(
        slugs: List<String>,
        fromAyahId: Int,
        toAyahId: Int,
    ): TranslationSlice {
        return TranslationSlice(
            verses = getVersesInRange(slugs, fromAyahId, toAyahId),
            footnotes = getFootnotesInRange(slugs, fromAyahId, toAyahId),
        )
    }

    suspend fun getAyahsSlice(slugs: List<String>, ayahIds: List<Int>): TranslationSlice {
        return TranslationSlice(
            verses = getVersesForAyahs(slugs, ayahIds),
            footnotes = getFootnotesForAyahs(slugs, ayahIds),
        )
    }

    @Transaction
    suspend fun replaceSlug(
        book: TranslationBookEntity,
        verses: List<TranslationVerseEntity>,
        footnotes: List<TranslationFootnoteEntity>,
    ) {
        deleteVerses(book.slug)
        deleteFootnotes(book.slug)
        upsertBook(book)

        verses.chunked(INSERT_CHUNK).forEach { chunk ->
            insertVerses(chunk)
        }
        footnotes.chunked(INSERT_CHUNK).forEach { chunk ->
            insertFootnotes(chunk)
        }
    }

    @Transaction
    suspend fun deleteSlug(slug: String) {
        deleteVerses(slug)
        deleteFootnotes(slug)
        deleteBook(slug)
    }

    private companion object {
        const val INSERT_CHUNK = 200
    }
}
