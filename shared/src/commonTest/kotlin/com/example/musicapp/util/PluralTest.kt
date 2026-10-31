package com.example.musicapp.util

import kotlin.test.Test
import kotlin.test.assertEquals

class PluralTest {

    @Test
    fun testPluralRu() {
        assertEquals("0 треков", pluralRu(0, "трек", "трека", "треков"))
        assertEquals("1 трек", pluralRu(1, "трек", "трека", "треков"))
        assertEquals("2 трека", pluralRu(2, "трек", "трека", "треков"))
        assertEquals("3 трека", pluralRu(3, "трек", "трека", "треков"))
        assertEquals("4 трека", pluralRu(4, "трек", "трека", "треков"))
        assertEquals("5 треков", pluralRu(5, "трек", "трека", "треков"))
        assertEquals("10 треков", pluralRu(10, "трек", "трека", "треков"))
        assertEquals("11 треков", pluralRu(11, "трек", "трека", "треков"))
        assertEquals("12 треков", pluralRu(12, "трек", "трека", "треков"))
        assertEquals("14 треков", pluralRu(14, "трек", "трека", "треков"))
        assertEquals("21 трек", pluralRu(21, "трек", "трека", "треков"))
        assertEquals("22 трека", pluralRu(22, "трек", "трека", "треков"))
        assertEquals("25 треков", pluralRu(25, "трек", "трека", "треков"))
        assertEquals("111 треков", pluralRu(111, "трек", "трека", "треков"))
        assertEquals("121 трек", pluralRu(121, "трек", "трека", "треков"))
    }
}
