package com.quranapp.android.utils.reader.tafsir

import com.quranapp.android.utils.reader.importing.ImportException
import com.quranapp.android.utils.reader.importing.ImportFailure
import com.quranapp.android.utils.reader.translation.translationSlug
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TafsirImportTest {
    @Test
    fun slugUsesLanguageAndBook() {
        assertEquals("en_custom-ibn-kathir", translationSlug("en", "Ibn Kathir"))
        assertNull(translationSlug("not-a-language", "Ibn Kathir"))
    }

    @Test
    fun invalidJsonIsRejected() {
        val error = failureOf { parseTafsirImport("{", "en_custom-test", ::acceptAll) }
        assertEquals(ImportFailure.INVALID, error.failure)
    }

    @Test
    fun verseOutsideRangeIsRejected() {
        val json = """{"tafsirs":[{"verse_key":"1:999","verses":["1:999"],"text":"Note"}]}"""
        val error = failureOf {
            parseTafsirImport(json, "en_custom-test") { _, verseNo -> verseNo <= 7 }
        }
        assertEquals(ImportFailure.INVALID, error.failure)
        assertTrue(error.message.orEmpty().contains("1:999"))
    }

    @Test
    fun overlappingRangeIsRejected() {
        val json = """
            {"tafsirs":[
                {"verse_key":"2:1","verses":["2:1","2:2"],"text":"First"},
                {"verse_key":"2:2","verses":["2:2","2:3"],"text":"Second"}
            ]}
        """.trimIndent()
        val error = failureOf { parseTafsirImport(json, "en_custom-test", ::acceptAll) }
        assertEquals(ImportFailure.INVALID, error.failure)
        assertTrue(error.message.orEmpty().contains("overlap"))
    }

    @Test
    fun validBlockUsesGeneratedKey() {
        val json = """
            {"version":"9","surahs":[2],"tafsirs":[{
                "key":"ignored",
                "verse_key":"2:1",
                "verses":["2:1","2:2"],
                "text":"<p>Note</p>"
            }]}
        """.trimIndent()
        val block = runBlocking {
            parseTafsirImport(json, "en_custom-ibn-kathir", ::acceptAll).single()
        }
        assertEquals("en_custom-ibn-kathir", block.key)
        assertEquals("2:1", block.verseKey)
        assertEquals(listOf("2:1", "2:2"), block.verses)
        assertEquals("<p>Note</p>", block.text)
    }

    private fun failureOf(block: suspend () -> Unit): ImportException = runBlocking {
        try {
            block()
        } catch (error: ImportException) {
            return@runBlocking error
        }
        throw AssertionError("Expected import failure")
    }

    private fun acceptAll(@Suppress("UNUSED_PARAMETER") chapterNo: Int, verseNo: Int) = verseNo >= 1
}
