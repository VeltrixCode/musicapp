package com.example.musicapp.provider

import com.example.musicapp.config.AppConfig
import com.example.musicapp.model.AppError
import com.example.musicapp.model.Page
import com.example.musicapp.model.RemoteTrack
import com.example.musicapp.model.StreamSource
import com.example.musicapp.model.TrackSource
import com.example.musicapp.settings.SettingsRepository
import com.example.musicapp.util.CryptoUtils
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WebDavProvider(
    private val client: HttpClient = appHttpClient,
    private val settingsRepo: SettingsRepository = SettingsRepository(),
) : MusicProvider {

    override val id: String = "webdav"
    override val displayName: String = "WebDAV / Nextcloud"
    override val source: TrackSource = TrackSource.WEBDAV
    override val iconName: String = "FolderShared"
    override val isPublicCatalog: Boolean = false

    private val _isSignedIn = MutableStateFlow(
        AppConfig.webdavServer.isNotBlank() && AppConfig.webdavUser.isNotBlank()
    )
    override val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    private fun getBaseUrl(): String {
        var base = AppConfig.webdavServer.trim()
        if (base.isBlank()) {
            throw AppError.ProviderError("Сервер WebDAV не настроен")
        }
        if (!base.startsWith("http://") && !base.startsWith("https://")) {
            base = "https://$base"
        }
        return base.trimEnd('/')
    }

    private fun getAuthHeader(): String {
        val user = AppConfig.webdavUser.trim()
        val pass = AppConfig.webdavPassword
        val credentials = "$user:$pass"
        return "Basic " + CryptoUtils.base64Encode(credentials.encodeToByteArray())
    }

    private fun getRootPath(): String {
        val p = AppConfig.webdavRootPath.trim()
        return if (p.startsWith("/")) p else "/$p"
    }

    override suspend fun signIn() {
        if (settingsRepo.load().offlineMode) {
            throw AppError.OfflineModeError()
        }
        val baseUrl = getBaseUrl()
        val path = getRootPath()
        val fullUrl = "$baseUrl$path"

        try {
            val response = client.request(fullUrl) {
                method = HttpMethod("PROPFIND")
                header(HttpHeaders.Authorization, getAuthHeader())
                header("Depth", "0")
                header(HttpHeaders.UserAgent, "JVXMusic/1.0 (WebDAV Client)")
            }

            val status = response.status.value
            if (status in 200..299 || status == 207) {
                _isSignedIn.value = true
            } else if (status == 401 || status == 403) {
                _isSignedIn.value = false
                throw AppError.AuthError("Ошибка авторизации WebDAV (код $status)")
            } else {
                _isSignedIn.value = false
                throw AppError.ProviderError("Сервер WebDAV вернул код $status")
            }
        } catch (e: Exception) {
            _isSignedIn.value = false
            if (e is AppError) throw e
            throw AppError.NetworkError("Не удалось подключиться к WebDAV: ${e.message}", e)
        }
    }

    override suspend fun signOut() {
        AppConfig.webdavServer = ""
        AppConfig.webdavUser = ""
        AppConfig.webdavPassword = ""
        AppConfig.webdavRootPath = ""
        _isSignedIn.value = false
    }

    override suspend fun listTracks(folderId: String?, pageToken: String?): Page<RemoteTrack> {
        if (settingsRepo.load().offlineMode) {
            throw AppError.OfflineModeError()
        }
        val baseUrl = getBaseUrl()
        val targetPath = folderId ?: getRootPath()
        val fullUrl = if (targetPath.startsWith("http://") || targetPath.startsWith("https://")) {
            targetPath
        } else {
            "$baseUrl$targetPath"
        }

        return try {
            val responseText: String = client.request(fullUrl) {
                method = HttpMethod("PROPFIND")
                header(HttpHeaders.Authorization, getAuthHeader())
                header("Depth", "1")
                header(HttpHeaders.UserAgent, "JVXMusic/1.0 (WebDAV Client)")
            }.body()

            val tracks = parseWebDavXml(responseText, baseUrl)
            Page(items = tracks)
        } catch (e: Exception) {
            if (e is AppError) throw e
            throw AppError.NetworkError("Ошибка при чтении WebDAV каталога: ${e.message}", e)
        }
    }

    override suspend fun search(query: String): List<RemoteTrack> {
        val all = listTracks().items
        val q = query.trim().lowercase()
        if (q.isBlank()) return all
        return all.filter {
            it.title.lowercase().contains(q) ||
            it.artist.lowercase().contains(q) ||
            (it.album?.lowercase()?.contains(q) == true)
        }
    }

    override suspend fun getStream(track: RemoteTrack): StreamSource {
        val streamUrl = track.streamUrl ?: track.id
        return StreamSource(
            url = streamUrl,
            headers = mapOf(HttpHeaders.Authorization to getAuthHeader()),
            supportsRange = true
        )
    }

    fun parseWebDavXml(xml: String, baseUrl: String): List<RemoteTrack> {
        val responseRegex = Regex("<(?:[a-zA-Z0-9]+:)?response[\\s>](.*?)</(?:[a-zA-Z0-9]+:)?response>", RegexOption.DOT_MATCHES_ALL)
        val hrefRegex = Regex("<(?:[a-zA-Z0-9]+:)?href>(.*?)</(?:[a-zA-Z0-9]+:)?href>")
        val contentTypeRegex = Regex("<(?:[a-zA-Z0-9]+:)?getcontenttype>(.*?)</(?:[a-zA-Z0-9]+:)?getcontenttype>")
        val contentLengthRegex = Regex("<(?:[a-zA-Z0-9]+:)?getcontentlength>(.*?)</(?:[a-zA-Z0-9]+:)?getcontentlength>")
        val displayNameRegex = Regex("<(?:[a-zA-Z0-9]+:)?displayname>(.*?)</(?:[a-zA-Z0-9]+:)?displayname>")

        val supportedAudioExtensions = setOf("mp3", "flac", "ogg", "m4a", "wav", "opus", "aac")
        val results = mutableListOf<RemoteTrack>()

        for (match in responseRegex.findAll(xml)) {
            val block = match.groupValues[1]
            val href = hrefRegex.find(block)?.groupValues?.get(1)?.trim() ?: continue
            val contentType = contentTypeRegex.find(block)?.groupValues?.get(1)?.trim() ?: ""
            val contentLength = contentLengthRegex.find(block)?.groupValues?.get(1)?.trim()?.toLongOrNull()
            val displayName = displayNameRegex.find(block)?.groupValues?.get(1)?.trim()

            val fileName = displayName?.ifBlank { null }
                ?: href.trimEnd('/').substringAfterLast('/')

            val ext = fileName.substringAfterLast('.', "").lowercase()
            val isAudio = contentType.startsWith("audio/") || ext in supportedAudioExtensions
            if (!isAudio) continue

            // Parse artist - title from filename if possible
            val rawName = fileName.substringBeforeLast('.')
            val parts = rawName.split(" - ")
            val (artist, title) = if (parts.size >= 2) {
                parts[0].trim() to parts.drop(1).joinToString(" - ").trim()
            } else {
                "WebDAV" to rawName.trim()
            }

            val fullStreamUrl = if (href.startsWith("http://") || href.startsWith("https://")) {
                href
            } else {
                baseUrl.trimEnd('/') + "/" + href.trimStart('/')
            }

            val isLossless = ext in listOf("flac", "wav", "alac")

            results.add(
                RemoteTrack(
                    id = fullStreamUrl,
                    title = title,
                    artist = artist,
                    album = "WebDAV",
                    durationMs = 0L,
                    streamUrl = fullStreamUrl,
                    artworkUrl = null,
                    license = "Личное облако WebDAV",
                    sizeBytes = contentLength,
                    source = TrackSource.WEBDAV,
                    mimeType = contentType.ifBlank { "audio/$ext" },
                    format = ext,
                    isLossless = isLossless,
                    webPageUrl = fullStreamUrl
                )
            )
        }

        return results
    }
}
