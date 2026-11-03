package com.example.musicapp.provider

import com.example.musicapp.config.AppConfig
import com.example.musicapp.model.Page
import com.example.musicapp.model.RemoteTrack
import com.example.musicapp.model.StreamSource
import com.example.musicapp.model.TrackSource
import com.example.musicapp.security.SecureStorage
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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
    private var accessToken: String? = SecureStorage.getSecret(AppConfig.TOKEN_GDRIVE_ACCESS),
    var syncFolderId: String? = null,
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
        if (!token.isNullOrBlank()) {
            SecureStorage.saveSecret(AppConfig.TOKEN_GDRIVE_ACCESS, token)
        } else {
            SecureStorage.removeSecret(AppConfig.TOKEN_GDRIVE_ACCESS)
            SecureStorage.removeSecret(AppConfig.TOKEN_GDRIVE_REFRESH)
        }
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
        val effectiveFolderId = folderId ?: syncFolderId
        return try {
            val query = StringBuilder("mimeType contains 'audio/' and trashed = false")
            if (!effectiveFolderId.isNullOrBlank()) {
                query.append(" and '$effectiveFolderId' in parents")
            }

            val response = appHttpClient.get("https://www.googleapis.com/drive/v3/files") {
                header("Authorization", "Bearer $token")
                parameter("q", query.toString())
                parameter("fields", "nextPageToken,files(id,name,size,mimeType)")
                parameter("pageSize", 50)
                if (!pageToken.isNullOrBlank()) {
                    parameter("pageToken", pageToken)
                }
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

    suspend fun fetchAccountEmail(): String? {
        val t = accessToken ?: return null
        return runCatching {
            val jsonText = appHttpClient.get("https://www.googleapis.com/drive/v3/about") {
                header("Authorization", "Bearer $t")
                parameter("fields", "user(displayName,emailAddress)")
            }.body<String>()
            val json = Json { ignoreUnknownKeys = true }
            val root = json.parseToJsonElement(jsonText).jsonObject
            val user = root["user"]?.jsonObject
            user?.get("emailAddress")?.jsonPrimitive?.content
                ?: user?.get("displayName")?.jsonPrimitive?.content
        }.getOrNull()
    }

    suspend fun listFolders(): List<Pair<String, String>> {
        val t = accessToken ?: return emptyList()
        return runCatching {
            val response = appHttpClient.get("https://www.googleapis.com/drive/v3/files") {
                header("Authorization", "Bearer $t")
                parameter("q", "mimeType='application/vnd.google-apps.folder' and trashed=false")
                parameter("fields", "files(id,name)")
                parameter("pageSize", 100)
            }.body<GDriveFileList>()
            response.files.map { it.id to it.name }
        }.getOrDefault(emptyList())
    }

    override suspend fun search(query: String): List<RemoteTrack> {
        val token = accessToken ?: return emptyList()
        val effectiveFolderId = syncFolderId
        return try {
            val q = StringBuilder("mimeType contains 'audio/' and trashed = false and name contains '$query'")
            if (!effectiveFolderId.isNullOrBlank()) {
                q.append(" and '$effectiveFolderId' in parents")
            }

            val response = appHttpClient.get("https://www.googleapis.com/drive/v3/files") {
                header("Authorization", "Bearer $token")
                parameter("q", q.toString())
                parameter("fields", "files(id,name,size,mimeType)")
                parameter("pageSize", 30)
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
        return StreamSource(
            url = url,
            headers = headers,
            supportsRange = true,
            contentLength = track.sizeBytes,
            contentType = track.mimeType ?: "audio/mpeg"
        )
    }
}
