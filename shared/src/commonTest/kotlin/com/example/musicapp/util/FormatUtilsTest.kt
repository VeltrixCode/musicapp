package com.example.musicapp.util

import kotlin.test.Test
import kotlin.test.assertEquals

class FormatUtilsTest {

    @Test
    fun testFormatTimeZeroAndSeconds() {
        assertEquals("0:00", formatTime(0L))
        assertEquals("0:05", formatTime(5_000L))
        assertEquals("0:45", formatTime(45_000L))
        assertEquals("0:59", formatTime(59_999L))
    }

    @Test
    fun testFormatTimeMinutes() {
        assertEquals("1:00", formatTime(60_000L))
        assertEquals("3:45", formatTime(225_000L))
        assertEquals("12:34", formatTime(754_000L))
        assertEquals("59:59", formatTime(3_599_000L))
    }

    @Test
    fun testFormatTimeHours() {
        assertEquals("1:00:00", formatTime(3_600_000L))
        assertEquals("1:05:30", formatTime(3_930_000L))
        assertEquals("2:15:08", formatTime(8_108_000L))
    }

    @Test
    fun testPluralRuCases() {
        assertEquals("0 треков", pluralRu(0, "трек", "трека", "треков"))
        assertEquals("1 трек", pluralRu(1, "трек", "трека", "треков"))
        assertEquals("2 трека", pluralRu(2, "трек", "трека", "треков"))
        assertEquals("5 треков", pluralRu(5, "трек", "трека", "треков"))
        assertEquals("11 треков", pluralRu(11, "трек", "трека", "треков"))
        assertEquals("21 трек", pluralRu(21, "трек", "трека", "треков"))
        assertEquals("24 трека", pluralRu(24, "трек", "трека", "треков"))
        assertEquals("100 треков", pluralRu(100, "трек", "трека", "треков"))
    }
}
