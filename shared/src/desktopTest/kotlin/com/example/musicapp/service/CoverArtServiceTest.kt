package com.example.musicapp.service

import com.example.musicapp.model.Track
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CoverArtServiceTest {

    private val samplePngBytes: ByteArray by lazy {
        val baos = java.io.ByteArrayOutputStream()
        val img = java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_ARGB)
        javax.imageio.ImageIO.write(img, "png", baos)
        baos.toByteArray()
    }

    @BeforeTest
    fun setup() {
        CoverArtService.negativeCache.clear()
        CoverArtService.offlineMode = false
        CoverArtService.loadOnlineCovers = true
    }

    @Test
    fun testPrivacyPathSanitization() {
        assertTrue(CoverArtService.containsLocalFilePathOrExtension("C:\\Music\\Track.mp3"))
        assertTrue(CoverArtService.containsLocalFilePathOrExtension("/home/user/song.flac"))
        assertTrue(CoverArtService.containsLocalFilePathOrExtension("artist.ogg"))
        assertTrue(CoverArtService.containsLocalFilePathOrExtension("track.wav"))

        assertFalse(CoverArtService.containsLocalFilePathOrExtension("Daft Punk"))
        assertFalse(CoverArtService.containsLocalFilePathOrExtension("Get Lucky"))
    }

    @Test
    fun testTrackWithLocalPathLeaksNoNetworkRequests(): Unit = runBlocking {
        var networkCalls = 0
        val mockEngine = MockEngine {
            networkCalls++
            respond("{}", HttpStatusCode.OK)
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        val track = Track(
            id = "t_leak",
            title = "song.mp3",
            artist = "C:\\Users\\Music\\artist",
            album = "Album",
            durationMs = 120_000L
        )

        val result = CoverArtService.loadArtworkForTrack(track, client)
        assertNull(result)
        assertEquals(0, networkCalls, "Must not query external network when artist/title contain file paths")
    }

    @Test
    fun testOfflineModeMakesZeroRequests(): Unit = runBlocking {
        var networkCalls = 0
        val mockEngine = MockEngine {
            networkCalls++
            respond("{}", HttpStatusCode.OK)
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        CoverArtService.offlineMode = true

        val track = Track(
            id = "t_offline",
            title = "Starlight",
            artist = "Muse",
            album = "Black Holes",
            durationMs = 180_000L
        )

        val result = CoverArtService.loadArtworkForTrack(track, client)
        assertNull(result)
        assertEquals(0, networkCalls, "Offline mode must prevent all outbound requests")
    }

    @Test
    fun testItunesSearchSuccess(): Unit = runBlocking {
        val mockEngine = MockEngine { request ->
            val url = request.url.toString()
            if (url.contains("itunes.apple.com")) {
                respond(
                    content = """{"resultCount":1,"results":[{"artworkUrl100":"https://is1-ssl.mzstatic.com/image/thumb/Music/v4/100x100bb.jpg"}]}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            } else if (url.contains("600x600bb.jpg")) {
                respond(
                    content = samplePngBytes,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "image/png")
                )
            } else {
                respond("{}", HttpStatusCode.NotFound)
            }
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        val artist = "Queen_${System.currentTimeMillis()}"
        val title = "Bohemian_${System.currentTimeMillis()}"
        val result = CoverArtService.getOrFetchOnlineCover(artist, title, client)
        assertNotNull(result)
        Unit
    }

    @Test
    fun testNegativeCacheStoresMissForSevenDays(): Unit = runBlocking {
        var itunesCalls = 0
        val mockEngine = MockEngine { request ->
            itunesCalls++
            respond(
                content = """{"resultCount":0,"results":[]}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        // First call: misses and writes to negative cache
        val result1 = CoverArtService.getOrFetchOnlineCover("UnknownArtist999", "NonexistentSong999", client)
        assertNull(result1)
        val initialCalls = itunesCalls
        assertTrue(initialCalls > 0)

        // Second call immediately after: negative cache hit, zero new requests!
        val result2 = CoverArtService.getOrFetchOnlineCover("UnknownArtist999", "NonexistentSong999", client)
        assertNull(result2)
        assertEquals(initialCalls, itunesCalls, "Second query must hit negative cache and not call network")
    }
}
