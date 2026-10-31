package com.example.musicapp.lyrics

import com.example.musicapp.model.LrcLine
import kotlin.test.Test
import kotlin.test.assertEquals

class LrcParserTest {

    @Test
    fun testParseStandardLrc() {
        val lrc = """
            [00:05.50]First line of lyrics
            [00:10.00]Second line of lyrics
            [00:18.250]Third line of lyrics with 3-digit ms
            Invalid non-timed line
            
            [01:02.10]One minute later
        """.trimIndent()

        val lines = LrcParser.parse(lrc)
        assertEquals(4, lines.size)

        assertEquals(5500L, lines[0].timestampMs)
        assertEquals("First line of lyrics", lines[0].text)

        assertEquals(10000L, lines[1].timestampMs)
        assertEquals("Second line of lyrics", lines[1].text)

        assertEquals(18250L, lines[2].timestampMs)
        assertEquals("Third line of lyrics with 3-digit ms", lines[2].text)

        assertEquals(62100L, lines[3].timestampMs)
        assertEquals("One minute later", lines[3].text)
    }

    @Test
    fun testParseOutOrderLrcSortsChronologically() {
        val lrc = """
            [00:20.00]Line 20
            [00:05.00]Line 5
            [00:12.00]Line 12
        """.trimIndent()

        val lines = LrcParser.parse(lrc)
        assertEquals(3, lines.size)
        assertEquals(5000L, lines[0].timestampMs)
        assertEquals(12000L, lines[1].timestampMs)
        assertEquals(20000L, lines[2].timestampMs)
    }

    @Test
    fun testFindActiveLineIndex() {
        val lines = listOf(
            LrcLine(1000L, "Line 1"),
            LrcLine(5000L, "Line 2"),
            LrcLine(12000L, "Line 3"),
            LrcLine(20000L, "Line 4")
        )

        // Before first line
        assertEquals(-1, LrcParser.findActiveLineIndex(lines, 500L))
        assertEquals(-1, LrcParser.findActiveLineIndex(lines, 0L))

        // Exactly on line 1
        assertEquals(0, LrcParser.findActiveLineIndex(lines, 1000L))

        // Between line 1 and line 2
        assertEquals(0, LrcParser.findActiveLineIndex(lines, 3000L))
        assertEquals(0, LrcParser.findActiveLineIndex(lines, 4999L))

        // Exactly on line 2
        assertEquals(1, LrcParser.findActiveLineIndex(lines, 5000L))

        // Between line 2 and line 3
        assertEquals(1, LrcParser.findActiveLineIndex(lines, 8000L))

        // Beyond last line
        assertEquals(3, LrcParser.findActiveLineIndex(lines, 25000L))
        assertEquals(3, LrcParser.findActiveLineIndex(lines, 100000L))

        // Empty list
        assertEquals(-1, LrcParser.findActiveLineIndex(emptyList(), 5000L))
    }
}
