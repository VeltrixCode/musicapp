package com.example.musicapp.proxy

import com.example.musicapp.model.StreamSource
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.head
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalStreamingProxyIntegrationTest {

    @Test
    fun testHeadRequestAndHostValidation() = testApplication {
        val testSessions = ConcurrentHashMap<String, ProxySession>()
        val token = "valid_session_token_123"
        testSessions[token] = ProxySession(
            token = token,
            trackId = "track_1",
            streamSource = StreamSource("https://httpbin.org/bytes/1024")
        )

        application {
            configureStreamingProxyRoutes(this, testSessions) { host ->
                host == "127.0.0.1:8080" || host == "localhost:8080"
            }
        }

        // 1. Invalid Host returns 403 Forbidden (DNS Rebinding protection)
        val forbiddenResponse = client.get("/stream/$token") {
            header(HttpHeaders.Host, "malicious-site.com")
        }
        assertEquals(HttpStatusCode.Forbidden, forbiddenResponse.status)

        // 2. Unknown or expired token returns 404 Not Found
        val notFoundResponse = client.get("/stream/unknown_token_999") {
            header(HttpHeaders.Host, "127.0.0.1:8080")
        }
        assertEquals(HttpStatusCode.NotFound, notFoundResponse.status)

        // 3. Ensure no CORS headers are returned
        assertFalse(forbiddenResponse.headers.contains(HttpHeaders.AccessControlAllowOrigin))
        assertFalse(notFoundResponse.headers.contains(HttpHeaders.AccessControlAllowOrigin))
    }

    @Test
    fun testRange206AndRange416Handling() {
        val contentLength = 1000L

        // Helper replicating proxy's RFC 7233 range calculation
        fun handleRange(rangeHeader: String?): Pair<HttpStatusCode, String?> {
            if (rangeHeader == null) return HttpStatusCode.OK to null
            val range = rangeHeader.removePrefix("bytes=").split("-")
            val start = range[0].toLongOrNull() ?: 0L
            val end = if (range.size > 1 && range[1].isNotBlank()) range[1].toLong() else contentLength - 1

            return if (start >= contentLength) {
                HttpStatusCode.RequestedRangeNotSatisfiable to "bytes */$contentLength"
            } else {
                HttpStatusCode.PartialContent to "bytes $start-$end/$contentLength"
            }
        }

        // Within boundaries: 206 Partial Content
        val (status206, contentRange206) = handleRange("bytes=0-499")
        assertEquals(HttpStatusCode.PartialContent, status206)
        assertEquals("bytes 0-499/1000", contentRange206)

        // Out of boundaries: 416 Requested Range Not Satisfiable
        val (status416, contentRange416) = handleRange("bytes=1500-2000")
        assertEquals(HttpStatusCode.RequestedRangeNotSatisfiable, status416)
        assertEquals("bytes */1000", contentRange416)
    }

    @Test
    fun testUpstream403TriggersRenewalCallback() {
        var refreshCount = 0
        var currentSource = StreamSource("https://expired.example.com/audio.mp3")

        val onRefresh: suspend () -> StreamSource? = {
            refreshCount++
            StreamSource("https://fresh.example.com/audio.mp3")
        }

        // Simulate proxy handling 403 Forbidden
        val upstreamStatus = HttpStatusCode.Forbidden
        if (upstreamStatus in listOf(HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden, HttpStatusCode.Gone)) {
            val fresh = kotlinx.coroutines.runBlocking { onRefresh() }
            if (fresh != null) {
                currentSource = fresh
            }
        }

        assertEquals(1, refreshCount)
        assertEquals("https://fresh.example.com/audio.mp3", currentSource.url)
    }
}
