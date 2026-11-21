package com.example.musicapp.service

import com.example.musicapp.model.LrcLine
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
import kotlin.test.assertTrue

class LyricsServiceTest {

    @BeforeTest
    fun setup() {
        LyricsService.clearCache()
        LyricsService.fetchOnlineLyrics = false
        LyricsService.offlineMode = false
    }

    @Test
    fun testDefaultSettingFetchOnlineLyricsDisabledMakesZeroRequests() = runBlocking {
        var networkCalls = 0
        val mockEngine = MockEngine {
            networkCalls++
            respond("{}", HttpStatusCode.OK)
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        val track = Track(
            id = "t_lyrics_disabled",
            title = "Clocks",
            artist = "Coldplay",
            album = "A Rush of Blood",
            durationMs = 300_000L
        )

        // Default setting: fetchOnlineLyrics is false
        val lyrics = LyricsService.getLyrics(track, client)
        assertTrue(lyrics.isEmpty())
        assertEquals(0, networkCalls, "Default privacy setting must prevent online lyrics lookup")
    }

    @Test
    fun testOfflineModeMakesZeroRequestsEvenIfSettingEnabled() = runBlocking {
        var networkCalls = 0
        val mockEngine = MockEngine {
            networkCalls++
            respond("{}", HttpStatusCode.OK)
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        LyricsService.fetchOnlineLyrics = true
        LyricsService.offlineMode = true

        val track = Track(
            id = "t_lyrics_offline",
            title = "Yellow",
            artist = "Coldplay",
            album = "Parachutes",
            durationMs = 260_000L
        )

        val lyrics = LyricsService.getLyrics(track, client)
        assertTrue(lyrics.isEmpty())
        assertEquals(0, networkCalls, "Global offline mode must block online lyrics queries")
    }

    @Test
    fun testMemoryCacheHits() = runBlocking {
        var networkCalls = 0
        val mockEngine = MockEngine {
            networkCalls++
            respond("{}", HttpStatusCode.OK)
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        val track = Track(
            id = "t_mem",
            title = "Cached Song",
            artist = "Band",
            album = "Album",
            durationMs = 180_000L
        )

        // Populate in-memory cache directly
        val cachedLines = listOf(
            LrcLine(1000L, "First line"),
            LrcLine(4000L, "Second line")
        )
        LyricsService.memoryCache["band_cached song"] = cachedLines

        LyricsService.fetchOnlineLyrics = true
        val result = LyricsService.getLyrics(track, client)

        assertEquals(2, result.size)
        assertEquals("First line", result[0].text)
        assertEquals(0, networkCalls, "In-memory cache must prevent network queries")
    }

    @Test
    fun testFetchOnlineLyricsSuccessWithUserAgent() = runBlocking {
        var capturedUserAgent = ""
        val mockEngine = MockEngine { request ->
            capturedUserAgent = request.headers[HttpHeaders.UserAgent] ?: ""
            respond(
                content = """{
                    "id": 12345,
                    "name": "Viva La Vida",
                    "artistName": "Coldplay",
                    "syncedLyrics": "[00:10.50]I used to rule the world\n[00:15.20]Seas would rise when I gave the word"
                }""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        LyricsService.fetchOnlineLyrics = true

        val ts = System.currentTimeMillis()
        val track = Track(
            id = "t_viva_$ts",
            title = "Viva La Vida $ts",
            artist = "Coldplay $ts",
            album = "Viva",
            durationMs = 240_000L
        )

        val lyrics = LyricsService.getLyrics(track, client)
        assertEquals(2, lyrics.size)
        assertEquals(10500L, lyrics[0].timestampMs)
        assertEquals("I used to rule the world", lyrics[0].text)
        assertEquals(LyricsService.USER_AGENT, capturedUserAgent)
    }

    @Test
    fun testMissingLyricsReturnsEmptyList() = runBlocking {
        val mockEngine = MockEngine {
            respond(
                content = """{"id":0,"name":"Unknown","artistName":"Unknown","syncedLyrics":null,"plainLyrics":null}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        LyricsService.fetchOnlineLyrics = true

        val track = Track(
            id = "t_unknown",
            title = "Instrumental Track",
            artist = "Composer",
            album = "Soundtrack",
            durationMs = 150_000L
        )

        val lyrics = LyricsService.getLyrics(track, client)
        assertTrue(lyrics.isEmpty())
    }

    @Test
    fun testTitleAndArtistCleaningBeforeQuery() = runBlocking {
        var requestedUrl = ""
        val ts = System.currentTimeMillis()
        val mockEngine = MockEngine { request ->
            requestedUrl = request.url.toString()
            respond(
                content = """{
                    "id": 555,
                    "name": "Karma Police $ts",
                    "artistName": "Radiohead $ts",
                    "syncedLyrics": "[00:10.00]Karma Police, arrest this man"
                }""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        LyricsService.fetchOnlineLyrics = true

        val track = Track(
            id = "t_cleaning_$ts",
            title = "Karma Police $ts (Remastered 2020) (feat. Someone)",
            artist = "Radiohead $ts feat. Guest",
            album = "OK Computer",
            durationMs = 260_000L
        )

        val lyrics = LyricsService.getLyrics(track, client)
        assertEquals(1, lyrics.size)
        assertEquals("Karma Police, arrest this man", lyrics[0].text)
        assertTrue(requestedUrl.contains("Karma") && requestedUrl.contains("Police"))
        assertTrue(!requestedUrl.contains("Remastered"))
        assertTrue(!requestedUrl.contains("Someone"))
        assertTrue(!requestedUrl.contains("Guest"))
    }

    @Test
    fun testFallbackQueryWhenInitialQueryFails() = runBlocking {
        val requestedUrls = mutableListOf<String>()
        val ts = System.currentTimeMillis()
        val mockEngine = MockEngine { request ->
            val url = request.url.toString()
            requestedUrls.add(url)
            if (url.contains("Single%20Version") || url.contains("Single+Version")) {
                respond("{}", HttpStatusCode.NotFound)
            } else if (url.contains("Heroes")) {
                respond(
                    content = """{
                        "id": 888,
                        "name": "Heroes $ts",
                        "artistName": "David Bowie $ts",
                        "syncedLyrics": "[00:20.00]I, I will be king"
                    }""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            } else {
                respond("{}", HttpStatusCode.NotFound)
            }
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        LyricsService.fetchOnlineLyrics = true

        val track = Track(
            id = "t_fallback_$ts",
            title = "Heroes $ts - Single Version",
            artist = "David Bowie $ts",
            album = "Heroes",
            durationMs = 210_000L
        )

        val lyrics = LyricsService.getLyrics(track, client)
        assertEquals(1, lyrics.size)
        assertEquals("I, I will be king", lyrics[0].text)
        assertTrue(requestedUrls.size >= 2, "Expected initial query to fail and fallback query to execute")
    }
}
