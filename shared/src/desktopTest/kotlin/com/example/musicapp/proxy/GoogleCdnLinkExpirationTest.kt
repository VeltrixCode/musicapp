package com.example.musicapp.proxy

import com.example.musicapp.model.StreamSource
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GoogleCdnLinkExpirationTest {

    @BeforeTest
    @AfterTest
    fun cleanupState() {
        LocalStreamingProxy.activePlayingTrackId = null
        com.example.musicapp.cache.AudioOfflineCache.currentlyPlayingTrackId = null
        LocalStreamingProxy.setActiveQueuedTracks(emptyList())
        com.example.musicapp.cache.AudioOfflineCache.currentlyQueuedTrackIds = emptySet()
    }

    @Test
    fun testRepeatedRangeRequestAfterCdnLinkExpiration() = runBlocking {
        val requestsList = mutableListOf<Pair<String, Map<String, String>>>()
        val cdn1Hits = AtomicInteger(0)
        val apiHits = AtomicInteger(0)
        val cdn2Hits = AtomicInteger(0)

        val mockEngine = MockEngine { request ->
            val urlStr = request.url.toString()
            val host = request.url.host
            val headersMap = mutableMapOf<String, String>()
            request.headers.forEach { name, values ->
                headersMap[name] = values.joinToString(";")
            }
            requestsList.add(urlStr to headersMap)

            when {
                host == "www.googleapis.com" -> {
                    val hit = apiHits.incrementAndGet()
                    val targetCdn = if (hit == 1) "https://doc-cdn1.googleusercontent.com/stream?id=file123&token=old"
                                    else "https://doc-cdn2.googleusercontent.com/stream?id=file123&token=fresh"
                    respond(
                        content = "",
                        status = HttpStatusCode.Found,
                        headers = headersOf(HttpHeaders.Location, targetCdn)
                    )
                }
                host == "doc-cdn1.googleusercontent.com" -> {
                    val count = cdn1Hits.incrementAndGet()
                    if (count == 1) {
                        // First request succeeds
                        respond(
                            content = "BYTE_0_TO_100",
                            status = HttpStatusCode.PartialContent,
                            headers = headersOf(
                                HttpHeaders.ContentType to listOf("audio/mpeg"),
                                HttpHeaders.ContentRange to listOf("bytes 0-100/1000")
                            )
                        )
                    } else {
                        // Second request hits expired link -> 403 Forbidden
                        respond(
                            content = "Forbidden: Signature expired",
                            status = HttpStatusCode.Forbidden
                        )
                    }
                }
                host == "doc-cdn2.googleusercontent.com" -> {
                    cdn2Hits.incrementAndGet()
                    respond(
                        content = "BYTE_101_TO_200",
                        status = HttpStatusCode.PartialContent,
                        headers = headersOf(
                            HttpHeaders.ContentType to listOf("audio/mpeg"),
                            HttpHeaders.ContentRange to listOf("bytes 101-200/1000")
                        )
                    )
                }
                else -> error("Unexpected request to $urlStr")
            }
        }

        val testHttpClient = HttpClient(mockEngine) {
            followRedirects = false
        }

        val sessions = ConcurrentHashMap<String, ProxySession>()
        val initialStream = StreamSource(
            url = "https://www.googleapis.com/drive/v3/files/file123?alt=media",
            headers = mapOf("Authorization" to "Bearer google_access_token_abc")
        )

        val session = ProxySession(
            token = "session-google-test",
            trackId = "track-gdrive-1",
            streamSource = initialStream
        )
        sessions["session-google-test"] = session

        testApplication {
            application {
                configureStreamingProxyRoutes(
                    application = this,
                    sessionsMap = sessions,
                    hostValidator = { true },
                    clientProvider = { testHttpClient }
                )
            }

            val client = createClient { }

            // 1. Initial Range request: bytes=0-100
            val resp1 = client.get("/stream/session-google-test") {
                header(HttpHeaders.Host, "127.0.0.1")
                header(HttpHeaders.Range, "bytes=0-100")
            }
            assertEquals(HttpStatusCode.PartialContent, resp1.status)
            assertEquals("BYTE_0_TO_100", resp1.bodyAsText())
            assertTrue(session.isDirectUrlValid())
            assertEquals("https://doc-cdn1.googleusercontent.com/stream?id=file123&token=old", session.resolvedDirectUrl)

            // 2. Second Range request: bytes=101-200. The cached direct URL returns 403.
            // Proxy must reset direct URL, re-query googleapis.com, obtain new CDN link, and serve bytes 101-200!
            val resp2 = client.get("/stream/session-google-test") {
                header(HttpHeaders.Host, "127.0.0.1")
                header(HttpHeaders.Range, "bytes=101-200")
            }
            assertEquals(HttpStatusCode.PartialContent, resp2.status)
            assertEquals("BYTE_101_TO_200", resp2.bodyAsText())
            assertTrue(session.isDirectUrlValid())
            assertEquals("https://doc-cdn2.googleusercontent.com/stream?id=file123&token=fresh", session.resolvedDirectUrl)

            // 3. Security check: Authorization must NEVER be sent to googleusercontent.com
            for ((url, headers) in requestsList) {
                if (url.contains("googleusercontent.com")) {
                    assertNull(headers["Authorization"], "Foreign CDN host MUST NOT receive Authorization header: $url")
                } else if (url.contains("googleapis.com")) {
                    assertEquals("Bearer google_access_token_abc", headers["Authorization"])
                }
            }
        }
    }
}
