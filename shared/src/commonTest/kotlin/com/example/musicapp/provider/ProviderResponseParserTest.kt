package com.example.musicapp.provider

import com.example.musicapp.model.RemoteTrack
import com.example.musicapp.model.TrackSource
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Serializable
private data class JamendoTestDto(
    val results: List<JamendoTrackTestDto> = emptyList()
)

@Serializable
private data class JamendoTrackTestDto(
    val id: String,
    val name: String,
    val duration: Int = 0,
    val artist_name: String,
    val album_name: String? = null,
    val audio: String,
    val image: String? = null,
    val license_ccurl: String? = null,
)

@Serializable
private data class AudiusTestDto(
    val data: List<AudiusTrackTestDto> = emptyList()
)

@Serializable
private data class AudiusTrackTestDto(
    val id: String,
    val title: String,
    val duration: Int = 0,
    val user: AudiusUserTestDto? = null,
    val artwork: AudiusArtworkTestDto? = null,
    val genre: String? = null,
)

@Serializable
private data class AudiusUserTestDto(val name: String = "")

@Serializable
private data class AudiusArtworkTestDto(val `150x150`: String? = null, val `480x480`: String? = null)

@Serializable
private data class GDriveTestDto(
    val files: List<GDriveFileTestDto> = emptyList(),
    val nextPageToken: String? = null,
)

@Serializable
private data class GDriveFileTestDto(
    val id: String,
    val name: String,
    val size: String? = null,
    val mimeType: String? = null,
)

@Serializable
private data class YandexTestDto(
    val items: List<YandexItemTestDto> = emptyList(),
    val offset: Int = 0,
    val total: Int = 0,
)

@Serializable
private data class YandexItemTestDto(
    val path: String,
    val name: String,
    val size: Long? = null,
    val mime_type: String? = null,
    val file: String? = null,
)

class ProviderResponseParserTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun testJamendoResponseParsing() {
        val fixture = """
            {
                "headers": {"status": "success", "code": 0},
                "results": [
                    {
                        "id": "1885623",
                        "name": "Summer Breeze",
                        "duration": 215,
                        "artist_name": "Chill Out Collective",
                        "album_name": "Sunset Lounge",
                        "audio": "https://prod-1.storage.jamendo.com/download/track/1885623/mp32/",
                        "image": "https://usercontent.jamendo.com?type=album&id=450&width=300",
                        "license_ccurl": "https://creativecommons.org/licenses/by-sa/3.0/"
                    }
                ]
            }
        """.trimIndent()

        val parsed = json.decodeFromString<JamendoTestDto>(fixture)
        assertEquals(1, parsed.results.size)
        val track = parsed.results.first()
        assertEquals("1885623", track.id)
        assertEquals("Summer Breeze", track.name)
        assertEquals("Chill Out Collective", track.artist_name)
        assertEquals(215, track.duration)
        assertEquals("https://creativecommons.org/licenses/by-sa/3.0/", track.license_ccurl)
    }

    @Test
    fun testAudiusResponseParsing() {
        val fixture = """
            {
                "data": [
                    {
                        "id": "W08NKdZ",
                        "title": "Post HospitiLL Slapperzzz Mix",
                        "duration": 394,
                        "genre": "Electronic",
                        "user": {
                            "name": "iLLPeTiLL"
                        },
                        "artwork": {
                            "150x150": "https://audius.co/art150.jpg",
                            "480x480": "https://audius.co/art480.jpg"
                        }
                    }
                ]
            }
        """.trimIndent()

        val parsed = json.decodeFromString<AudiusTestDto>(fixture)
        assertEquals(1, parsed.data.size)
        val track = parsed.data.first()
        assertEquals("W08NKdZ", track.id)
        assertEquals("Post HospitiLL Slapperzzz Mix", track.title)
        assertEquals("iLLPeTiLL", track.user?.name)
        assertEquals(394, track.duration)
        assertEquals("https://audius.co/art480.jpg", track.artwork?.`480x480`)
    }

    @Test
    fun testGoogleDriveResponseParsing() {
        val fixture = """
            {
                "nextPageToken": "token_page_2",
                "files": [
                    {
                        "id": "1gdrive_abc123",
                        "name": "Pink Floyd - Comfortably Numb.mp3",
                        "size": "15890214",
                        "mimeType": "audio/mpeg"
                    }
                ]
            }
        """.trimIndent()

        val parsed = json.decodeFromString<GDriveTestDto>(fixture)
        assertEquals("token_page_2", parsed.nextPageToken)
        assertEquals(1, parsed.files.size)
        val file = parsed.files.first()
        assertEquals("1gdrive_abc123", file.id)
        assertEquals("Pink Floyd - Comfortably Numb.mp3", file.name)
        assertEquals("15890214", file.size)
        assertEquals("audio/mpeg", file.mimeType)
    }

    @Test
    fun testYandexDiskResponseParsing() {
        val fixture = """
            {
                "total": 42,
                "offset": 0,
                "items": [
                    {
                        "path": "disk:/Music/Queen - Bohemian Rhapsody.mp3",
                        "name": "Queen - Bohemian Rhapsody.mp3",
                        "size": 12048590,
                        "mime_type": "audio/mpeg",
                        "file": "https://downloader.disk.yandex.ru/disk/12345"
                    }
                ]
            }
        """.trimIndent()

        val parsed = json.decodeFromString<YandexTestDto>(fixture)
        assertEquals(42, parsed.total)
        assertEquals(1, parsed.items.size)
        val item = parsed.items.first()
        assertEquals("Queen - Bohemian Rhapsody.mp3", item.name)
        assertEquals(12048590L, item.size)
        assertNotNull(item.file)
    }
}
