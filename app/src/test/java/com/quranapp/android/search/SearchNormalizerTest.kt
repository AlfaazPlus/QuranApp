package com.quranapp.android.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchNormalizerTest {

    @Test
    fun latin_mercyVariantsCollapse() {
        val a = SearchNormalizer.normalize("Mercy", ScriptType.LATIN)
        val b = SearchNormalizer.normalize("mercy!", ScriptType.LATIN)
        val c = SearchNormalizer.normalize("MERCY", ScriptType.LATIN)
        assertEquals(a, b)
        assertEquals(b, c)
        assertEquals("mercy", a)
    }

    @Test
    fun scriptForLang_mapsKnownCodes() {
        assertEquals(ScriptType.LATIN, SearchNormalizer.scriptForLang("en"))
        assertEquals(ScriptType.ARABIC, SearchNormalizer.scriptForLang("ar"))
        assertEquals(ScriptType.OTHER, SearchNormalizer.scriptForLang("hi"))
    }

    @Test
    fun arabic_stripsTashkeel() {
        val s = SearchNormalizer.normalize("رَحْمَةٍ", ScriptType.ARABIC)
        assertTrue(s.isNotEmpty())
        assertTrue(!s.contains("\u064B"))
    }
}
