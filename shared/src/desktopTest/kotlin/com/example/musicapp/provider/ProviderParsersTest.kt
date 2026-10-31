package com.example.musicapp.provider

import com.example.musicapp.cache.AudioOfflineCache
import com.example.musicapp.library.Library
import com.example.musicapp.model.Track
import com.example.musicapp.model.TrackSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProviderParsersTest {

    @Test
    fun testInternetArchiveParserWithLicense() {
        val provider = InternetArchiveProvider()
        val jsonFixture = """
        {
          "response": {
            "numFound": 1,
            "start": 0,
            "docs": [
              {
                "identifier": "test_audio_123",
                "title": "Ambient Meditation",
                "creator": "Open Music Project",
                "album": "Calm Reflections",
                "mediatype": "audio",
                "licenseurl": "http://creativecommons.org/licenses/by-nc-sa/4.0/",
                "year": "2021"
              }
            ]
          }
        }
        """.trimIndent()

        val results = provider.parseSearchResponse(jsonFixture)
        assertEquals(1, results.size)
        val track = results.first()
        assertEquals("ia_test_audio_123", track.id)
        assertEquals("Ambient Meditation", track.title)
        assertEquals("Open Music Project", track.artist)
        assertEquals("Calm Reflections", track.album)
        assertEquals("http://creativecommons.org/licenses/by-nc-sa/4.0/", track.license)
        assertEquals("https://archive.org/details/test_audio_123", track.webPageUrl)
        assertEquals(TrackSource.INTERNET_ARCHIVE, track.source)
        assertFalse(InternetArchiveProvider.isLicenseRestrictedOrUnknown(track.license))
    }

    @Test
    fun testInternetArchiveParserWithoutLicenseShowsDefault() {
        val provider = InternetArchiveProvider()
        val jsonFixture = """
        {
          "response": {
            "numFound": 1,
            "start": 0,
            "docs": [
              {
                "identifier": "no_license_song",
                "title": "Old Archive Recording",
                "creator": "Archivist"
              }
            ]
          }
        }
        """.trimIndent()

        val results = provider.parseSearchResponse(jsonFixture)
        assertEquals(1, results.size)
        val track = results.first()
        assertEquals("лицензия не указана", track.license)
        assertTrue(InternetArchiveProvider.isLicenseRestrictedOrUnknown(track.license))
    }

    @Test
    fun testSubsonicSongParserWithoutCredentialLeak() {
        val provider = SubsonicProvider()
        val jsonFixture = """
        {
          "subsonic-response": {
            "status": "ok",
            "version": "1.16.1",
            "searchResult3": {
              "song": [
                {
                  "id": "song_456",
                  "title": "Subsonic Groove",
                  "artist": "Navidrome Artist",
                  "album": "Server Beats",
                  "duration": 215,
                  "bitRate": 320,
                  "suffix": "flac",
                  "contentType": "audio/flac",
                  "coverArt": "al-456",
                  "size": 24500000
                }
              ]
            }
          }
        }
        """.trimIndent()

        val results = provider.parseSubsonicSongs(jsonFixture, "https://navidrome.local")
        assertEquals(1, results.size)
        val track = results.first()
        assertEquals("song_456", track.id)
        assertEquals("Subsonic Groove", track.title)
        assertEquals("Navidrome Artist", track.artist)
        assertEquals("Server Beats", track.album)
        assertEquals(215000L, track.durationMs)
        assertEquals(320, track.bitrateKbps)
        assertEquals("flac", track.format)
        assertTrue(track.isLossless)
        assertTrue(track.streamUrl!!.contains("stream.view?id=song_456"))
        assertTrue(track.artworkUrl!!.contains("getCoverArt.view?id=al-456"))
        assertEquals(TrackSource.SUBSONIC, track.source)

        // Ensure token and salt NEVER leak into parsed streamUrl or artworkUrl
        assertFalse(track.streamUrl!!.contains("t="), "streamUrl must not contain auth token")
        assertFalse(track.streamUrl!!.contains("s="), "streamUrl must not contain auth salt")
        assertFalse(track.artworkUrl!!.contains("t="), "artworkUrl must not contain auth token")
        assertFalse(track.artworkUrl!!.contains("s="), "artworkUrl must not contain auth salt")
    }

    @Test
    fun testSubsonicCredentialSanitization() {
        val rawUrl = "https://sub.domain.com/rest/ping.view?u=alice&t=0123456789abcdef&s=jvx1234&v=1.16.1&c=JVXMusic"
        val sanitized = SubsonicProvider.sanitizeSubsonicUrl(rawUrl)
        assertFalse(sanitized.contains("0123456789abcdef"), "Token must be masked")
        assertFalse(sanitized.contains("jvx1234"), "Salt must be masked")
        assertTrue(sanitized.contains("t=***"))
        assertTrue(sanitized.contains("s=***"))

        val errorWithPassword = "Auth failed for user alice with password=supersecretpass and p=secret123"
        val sanitizedError = SubsonicProvider.sanitizeSubsonicUrl(errorWithPassword)
        assertFalse(sanitizedError.contains("supersecretpass"))
        assertFalse(sanitizedError.contains("secret123"))
        assertTrue(sanitizedError.contains("p=***") || sanitizedError.contains("password=***"))
    }

    @Test
    fun testSubsonicLanDetectionAndPlainHttpWarning() {
        assertTrue(SubsonicProvider.isLanAddress("http://127.0.0.1:4533"))
        assertTrue(SubsonicProvider.isLanAddress("http://localhost:4533"))
        assertTrue(SubsonicProvider.isLanAddress("http://192.168.1.50:4040/rest"))
        assertTrue(SubsonicProvider.isLanAddress("http://10.0.0.5:4040"))
        assertTrue(SubsonicProvider.isLanAddress("http://172.20.0.2:4040"))
        assertTrue(SubsonicProvider.isLanAddress("http://nas.local:4040"))

        assertFalse(SubsonicProvider.isLanAddress("http://music.example.com"))
        assertFalse(SubsonicProvider.isLanAddress("http://8.8.8.8:4040"))

        // Remote server on plain HTTP must trigger warning
        val warning = SubsonicProvider.checkPlainHttpWarning("http://music.example.com")
        assertNotNull(warning, "Plain HTTP on remote server must return security warning")

        // LAN server on plain HTTP should NOT trigger remote warning
        val lanWarning = SubsonicProvider.checkPlainHttpWarning("http://192.168.1.10:4533")
        assertNull(lanWarning)

        // HTTPS on remote server should NOT trigger plain HTTP warning
        val httpsWarning = SubsonicProvider.checkPlainHttpWarning("https://music.example.com")
        assertNull(httpsWarning)
    }

    @Test
    fun testRadioBrowserParser() {
        val provider = RadioBrowserProvider()
        val jsonFixture = """
        [
          {
            "stationuuid": "964e5914-0608-42fa-b6ff-3f40d3d64287",
            "name": "Jazz Radio Paris",
            "url_resolved": "https://stream.jazzradio.fr/jazz-radio.mp3",
            "favicon": "https://jazzradio.fr/favicon.ico",
            "tags": "jazz,classic,chill",
            "country": "France",
            "bitrate": 192,
            "codec": "mp3",
            "homepage": "https://jazzradio.fr"
          }
        ]
        """.trimIndent()

        val results = provider.parseRadioStationsJson(jsonFixture)
        assertEquals(1, results.size)
        val station = results.first()
        assertEquals("964e5914-0608-42fa-b6ff-3f40d3d64287", station.id)
        assertEquals("Jazz Radio Paris", station.title)
        assertEquals("jazz, classic, chill", station.artist)
        assertEquals("Интернет-радио", station.album)
        assertEquals("https://stream.jazzradio.fr/jazz-radio.mp3", station.streamUrl)
        assertEquals(192, station.bitrateKbps)
        assertEquals("mp3", station.format)
        assertEquals(TrackSource.RADIO_BROWSER, station.source)
        assertEquals("https://jazzradio.fr", station.webPageUrl)
    }

    @Test
    fun testRadioOfflineDownloadAndPreloadDisabled() {
        val radioTrack = Track(
            id = "radio_live_1",
            title = "Live FM",
            artist = "Radio",
            source = "https://live.fm/stream.mp3",
            durationMs = 0L,
            sourceProvider = TrackSource.RADIO_BROWSER
        )
        assertTrue(radioTrack.isLiveStream)

        // Preload must do nothing
        AudioOfflineCache.preloadNextTrack(radioTrack, radioTrack.source)
        assertFalse(AudioOfflineCache.isCached(radioTrack.id), "Live radio track must never be cached")

        // Direct download must do nothing
        AudioOfflineCache.downloadTrack(radioTrack, radioTrack.source!!)
        assertFalse(AudioOfflineCache.isCached(radioTrack.id), "Live radio track must never be downloaded")

        // Library duration resolution must ignore duration updates for live streams
        val library = Library()
        library.tracks.clear()
        library.tracks.add(radioTrack)
        library.updateTrackDuration("radio_live_1", 300_000L)
        assertEquals(0L, library.tracks.first().durationMs, "Live radio track duration must not be updated to synthetic value")
    }

    @Test
    fun testWebDavXmlParserNextcloud() {
        val provider = WebDavProvider()
        val nextcloudXml = """
        <?xml version="1.0" encoding="utf-8"?>
        <d:multistatus xmlns:d="DAV:" xmlns:s="http://sabredav.org/ns" xmlns:oc="http://owncloud.org/ns">
          <d:response>
            <d:href>/remote.php/dav/files/user/Music/Daft%20Punk%20-%20One%20More%20Time.mp3</d:href>
            <d:propstat>
              <d:prop>
                <d:displayname>Daft Punk - One More Time.mp3</d:displayname>
                <d:getcontenttype>audio/mpeg</d:getcontenttype>
                <d:getcontentlength>8500000</d:getcontentlength>
              </d:prop>
              <d:status>HTTP/1.1 200 OK</d:status>
            </d:propstat>
          </d:response>
          <d:response>
            <d:href>/remote.php/dav/files/user/Music/notes.txt</d:href>
            <d:propstat>
              <d:prop>
                <d:displayname>notes.txt</d:displayname>
                <d:getcontenttype>text/plain</d:getcontenttype>
                <d:getcontentlength>120</d:getcontentlength>
              </d:prop>
              <d:status>HTTP/1.1 200 OK</d:status>
            </d:propstat>
          </d:response>
        </d:multistatus>
        """.trimIndent()

        val results = provider.parseWebDavXml(nextcloudXml, "https://cloud.example.com")
        assertEquals(1, results.size, "Non-audio files like txt must be skipped")
        val track = results.first()
        assertEquals("One More Time", track.title)
        assertEquals("Daft Punk", track.artist)
        assertEquals("mp3", track.format)
        assertEquals(8500000L, track.sizeBytes)
        assertEquals(TrackSource.WEBDAV, track.source)
        assertTrue(track.streamUrl!!.contains("/Music/Daft%20Punk%20-%20One%20More%20Time.mp3"))
    }

    @Test
    fun testWebDavXmlParserOwnCloud() {
        val provider = WebDavProvider()
        val owncloudXml = """
        <?xml version="1.0" encoding="utf-8"?>
        <D:multistatus xmlns:D="DAV:">
          <D:response>
            <D:href>/files/Queen%20-%20Bohemian%20Rhapsody.flac</D:href>
            <D:propstat>
              <D:prop>
                <D:displayname>Queen - Bohemian Rhapsody.flac</D:displayname>
                <D:getcontenttype>audio/flac</D:getcontenttype>
                <D:getcontentlength>42000000</D:getcontentlength>
              </D:prop>
              <D:status>HTTP/1.1 200 OK</D:status>
            </D:propstat>
          </D:response>
        </D:multistatus>
        """.trimIndent()

        val results = provider.parseWebDavXml(owncloudXml, "https://owncloud.local")
        assertEquals(1, results.size)
        val track = results.first()
        assertEquals("Bohemian Rhapsody", track.title)
        assertEquals("Queen", track.artist)
        assertEquals("flac", track.format)
        assertTrue(track.isLossless)
        assertEquals(42000000L, track.sizeBytes)
    }

    @Test
    fun testWebDavXmlParserNginxDefaultNamespace() {
        val provider = WebDavProvider()
        val nginxXml = """
        <?xml version="1.0" encoding="utf-8"?>
        <multistatus xmlns="DAV:">
          <response>
            <href>/webdav/The%20Beatles%20-%20Yesterday.wav</href>
            <propstat>
              <prop>
                <getcontenttype>audio/x-wav</getcontenttype>
                <getcontentlength>32000000</getcontentlength>
                <displayname>The Beatles - Yesterday.wav</displayname>
              </prop>
            </propstat>
          </response>
        </multistatus>
        """.trimIndent()

        val results = provider.parseWebDavXml(nginxXml, "http://nginx-webdav.lan")
        assertEquals(1, results.size)
        val track = results.first()
        assertEquals("Yesterday", track.title)
        assertEquals("The Beatles", track.artist)
        assertEquals("wav", track.format)
        assertTrue(track.isLossless)
        assertEquals(32000000L, track.sizeBytes)
    }

    @Test
    fun testWebDavXmlParserXxeProtection() {
        val provider = WebDavProvider()
        // XXE payload attempting to read external entity
        val xxePayload = """
        <?xml version="1.0" encoding="utf-8"?>
        <!DOCTYPE test [
          <!ENTITY xxe SYSTEM "file:///etc/passwd">
        ]>
        <d:multistatus xmlns:d="DAV:">
          <d:response>
            <d:href>&xxe;</d:href>
            <d:propstat>
              <d:prop>
                <d:getcontenttype>audio/mpeg</d:getcontenttype>
              </d:prop>
            </d:propstat>
          </d:response>
        </d:multistatus>
        """.trimIndent()

        // With disallow-doctype-decl enabled, parsing either throws/returns emptyList without resolving external entity
        val results = provider.parseWebDavXml(xxePayload, "https://cloud.example.com")
        assertTrue(results.isEmpty(), "XXE payload must be safely rejected")
    }
}
