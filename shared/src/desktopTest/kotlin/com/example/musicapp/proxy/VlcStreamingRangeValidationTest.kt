package com.example.musicapp.proxy

import com.example.musicapp.cache.AudioOfflineCache
import com.example.musicapp.model.StreamSource
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VlcStreamingRangeValidationTest {

    @BeforeTest
    @AfterTest
    fun cleanupState() {
        LocalStreamingProxy.activePlayingTrackId = null
        AudioOfflineCache.currentlyPlayingTrackId = null
        LocalStreamingProxy.setActiveQueuedTracks(emptyList())
        AudioOfflineCache.currentlyQueuedTrackIds = emptySet()
    }

    @Test
    fun testVlcRangeRequestReturns206WithRfc7233HeadersAndAudioContentType() = runBlocking {
        val testSessions = ConcurrentHashMap<String, ProxySession>()
        val token = "vlc_session_token_abc"
        val totalBytes = 5_000_000L

        // StreamSource specifies audio/mpeg and known content length (as extracted from Google Drive / Cloud providers)
        testSessions[token] = ProxySession(
            token = token,
            trackId = "gdrive_track_1",
            streamSource = StreamSource(
                url = "https://www.googleapis.com/drive/v3/files/file123?alt=media",
                contentType = "audio/mpeg",
                contentLength = totalBytes
            )
        )

        // Mock upstream returning 200 OK with application/octet-stream (typical Google Drive download behavior)
        val mockEngine = MockEngine { _ ->
            respond(
                content = "DUMMY_MP3_STREAM_PAYLOAD",
                status = HttpStatusCode.OK,
                headers = headersOf(
                    HttpHeaders.ContentType to listOf("application/octet-stream"),
                    HttpHeaders.ContentLength to listOf(totalBytes.toString())
                )
            )
        }

        testApplication {
            application {
                configureStreamingProxyRoutes(
                    application = this,
                    sessionsMap = testSessions,
                    hostValidator = { true },
                    clientProvider = { HttpClient(mockEngine) }
                )
            }

            // Simulate VLC's initial probing request: GET with Range: bytes=0-
            val response = client.get("/stream/$token") {
                header(HttpHeaders.Host, "127.0.0.1:8080")
                header(HttpHeaders.Range, "bytes=0-")
                header(HttpHeaders.UserAgent, "VLC/3.0.21 LibVLC/3.0.21")
            }

            // (a) Must respond with HTTP 206 Partial Content (not 200 OK)
            assertEquals(HttpStatusCode.PartialContent, response.status, "VLC Range requests must receive 206 Partial Content")

            // (b) Content-Range header must follow RFC 7233 format
            val contentRange = response.headers[HttpHeaders.ContentRange]
            assertNotNull(contentRange, "Content-Range header must be present on 206 response")
            assertEquals("bytes 0-${totalBytes - 1}/$totalBytes", contentRange, "Content-Range must strictly adhere to RFC 7233")

            // (c) Accept-Ranges must be present
            assertEquals("bytes", response.headers[HttpHeaders.AcceptRanges], "Accept-Ranges must be 'bytes'")

            // (d) Content-Type must NOT be application/octet-stream; it must be concrete audio/mpeg
            val contentType = response.headers[HttpHeaders.ContentType]
            assertEquals("audio/mpeg", contentType, "Content-Type must be valid audio MIME type, not application/octet-stream")
        }
    }

    @Test
    fun testVlcFlacRangeRequestPreservesFlacContentType() = runBlocking {
        val testSessions = ConcurrentHashMap<String, ProxySession>()
        val token = "vlc_flac_token"
        val totalBytes = 48_000_000L

        testSessions[token] = ProxySession(
            token = token,
            trackId = "gdrive_flac_track",
            streamSource = StreamSource(
                url = "https://doc-01.googleusercontent.com/stream/file_flac",
                contentType = "audio/flac",
                contentLength = totalBytes
            )
        )

        val mockEngine = MockEngine { _ ->
            respond(
                content = "FLAC_STREAM_BYTES",
                status = HttpStatusCode.PartialContent,
                headers = headersOf(
                    HttpHeaders.ContentType to listOf("application/octet-stream"),
                    HttpHeaders.ContentRange to listOf("bytes 0-${totalBytes - 1}/$totalBytes"),
                    HttpHeaders.ContentLength to listOf(totalBytes.toString())
                )
            )
        }

        testApplication {
            application {
                configureStreamingProxyRoutes(
                    application = this,
                    sessionsMap = testSessions,
                    hostValidator = { true },
                    clientProvider = { HttpClient(mockEngine) }
                )
            }

            val response = client.get("/stream/$token") {
                header(HttpHeaders.Host, "127.0.0.1:8080")
                header(HttpHeaders.Range, "bytes=0-")
            }

            assertEquals(HttpStatusCode.PartialContent, response.status)
            assertEquals("bytes 0-${totalBytes - 1}/$totalBytes", response.headers[HttpHeaders.ContentRange])
            assertEquals("audio/flac", response.headers[HttpHeaders.ContentType], "FLAC mime type must be preserved")
        }
    }

    @Test
    fun testFreshSessionRemainsAliveWhenQueuedDuringActivePlayback() {
        // Active playback of a different track is currently underway
        LocalStreamingProxy.activePlayingTrackId = "currently_playing_track_xyz"
        AudioOfflineCache.currentlyPlayingTrackId = "currently_playing_track_xyz"
        LocalStreamingProxy.setActiveQueuedTracks(listOf("currently_playing_track_xyz", "upcoming_track_abc"))

        // A new track session is registered for queued track
        val queuedSession = ProxySession(
            token = "new_incoming_track_token",
            trackId = "upcoming_track_abc",
            streamSource = StreamSource("https://example.com/audio.mp3")
        )

        // Must not be expired!
        assertFalse(
            queuedSession.isExpired(),
            "A session for a queued track must NOT expire during active playback of another track"
        )
    }
}
