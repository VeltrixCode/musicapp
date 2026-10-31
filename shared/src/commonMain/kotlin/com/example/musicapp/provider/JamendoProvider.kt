package com.example.musicapp.provider

import com.example.musicapp.model.Page
import com.example.musicapp.model.RemoteTrack
import com.example.musicapp.model.StreamSource
import com.example.musicapp.model.TrackSource
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

@Serializable
private data class JamendoResponse(
    val results: List<JamendoTrackDto> = emptyList()
)

@Serializable
private data class JamendoTrackDto(
    val id: String,
    val name: String,
    val duration: Int = 0,
    val artist_name: String,
    val album_name: String? = null,
    val audio: String,
    val image: String? = null,
    val license_ccurl: String? = null,
)

class JamendoProvider : MusicProvider {
    override val id: String = "jamendo"
    override val displayName: String = "Jamendo Music"
    override val source: TrackSource = TrackSource.JAMENDO
    override val iconName: String = "Explore"
    override val isPublicCatalog: Boolean = true

    private val _isSignedIn = MutableStateFlow(true)
    override val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    private val clientId = "56d30c95"

    override suspend fun signIn() {
        _isSignedIn.value = true
    }

    override suspend fun signOut() {
        // Public API
    }

    override suspend fun listTracks(folderId: String?, pageToken: String?): Page<RemoteTrack> {
        return try {
            val offset = pageToken?.toIntOrNull() ?: 0
            val url = "https://api.jamendo.com/v3.0/tracks/?client_id=$clientId&format=json&limit=30&offset=$offset&order=popularity_total&audioformat=mp32"
            val response = appHttpClient.get(url).body<JamendoResponse>()
            val tracks = response.results.map { dto ->
                RemoteTrack(
                    id = "jamendo_${dto.id}",
                    title = dto.name,
                    artist = dto.artist_name,
                    album = dto.album_name ?: "Jamendo",
                    durationMs = dto.duration * 1000L,
                    streamUrl = dto.audio,
                    artworkUrl = dto.image,
                    license = dto.license_ccurl ?: "Creative Commons",
                    source = TrackSource.JAMENDO,
                    mimeType = "audio/mpeg"
                )
            }
            Page(items = tracks, nextPageToken = (offset + 30).toString())
        } catch (e: Exception) {
            Page(items = emptyList())
        }
    }

    override suspend fun search(query: String): List<RemoteTrack> {
        return try {
            val encoded = query.trim().replace(" ", "+")
            val url = "https://api.jamendo.com/v3.0/tracks/?client_id=$clientId&format=json&limit=25&namesearch=$encoded&audioformat=mp32"
            val response = appHttpClient.get(url).body<JamendoResponse>()
            response.results.map { dto ->
                RemoteTrack(
                    id = "jamendo_${dto.id}",
                    title = dto.name,
                    artist = dto.artist_name,
                    album = dto.album_name ?: "Jamendo",
                    durationMs = dto.duration * 1000L,
                    streamUrl = dto.audio,
                    artworkUrl = dto.image,
                    license = dto.license_ccurl ?: "Creative Commons",
                    source = TrackSource.JAMENDO,
                    mimeType = "audio/mpeg"
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getStream(track: RemoteTrack): StreamSource {
        val url = track.streamUrl ?: "https://api.jamendo.com/v3.0/tracks/file/?client_id=$clientId&id=${track.id.removePrefix("jamendo_")}&action=stream"
        return StreamSource(url = url, supportsRange = true)
    }
}
