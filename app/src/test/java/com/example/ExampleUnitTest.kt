package com.example

import com.example.util.AlphanumericComparator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testAlphanumericComparatorNaturalOrder() {
        val comparator = AlphanumericComparator()
        val filenames = listOf(
            "page_10.png",
            "page_1.png",
            "page_2.png",
            "page_100.png",
            "page_21.png"
        )
        val sorted = filenames.sortedWith(comparator)
        val expected = listOf(
            "page_1.png",
            "page_2.png",
            "page_10.png",
            "page_21.png",
            "page_100.png"
        )
        assertEquals(expected, sorted)
    }

    @Test
    fun testAlphanumericComparatorWithLeadingZeros() {
        val comparator = AlphanumericComparator()
        val list = listOf("chapter_09", "chapter_01", "chapter_002", "chapter_10")
        val sorted = list.sortedWith(comparator)
        assertEquals(listOf("chapter_01", "chapter_002", "chapter_09", "chapter_10"), sorted)
    }
}
