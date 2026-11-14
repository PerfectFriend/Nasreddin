package com.example.util

import java.util.Comparator

/**
 * Natural sort order comparator that handles embedded numbers properly.
 * E.g., "page1", "page2", "page10" rather than "page1", "page10", "page2".
 */
class AlphanumericComparator : Comparator<String> {

    override fun compare(s1: String?, s2: String?): Int {
        if (s1 == null && s2 == null) return 0
        if (s1 == null) return -1
        if (s2 == null) return 1

        var thisMarker = 0
        var thatMarker = 0
        val s1Length = s1.length
        val s2Length = s2.length

        while (thisMarker < s1Length && thatMarker < s2Length) {
            val thisChunk = getChunk(s1, s1Length, thisMarker)
            thisMarker += thisChunk.length

            val thatChunk = getChunk(s2, s2Length, thatMarker)
            thatMarker += thatChunk.length

            var result = 0
            if (isDigit(thisChunk[0]) && isDigit(thatChunk[0])) {
                val thisChunkLength = thisChunk.length
                result = thisChunkLength - thatChunk.length
                if (result == 0) {
                    for (i in 0 until thisChunkLength) {
                        result = thisChunk[i] - thatChunk[i]
                        if (result != 0) return result
                    }
                } else {
                    // Compare numerical values without overflow
                    val thisNum = thisChunk.trimStart('0')
                    val thatNum = thatChunk.trimStart('0')
                    val numLengthDiff = thisNum.length - thatNum.length
                    if (numLengthDiff != 0) {
                        return numLengthDiff
                    }
                    return thisNum.compareTo(thatNum)
                }
            } else {
                result = thisChunk.compareTo(thatChunk, ignoreCase = true)
            }

            if (result != 0) return result
        }

        return s1Length - s2Length
    }

    private fun getChunk(s: String, slength: Int, marker: Int): String {
        val chunk = StringBuilder()
        var c = s[marker]
        chunk.append(c)
        var current = marker + 1
        val fromDigit = isDigit(c)
        while (current < slength) {
            c = s[current]
            if (fromDigit != isDigit(c)) break
            chunk.append(c)
            current++
        }
        return chunk.toString()
    }

    private fun isDigit(ch: Char): Boolean = ch in '0'..'9'
}
