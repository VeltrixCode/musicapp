package com.example.musicapp.proxy

import com.example.musicapp.cache.AudioOfflineCache
import com.example.musicapp.model.StreamSource
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RedirectSecurityTest {

    @Test
    fun testCrossHostRedirectStripsAuthorizationAndCookie() = runBlocking {
        val requestsReceived = mutableListOf<Map<String, String>>()

        val mockEngine = MockEngine { request ->
            val host = request.url.host
            val headersMap = mutableMapOf<String, String>()
            request.headers.forEach { name, values ->
                headersMap[name] = values.joinToString(";")
            }
            requestsReceived.add(headersMap)

            when (host) {
                "api.musicapp.com" -> {
                    // Returns 302 redirect to a foreign CDN host
                    respond(
                        content = "",
                        status = HttpStatusCode.Found,
                        headers = headersOf(HttpHeaders.Location, "https://cdn.storage-cdn.net/audio/track123.mp3")
                    )
                }
                "cdn.storage-cdn.net" -> {
                    respond(
                        content = "AUDIO_STREAM_DATA",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "audio/mpeg")
                    )
                }
                else -> error("Unexpected host: $host")
            }
        }

        val testClient = HttpClient(mockEngine) {
            followRedirects = false
        }

        val streamSource = StreamSource(
            url = "https://api.musicapp.com/stream/v1",
            headers = mapOf(
                "Authorization" to "Bearer secret_user_token",
                "Cookie" to "session_id=abcdef123456"
            )
        )

        val response = LocalStreamingProxy.fetchUpstream(streamSource, null, testClient)
        assertEquals(HttpStatusCode.OK, response.status)

        assertEquals(2, requestsReceived.size)
        // 1st request to api.musicapp.com must carry Authorization & Cookie
        assertEquals("Bearer secret_user_token", requestsReceived[0]["Authorization"])
        assertEquals("session_id=abcdef123456", requestsReceived[0]["Cookie"])

        // 2nd request to cdn.storage-cdn.net must STRIP Authorization and Cookie
        assertNull(requestsReceived[1]["Authorization"], "Authorization header must be stripped on cross-host redirect")
        assertNull(requestsReceived[1]["Cookie"], "Cookie header must be stripped on cross-host redirect")
    }

    @Test
    fun testSameHostRedirectPreservesAuthorization() = runBlocking {
        val requestsReceived = mutableListOf<Map<String, String>>()

        val mockEngine = MockEngine { request ->
            val headersMap = mutableMapOf<String, String>()
            request.headers.forEach { name, values ->
                headersMap[name] = values.joinToString(";")
            }
            requestsReceived.add(headersMap)

            if (request.url.encodedPath == "/v1/audio") {
                // Same host relative redirect to /v2/audio
                respond(
                    content = "",
                    status = HttpStatusCode.Found,
                    headers = headersOf(HttpHeaders.Location, "/v2/audio")
                )
            } else {
                respond(
                    content = "AUDIO_DATA",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "audio/mpeg")
                )
            }
        }

        val testClient = HttpClient(mockEngine) {
            followRedirects = false
        }

        val streamSource = StreamSource(
            url = "https://api.musicapp.com/v1/audio",
            headers = mapOf("Authorization" to "Bearer persistent_token")
        )

        val response = LocalStreamingProxy.fetchUpstream(streamSource, null, testClient)
        assertEquals(HttpStatusCode.OK, response.status)

        assertEquals(2, requestsReceived.size)
        assertEquals("Bearer persistent_token", requestsReceived[0]["Authorization"])
        assertEquals("Bearer persistent_token", requestsReceived[1]["Authorization"], "Same-origin redirect must preserve Authorization")
    }

    @Test
    fun testRedirectChainUpTo5Hops() = runBlocking {
        var hopCount = 0

        val mockEngine = MockEngine { request ->
            val path = request.url.encodedPath
            val currentHop = path.removePrefix("/hop").toIntOrNull() ?: 0
            hopCount = currentHop

            if (currentHop < 5) {
                val nextHop = currentHop + 1
                respond(
                    content = "",
                    status = HttpStatusCode.TemporaryRedirect,
                    headers = headersOf(HttpHeaders.Location, "/hop$nextHop")
                )
            } else {
                respond(
                    content = "FINAL_HOP_REACHED",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "audio/mpeg")
                )
            }
        }

        val testClient = HttpClient(mockEngine) {
            followRedirects = false
        }

        val streamSource = StreamSource(url = "https://api.musicapp.com/hop0")
        val response = LocalStreamingProxy.fetchUpstream(streamSource, null, testClient, maxRedirectHops = 5)

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(5, hopCount, "Should successfully navigate up to 5 redirect transitions")
    }

    @Test
    fun testSchemeDowngradeStripsAuthorization() = runBlocking {
        val requestsReceived = mutableListOf<Map<String, String>>()

        val mockEngine = MockEngine { request ->
            val headersMap = mutableMapOf<String, String>()
            request.headers.forEach { name, values ->
                headersMap[name] = values.joinToString(";")
            }
            requestsReceived.add(headersMap)

            if (request.url.protocol.name == "https") {
                // Insecure downgrade redirect https -> http
                respond(
                    content = "",
                    status = HttpStatusCode.MovedPermanently,
                    headers = headersOf(HttpHeaders.Location, "http://api.musicapp.com/insecure-endpoint")
                )
            } else {
                respond(
                    content = "DOWNGRADED_CONTENT",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "audio/mpeg")
                )
            }
        }

        val testClient = HttpClient(mockEngine) {
            followRedirects = false
        }

        val streamSource = StreamSource(
            url = "https://api.musicapp.com/secure-endpoint",
            headers = mapOf("Authorization" to "Bearer sensitive_downgrade_token")
        )

        val response = LocalStreamingProxy.fetchUpstream(streamSource, null, testClient)
        assertEquals(HttpStatusCode.OK, response.status)

        assertEquals(2, requestsReceived.size)
        assertEquals("Bearer sensitive_downgrade_token", requestsReceived[0]["Authorization"])
        assertNull(requestsReceived[1]["Authorization"], "HTTPS to HTTP downgrade MUST strip Authorization header")
    }

    @Test
    fun testRollingTtlRenewalAndInactivityExpiration() {
        val now = System.currentTimeMillis()
        AudioOfflineCache.currentlyPlayingTrackId = "track_active_1"
        AudioOfflineCache.currentlyQueuedTrackIds = setOf("track_active_1", "track_active_2")

        val session = ProxySession(
            token = "token_sliding_1",
            trackId = "track_active_1",
            streamSource = StreamSource("https://example.com/audio.mp3"),
            lastAccessMs = now - 1000L,
            ttlMs = 5000L
        )

        // Still active within TTL
        assertFalse(session.isExpired())

        // Touch renews lastAccessMs
        val oldAccess = session.lastAccessMs
        Thread.sleep(10)
        session.touch()
        assertTrue(session.lastAccessMs > oldAccess, "session.touch() must advance lastAccessMs")

        // Expiration after inactivity timeout
        val expiredSession = ProxySession(
            token = "token_expired",
            trackId = "track_active_1",
            streamSource = StreamSource("https://example.com/audio.mp3"),
            lastAccessMs = System.currentTimeMillis() - (31 * 60 * 1000L), // 31 minutes ago
            ttlMs = 30 * 60 * 1000L
        )
        assertTrue(expiredSession.isExpired(), "Session inactive for > 30 min must expire")
    }

    @Test
    fun testSessionInvalidationOnTrackChangeAndQueueDeparture() {
        AudioOfflineCache.currentlyPlayingTrackId = "track_1"
        AudioOfflineCache.currentlyQueuedTrackIds = setOf("track_1", "track_2")

        val token1 = "token_t1"
        LocalStreamingProxy.sessions[token1] = ProxySession(
            token = token1,
            trackId = "track_1",
            streamSource = StreamSource("https://example.com/t1.mp3")
        )

        assertTrue(LocalStreamingProxy.sessions.containsKey(token1))

        // Change track to track_2 -> invalidate sessions for track_1
        LocalStreamingProxy.invalidateSessionsForTrack("track_1")
        assertFalse(LocalStreamingProxy.sessions.containsKey(token1), "Previous track session must be invalidated on track change")

        // Orphaned track not in current playing and not in queue must be expired
        val orphanSession = ProxySession(
            token = "token_orphan",
            trackId = "track_not_in_queue",
            streamSource = StreamSource("https://example.com/orphan.mp3")
        )
        assertTrue(orphanSession.isExpired(), "Track not in currently playing or queue must be considered expired")
    }
}
