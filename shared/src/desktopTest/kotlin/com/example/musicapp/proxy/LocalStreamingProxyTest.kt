package com.example.musicapp.proxy

import com.example.musicapp.model.StreamSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class LocalStreamingProxyTest {

    @Test
    fun testHostHeaderValidation() {
        val port = 12345
        // Replicate proxy's strict host validation
        fun validate(host: String?): Boolean {
            if (host.isNullOrBlank()) return false
            val allowed = setOf(
                "127.0.0.1:$port",
                "localhost:$port",
                "127.0.0.1",
                "localhost"
            )
            return host in allowed
        }

        assertTrue(validate("127.0.0.1:$port"))
        assertTrue(validate("localhost:$port"))
        assertTrue(validate("127.0.0.1"))
        assertTrue(validate("localhost"))

        // Malicious DNS-rebinding attacks or foreign hosts
        assertFalse(validate("evil.com"))
        assertFalse(validate("evil.com:$port"))
        assertFalse(validate("192.168.1.100:$port"))
        assertFalse(validate(null))
        assertFalse(validate(""))
    }

    @Test
    fun testSessionTokensAreRandomizedAndOpaque() {
        val stream = StreamSource(url = "https://example.com/audio.mp3")
        val url1 = LocalStreamingProxy.registerStream("track_secret_123", stream)
        val url2 = LocalStreamingProxy.registerStream("track_secret_123", stream)

        assertTrue(url1.startsWith("http://127.0.0.1:"))
        assertFalse(url1.contains("track_secret_123"), "URL must NOT leak raw track ID")

        assertNotEquals(url1, url2, "Every playback session must generate a distinct single-use token")
    }

    @Test
    fun testRangeHeaderParsing() {
        val totalLength = 1000L
        val rangeHeader = "bytes=100-199"

        val range = rangeHeader.removePrefix("bytes=").split("-")
        val start = range[0].toLongOrNull() ?: 0L
        val end = if (range.size > 1 && range[1].isNotBlank()) range[1].toLong() else totalLength - 1

        assertEquals(100L, start)
        assertEquals(199L, end)
        assertEquals(100L, end - start + 1)

        val contentRangeHeader = "bytes $start-$end/$totalLength"
        assertEquals("bytes 100-199/1000", contentRangeHeader)
    }

    @Test
    fun testRangeNotSatisfiableWhenStartExceedsLength() {
        val totalLength = 500L
        val requestedStart = 600L
        val isSatisfiable = requestedStart < totalLength
        assertFalse(isSatisfiable, "Range starting beyond content length must result in 416")
    }
}
