package com.quranapp.android.search

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FtsQueryBuilderTest {

    @Test
    fun prefixAndQuery_joinsWithAnd() {
        val q = FtsQueryBuilder.toPrefixAndQuery("mercy lord")
        assertNotNull(q)
        assertTrue(q!!.contains("mercy*"))
        assertTrue(q.contains("lord*"))
        assertTrue(q.contains(" AND "))
    }

    @Test
    fun blank_returnsNull() {
        assertNull(FtsQueryBuilder.toPrefixAndQuery("   "))
    }
}
