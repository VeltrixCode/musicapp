package com.example.musicapp.lyrics

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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LrclibLyricsProviderTest {

    @Test
    fun testCleanTitleStripsParentheticalAndBracketTails() {
        assertEquals("Yesterday", LrclibLyricsProvider.cleanTitle("Yesterday (2020 Remaster)"))
        assertEquals("Yesterday", LrclibLyricsProvider.cleanTitle("Yesterday (Remastered 2011)"))
        assertEquals("Yesterday", LrclibLyricsProvider.cleanTitle("Yesterday (Remastered)"))
        assertEquals("Yesterday", LrclibLyricsProvider.cleanTitle("Yesterday [Remastered]"))
        assertEquals("Yesterday", LrclibLyricsProvider.cleanTitle("Yesterday (original)"))
        assertEquals("Yesterday", LrclibLyricsProvider.cleanTitle("Yesterday (Original Mix)"))
        assertEquals("Yesterday", LrclibLyricsProvider.cleanTitle("Yesterday (Original Version)"))
        assertEquals("Hotel California", LrclibLyricsProvider.cleanTitle("Hotel California (live)"))
        assertEquals("Hotel California", LrclibLyricsProvider.cleanTitle("Hotel California (Live at The Forum)"))
        assertEquals("Starboy", LrclibLyricsProvider.cleanTitle("Starboy [feat. Daft Punk]"))
        assertEquals("Empire State of Mind", LrclibLyricsProvider.cleanTitle("Empire State of Mind (feat. Alicia Keys)"))
        assertEquals("Song Title", LrclibLyricsProvider.cleanTitle("Song Title (ft. Guest)"))
        assertEquals("Song Title", LrclibLyricsProvider.cleanTitle("Song Title (Deluxe Edition)"))
        assertEquals("Song Title", LrclibLyricsProvider.cleanTitle("Song Title (Bonus Track)"))
        assertEquals("Song Title", LrclibLyricsProvider.cleanTitle("Song Title (Radio Edit)"))
        assertEquals("Song Title", LrclibLyricsProvider.cleanTitle("Song Title (Album Version)"))
        assertEquals("Song Title", LrclibLyricsProvider.cleanTitle("Song Title (Official Music Video)"))
    }

    @Test
    fun testCleanTitleHandlesMultipleTailsAndTrailingFeat() {
        assertEquals("Song Title", LrclibLyricsProvider.cleanTitle("Song Title (feat. Guest) (2020 Remaster)"))
        assertEquals("Song Title", LrclibLyricsProvider.cleanTitle("Song Title feat. Guest"))
        assertEquals("Song Title", LrclibLyricsProvider.cleanTitle("Song Title ft. Guest"))
        assertEquals("Song Title", LrclibLyricsProvider.cleanTitle("Song Title featuring Guest"))
    }

    @Test
    fun testCleanArtistStripsFeaturedArtists() {
        assertEquals("The Weeknd", LrclibLyricsProvider.cleanArtist("The Weeknd feat. Daft Punk"))
        assertEquals("The Weeknd", LrclibLyricsProvider.cleanArtist("The Weeknd ft. Daft Punk"))
        assertEquals("The Weeknd", LrclibLyricsProvider.cleanArtist("The Weeknd featuring Daft Punk"))
        assertEquals("The Weeknd", LrclibLyricsProvider.cleanArtist("The Weeknd (feat. Daft Punk)"))
        assertEquals("The Weeknd", LrclibLyricsProvider.cleanArtist("The Weeknd [feat. Daft Punk]"))
        assertEquals("Coldplay", LrclibLyricsProvider.cleanArtist("Coldplay"))
    }

    @Test
    fun testShortenTitleRemovesSubtitlesAndDelimiters() {
        assertEquals("Space Oddity", LrclibLyricsProvider.shortenTitle("Space Oddity - 2015 Remaster"))
        assertEquals("Track Name", LrclibLyricsProvider.shortenTitle("Track Name / Alternate Title"))
        assertEquals("Track Name", LrclibLyricsProvider.shortenTitle("Track Name : Subtitle"))
        assertEquals("Track Name", LrclibLyricsProvider.shortenTitle("Track Name (Acoustic)"))
        assertEquals("Track Name", LrclibLyricsProvider.shortenTitle("Track Name [Live]"))
    }

    @Test
    fun testShortenArtistExtractsPrimaryArtist() {
        assertEquals("David Bowie", LrclibLyricsProvider.shortenArtist("David Bowie & Queen"))
        assertEquals("Eminem", LrclibLyricsProvider.shortenArtist("Eminem, Rihanna"))
        assertEquals("Marshmello", LrclibLyricsProvider.shortenArtist("Marshmello x Bastille"))
        assertEquals("Artist 1", LrclibLyricsProvider.shortenArtist("Artist 1 / Artist 2"))
        assertEquals("Artist 1", LrclibLyricsProvider.shortenArtist("Artist 1 vs. Artist 2"))
    }

    @Test
    fun testFallbackQueryWhenInitialQueryFails() = runBlocking {
        val requestedUrls = mutableListOf<String>()
        val mockEngine = MockEngine { request ->
            val url = request.url.toString()
            requestedUrls.add(url)

            if (url.contains("track_name=Space%20Oddity%20-%202015%20Remaster") ||
                url.contains("track_name=Space+Oddity+-+2015+Remaster")
            ) {
                // Initial query with subtitle fails
                respond(
                    content = """{"error": "not found"}""",
                    status = HttpStatusCode.NotFound,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            } else if (url.contains("track_name=Space%20Oddity") || url.contains("track_name=Space+Oddity")) {
                // Fallback query with shortened title succeeds
                respond(
                    content = """{
                        "id": 999,
                        "name": "Space Oddity",
                        "artistName": "David Bowie",
                        "syncedLyrics": "[00:15.00]Ground Control to Major Tom"
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

        val result = LrclibLyricsProvider.fetchLyrics(
            client = client,
            artist = "David Bowie feat. Rick Wakeman",
            title = "Space Oddity - 2015 Remaster",
            durationMs = 315_000L
        )

        assertNotNull(result)
        val (lines, rawText) = result
        assertEquals(1, lines.size)
        assertEquals(15000L, lines[0].timestampMs)
        assertEquals("Ground Control to Major Tom", lines[0].text)
        assertTrue(rawText.contains("Ground Control to Major Tom"))
        assertTrue(requestedUrls.size >= 2, "Expected initial query followed by fallback query")
    }
}
