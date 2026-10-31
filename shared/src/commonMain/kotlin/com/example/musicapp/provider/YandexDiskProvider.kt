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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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
    private var token: String? = SecureStorage.getSecret(AppConfig.TOKEN_YANDEX_ACCESS),
    private val onTokenRefreshNeeded: (suspend () -> String?)? = null,
) : MusicProvider {
    override val id: String = "yandex"
    override val displayName: String = "Яндекс Диск"
    override val source: TrackSource = TrackSource.YANDEX_DISK
    override val iconName: String = "CloudDone"

    private val _isSignedIn = MutableStateFlow(!token.isNullOrBlank())
    override val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    // Map track ID to Yandex disk path for renewing download links
    private val trackPaths = mutableMapOf<String, String>()

    fun updateToken(newToken: String?) {
        token = newToken
        if (!newToken.isNullOrBlank()) {
            SecureStorage.saveSecret(AppConfig.TOKEN_YANDEX_ACCESS, newToken)
        } else {
            SecureStorage.removeSecret(AppConfig.TOKEN_YANDEX_ACCESS)
        }
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
                val trackId = "yandex_${item.path.hashCode().toUInt()}"
                trackPaths[trackId] = item.path

                RemoteTrack(
                    id = trackId,
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

    suspend fun fetchAccountEmail(): String? {
        val oauth = token ?: return null
        return runCatching {
            val url = "https://cloud-api.yandex.net/v1/disk/"
            val jsonText = appHttpClient.get(url) {
                header("Authorization", "OAuth $oauth")
            }.body<String>()
            val json = Json { ignoreUnknownKeys = true }
            val root = json.parseToJsonElement(jsonText).jsonObject
            val user = root["user"]?.jsonObject
            user?.get("display_name")?.jsonPrimitive?.content
                ?: user?.get("login")?.jsonPrimitive?.content
        }.getOrNull()
    }

    suspend fun listFolders(): List<Pair<String, String>> {
        val oauth = token ?: return emptyList()
        return runCatching {
            val url = "https://cloud-api.yandex.net/v1/disk/resources?path=/&fields=_embedded.items.name,_embedded.items.type,_embedded.items.path&limit=100"
            val jsonText = appHttpClient.get(url) {
                header("Authorization", "OAuth $oauth")
            }.body<String>()
            val json = Json { ignoreUnknownKeys = true }
            val root = json.parseToJsonElement(jsonText).jsonObject
            val embedded = root["_embedded"]?.jsonObject
            val items = embedded?.get("items")?.jsonArray ?: return emptyList()
            val result = mutableListOf<Pair<String, String>>()
            for (item in items) {
                val obj = item.jsonObject
                val type = obj["type"]?.jsonPrimitive?.content
                if (type == "dir") {
                    val path = obj["path"]?.jsonPrimitive?.content ?: continue
                    val name = obj["name"]?.jsonPrimitive?.content ?: path
                    result.add(path to name)
                }
            }
            result
        }.getOrDefault(emptyList())
    }

    override suspend fun search(query: String): List<RemoteTrack> {
        val page = listTracks()
        val q = query.lowercase().trim()
        return page.items.filter { it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) }
    }

    /**
     * Obtains a fresh direct download/streaming link from Yandex Disk API.
     * Essential for handling expired links (403/410) seamlessly during playback.
     */
    suspend fun fetchFreshDownloadLink(path: String): String? {
        val oauth = token ?: return null
        return try {
            val encodedPath = path.replace(" ", "%20")
            val url = "https://cloud-api.yandex.net/v1/disk/resources/download?path=$encodedPath"
            val response = appHttpClient.get(url) {
                header("Authorization", "OAuth $oauth")
            }.body<YandexDownloadResponse>()
            response.href
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getStream(track: RemoteTrack): StreamSource {
        val oauth = token
        val originalPath = trackPaths[track.id]
        var streamUrl = track.streamUrl

        if (streamUrl.isNullOrBlank() && originalPath != null) {
            streamUrl = fetchFreshDownloadLink(originalPath)
        }

        val headers = mutableMapOf<String, String>()
        if (!oauth.isNullOrBlank()) {
            headers["Authorization"] = "OAuth $oauth"
        }

        return StreamSource(
            url = streamUrl ?: "",
            headers = headers,
            supportsRange = true
        )
    }

    suspend fun getStreamWithAutoRefresh(track: RemoteTrack): Pair<StreamSource, (suspend () -> StreamSource?)?> {
        val originalStream = getStream(track)
        val path = trackPaths[track.id]
        val refreshCallback: (suspend () -> StreamSource?)? = if (path != null) {
            {
                val freshHref = fetchFreshDownloadLink(path)
                if (freshHref != null) {
                    StreamSource(url = freshHref, headers = originalStream.headers, supportsRange = true)
                } else null
            }
        } else null

        return originalStream to refreshCallback
    }
}
