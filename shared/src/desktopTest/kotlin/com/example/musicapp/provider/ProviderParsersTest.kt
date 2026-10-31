package com.example.musicapp.provider

import com.example.musicapp.model.TrackSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ProviderParsersTest {

    @Test
    fun testInternetArchiveParser() {
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
    }

    @Test
    fun testSubsonicSongParser() {
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

        val results = provider.parseSubsonicSongs(jsonFixture, "https://navidrome.local", "u=test&t=token&s=salt")
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
    fun testWebDavXmlParser() {
        val provider = WebDavProvider()
        val xmlFixture = """
        <?xml version="1.0" encoding="utf-8"?>
        <d:multistatus xmlns:d="DAV:">
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

        val results = provider.parseWebDavXml(xmlFixture, "https://cloud.example.com")
        assertEquals(1, results.size, "Non-audio files like txt must be skipped")
        val track = results.first()
        assertEquals("One More Time", track.title)
        assertEquals("Daft Punk", track.artist)
        assertEquals("mp3", track.format)
        assertEquals(8500000L, track.sizeBytes)
        assertEquals(TrackSource.WEBDAV, track.source)
        assertTrue(track.streamUrl!!.contains("/Music/Daft%20Punk%20-%20One%20More%20Time.mp3"))
    }
}
