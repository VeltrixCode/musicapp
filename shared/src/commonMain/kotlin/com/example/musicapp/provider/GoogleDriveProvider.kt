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
private data class GDriveFileList(
    val files: List<GDriveFile> = emptyList(),
    val nextPageToken: String? = null,
)

@Serializable
private data class GDriveFile(
    val id: String,
    val name: String,
    val size: String? = null,
    val mimeType: String? = null,
)

class GoogleDriveProvider(
    private var accessToken: String? = null,
    private val onTokenRefreshNeeded: (suspend () -> String?)? = null,
) : MusicProvider {
    override val id: String = "gdrive"
    override val displayName: String = "Google Диск"
    override val source: TrackSource = TrackSource.GOOGLE_DRIVE
    override val iconName: String = "CloudQueue"

    private val _isSignedIn = MutableStateFlow(!accessToken.isNullOrBlank())
    override val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    fun updateToken(token: String?) {
        accessToken = token
        _isSignedIn.value = !token.isNullOrBlank()
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
        val token = accessToken ?: return Page(emptyList())
        return try {
            val query = StringBuilder("mimeType contains 'audio/' and trashed = false")
            if (!folderId.isNullOrBlank()) {
                query.append(" and '$folderId' in parents")
            }
            val encodedQuery = query.toString().replace(" ", "%20").replace("'", "%27")
            val tokenParam = if (pageToken != null) "&pageToken=$pageToken" else ""
            val url = "https://www.googleapis.com/drive/v3/files?q=$encodedQuery&fields=nextPageToken,files(id,name,size,mimeType)&pageSize=50$tokenParam"

            val response = appHttpClient.get(url) {
                header("Authorization", "Bearer $token")
            }.body<GDriveFileList>()

            val tracks = response.files.map { f ->
                val cleanTitle = f.name.substringBeforeLast(".")
                val parts = cleanTitle.split(" - ", limit = 2)
                val artist = if (parts.size == 2) parts[0].trim() else "Google Диск"
                val title = if (parts.size == 2) parts[1].trim() else cleanTitle

                RemoteTrack(
                    id = "gdrive_${f.id}",
                    title = title,
                    artist = artist,
                    album = "Google Диск",
                    durationMs = 0L,
                    streamUrl = "https://www.googleapis.com/drive/v3/files/${f.id}?alt=media",
                    sizeBytes = f.size?.toLongOrNull(),
                    source = TrackSource.GOOGLE_DRIVE,
                    mimeType = f.mimeType ?: "audio/mpeg"
                )
            }
            Page(items = tracks, nextPageToken = response.nextPageToken)
        } catch (e: Exception) {
            Page(emptyList())
        }
    }

    override suspend fun search(query: String): List<RemoteTrack> {
        val token = accessToken ?: return emptyList()
        return try {
            val q = "mimeType contains 'audio/' and trashed = false and name contains '$query'"
                .replace(" ", "%20").replace("'", "%27")
            val url = "https://www.googleapis.com/drive/v3/files?q=$q&fields=files(id,name,size,mimeType)&pageSize=30"

            val response = appHttpClient.get(url) {
                header("Authorization", "Bearer $token")
            }.body<GDriveFileList>()

            response.files.map { f ->
                val cleanTitle = f.name.substringBeforeLast(".")
                val parts = cleanTitle.split(" - ", limit = 2)
                val artist = if (parts.size == 2) parts[0].trim() else "Google Диск"
                val title = if (parts.size == 2) parts[1].trim() else cleanTitle

                RemoteTrack(
                    id = "gdrive_${f.id}",
                    title = title,
                    artist = artist,
                    album = "Google Диск",
                    durationMs = 0L,
                    streamUrl = "https://www.googleapis.com/drive/v3/files/${f.id}?alt=media",
                    sizeBytes = f.size?.toLongOrNull(),
                    source = TrackSource.GOOGLE_DRIVE,
                    mimeType = f.mimeType ?: "audio/mpeg"
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getStream(track: RemoteTrack): StreamSource {
        val rawId = track.id.removePrefix("gdrive_")
        val url = "https://www.googleapis.com/drive/v3/files/$rawId?alt=media"
        val headers = mutableMapOf<String, String>()
        accessToken?.let { headers["Authorization"] = "Bearer $it" }
        return StreamSource(url = url, headers = headers, supportsRange = true)
    }
}
