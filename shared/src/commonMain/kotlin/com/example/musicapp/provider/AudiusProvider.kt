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
private data class AudiusResponse(
    val data: List<AudiusTrackDto> = emptyList()
)

@Serializable
private data class AudiusTrackDto(
    val id: String,
    val title: String,
    val duration: Int = 0,
    val user: AudiusUserDto? = null,
    val artwork: AudiusArtworkDto? = null,
    val genre: String? = null,
)

@Serializable
private data class AudiusUserDto(
    val name: String = "",
)

@Serializable
private data class AudiusArtworkDto(
    val `150x150`: String? = null,
    val `480x480`: String? = null,
)

class AudiusProvider : MusicProvider {
    override val id: String = "audius"
    override val displayName: String = "Audius Open Music"
    override val source: TrackSource = TrackSource.AUDIUS
    override val iconName: String = "Audiotrack"
    override val isPublicCatalog: Boolean = true

    private val _isSignedIn = MutableStateFlow(true)
    override val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    private val host = "https://discoveryprovider.audius.co/v1"
    private val appName = "JVXMusic"

    override suspend fun signIn() {
        _isSignedIn.value = true
    }

    override suspend fun signOut() {}

    override suspend fun listTracks(folderId: String?, pageToken: String?): Page<RemoteTrack> {
        return try {
            val url = "$host/tracks/trending?app_name=$appName&limit=30"
            val response = appHttpClient.get(url).body<AudiusResponse>()
            val tracks = response.data.map { dto ->
                val art = dto.artwork?.`480x480` ?: dto.artwork?.`150x150`
                RemoteTrack(
                    id = "audius_${dto.id}",
                    title = dto.title,
                    artist = dto.user?.name ?: "Unknown Artist",
                    album = dto.genre ?: "Audius",
                    durationMs = dto.duration * 1000L,
                    streamUrl = "$host/tracks/${dto.id}/stream?app_name=$appName",
                    artworkUrl = art,
                    license = "Open Audio Stream",
                    source = TrackSource.AUDIUS,
                    mimeType = "audio/mpeg"
                )
            }
            Page(items = tracks)
        } catch (e: Exception) {
            Page(items = emptyList())
        }
    }

    override suspend fun search(query: String): List<RemoteTrack> {
        return try {
            val encoded = query.trim().replace(" ", "+")
            val url = "$host/tracks/search?query=$encoded&app_name=$appName&limit=25"
            val response = appHttpClient.get(url).body<AudiusResponse>()
            response.data.map { dto ->
                val art = dto.artwork?.`480x480` ?: dto.artwork?.`150x150`
                RemoteTrack(
                    id = "audius_${dto.id}",
                    title = dto.title,
                    artist = dto.user?.name ?: "Unknown Artist",
                    album = dto.genre ?: "Audius",
                    durationMs = dto.duration * 1000L,
                    streamUrl = "$host/tracks/${dto.id}/stream?app_name=$appName",
                    artworkUrl = art,
                    license = "Open Audio Stream",
                    source = TrackSource.AUDIUS,
                    mimeType = "audio/mpeg"
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getStream(track: RemoteTrack): StreamSource {
        val rawId = track.id.removePrefix("audius_")
        val streamUrl = "$host/tracks/$rawId/stream?app_name=$appName"
        return StreamSource(url = streamUrl, supportsRange = true)
    }
}
