package com.example.musicapp.provider

import com.example.musicapp.model.Page
import com.example.musicapp.model.RemoteTrack
import com.example.musicapp.model.StreamSource
import com.example.musicapp.model.TrackSource
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

@Serializable
private data class YandexFilesResponse(
    val items: List<YandexItemDto> = emptyList(),
    val offset: Int = 0,
    val total: Int = 0,
)

@Serializable
private data class YandexItemDto(
    val path: String,
    val name: String,
    val size: Long? = null,
    val mime_type: String? = null,
    val file: String? = null,
)

@Serializable
private data class YandexDownloadResponse(
    val href: String,
)

class YandexDiskProvider(
    private var token: String? = null,
    private val onTokenRefreshNeeded: (suspend () -> String?)? = null,
) : MusicProvider {
    override val id: String = "yandex"
    override val displayName: String = "Яндекс Диск"
    override val source: TrackSource = TrackSource.YANDEX_DISK
    override val iconName: String = "CloudDone"

    private val _isSignedIn = MutableStateFlow(!token.isNullOrBlank())
    override val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    fun updateToken(newToken: String?) {
        token = newToken
        _isSignedIn.value = !newToken.isNullOrBlank()
    }

    override suspend fun signIn() {
        val refreshed = onTokenRefreshNeeded?.invoke()
        if (!refreshed.isNullOrBlank()) {
            updateToken(refreshed)
        }
    }

    override suspend fun signOut() {
        updateToken(null)
    }

    override suspend fun listTracks(folderId: String?, pageToken: String?): Page<RemoteTrack> {
        val oauth = token ?: return Page(emptyList())
        return try {
            val offset = pageToken?.toIntOrNull() ?: 0
            val url = "https://cloud-api.yandex.net/v1/disk/resources/files?media_type=audio&limit=50&offset=$offset"
            val response = appHttpClient.get(url) {
                header("Authorization", "OAuth $oauth")
            }.body<YandexFilesResponse>()

            val tracks = response.items.map { item ->
                val cleanTitle = item.name.substringBeforeLast(".")
                val parts = cleanTitle.split(" - ", limit = 2)
                val artist = if (parts.size == 2) parts[0].trim() else "Яндекс Диск"
                val title = if (parts.size == 2) parts[1].trim() else cleanTitle

                RemoteTrack(
                    id = "yandex_${item.path.hashCode()}",
                    title = title,
                    artist = artist,
                    album = "Яндекс Диск",
                    durationMs = 0L,
                    streamUrl = item.file,
                    sizeBytes = item.size,
                    source = TrackSource.YANDEX_DISK,
                    mimeType = item.mime_type ?: "audio/mpeg"
                )
            }
            val nextToken = if (offset + 50 < response.total) (offset + 50).toString() else null
            Page(items = tracks, nextPageToken = nextToken)
        } catch (e: Exception) {
            Page(emptyList())
        }
    }

    override suspend fun search(query: String): List<RemoteTrack> {
        val page = listTracks()
        val q = query.lowercase().trim()
        return page.items.filter { it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) }
    }

    override suspend fun getStream(track: RemoteTrack): StreamSource {
        val directUrl = track.streamUrl
        val headers = mutableMapOf<String, String>()
        token?.let { headers["Authorization"] = "OAuth $it" }
        return StreamSource(url = directUrl ?: "", headers = headers, supportsRange = true)
    }
}
