package com.example.musicapp.provider

import androidx.compose.ui.graphics.ImageBitmap
import com.example.musicapp.cache.ArtworkCache
import com.example.musicapp.config.AppConfig
import com.example.musicapp.model.AppError
import com.example.musicapp.model.Page
import com.example.musicapp.model.RemoteTrack
import com.example.musicapp.model.StreamSource
import com.example.musicapp.model.Track
import com.example.musicapp.model.TrackSource
import com.example.musicapp.platform.AppStorage
import com.example.musicapp.platform.decodeImageBytes
import com.example.musicapp.settings.SettingsRepository
import com.example.musicapp.util.CryptoUtils
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.contentLength
import io.ktor.http.encodeURLQueryComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

class SubsonicProvider(
    private val client: HttpClient = appHttpClient,
    private val settingsRepo: SettingsRepository = SettingsRepository(),
) : MusicProvider {

    override val id: String = "subsonic"
    override val displayName: String = "Subsonic / Navidrome"
    override val source: TrackSource = TrackSource.SUBSONIC
    override val iconName: String = "Dns"
    override val isPublicCatalog: Boolean = false

    private val _isSignedIn = MutableStateFlow(
        AppConfig.subsonicServer.isNotBlank() && AppConfig.subsonicUser.isNotBlank()
    )
    override val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        const val MAX_COVER_SIZE_BYTES = 5L * 1024L * 1024L // 5 MB

        /**
         * Sanitizes URLs and exception messages so credentials (token `t=`, salt `s=`, password `p=`)
         * are masked and NEVER leak into logs, exceptions, or error messages.
         */
        fun sanitizeSubsonicUrl(urlOrMessage: String?): String {
            if (urlOrMessage == null) return ""
            return urlOrMessage
                .replace(Regex("""\b(t|s|p|password|token|salt)=[^&\s"'<>]+""", RegexOption.IGNORE_CASE)) { match ->
                    val param = match.value.substringBefore('=')
                    "$param=***"
                }
        }

        /**
         * Checks if a server host or URL belongs to Local Area Network (LAN) / private ranges.
         */
        fun isLanAddress(urlOrHost: String): Boolean {
            val clean = urlOrHost.trim()
                .removePrefix("http://")
                .removePrefix("https://")
                .substringBefore('/')
                .substringBefore(':')
                .lowercase()
            if (clean.isBlank()) return false
            if (clean == "localhost" || clean == "127.0.0.1" || clean == "::1") return true
            if (clean.endsWith(".local") || clean.endsWith(".lan") || clean.endsWith(".home") || clean.endsWith(".internal")) return true

            val parts = clean.split('.')
            if (parts.size == 4 && parts.all { it.toIntOrNull() in 0..255 }) {
                val p0 = parts[0].toInt()
                val p1 = parts[1].toInt()
                if (p0 == 10) return true
                if (p0 == 192 && p1 == 168) return true
                if (p0 == 172 && p1 in 16..31) return true
                if (p0 == 127) return true
                if (p0 == 169 && p1 == 254) return true // link-local
            }
            return false
        }

        /**
         * Returns a warning if plain HTTP is used outside a local area network (LAN).
         */
        fun checkPlainHttpWarning(serverUrl: String): String? {
            val trimmed = serverUrl.trim()
            if (trimmed.startsWith("http://", ignoreCase = true) && !isLanAddress(trimmed)) {
                return "Внимание: передача учётных данных через незащищённое соединение (HTTP) за пределами локальной сети (LAN) небезопасна. Рекомендуется использовать HTTPS."
            }
            return null
        }
    }

    private fun sanitizeAppError(e: Throwable): AppError {
        val sanitizedMsg = sanitizeSubsonicUrl(e.message)
        return when (e) {
            is AppError.AuthError -> AppError.AuthError(sanitizedMsg)
            is AppError.NetworkError -> AppError.NetworkError(sanitizedMsg)
            is AppError.ProviderError -> AppError.ProviderError(sanitizedMsg)
            else -> AppError.NetworkError(sanitizedMsg)
        }
    }

    fun buildAuthParams(): String {
        val user = AppConfig.subsonicUser.trim()
        val password = AppConfig.subsonicPassword
        if (user.isBlank() || password.isBlank()) {
            throw AppError.AuthError("Учётные данные Subsonic не настроены")
        }
        val salt = "jvx" + (1000..9999).random()
        val token = CryptoUtils.md5Hex(password + salt)
        return "u=${user.encodeURLQueryComponent()}&t=$token&s=$salt&v=1.16.1&c=JVXMusic&f=json"
    }

    fun getBaseUrl(): String {
        var base = AppConfig.subsonicServer.trim()
        if (base.isBlank()) {
            throw AppError.ProviderError("Адрес сервера Subsonic не указан")
        }
        if (!base.startsWith("http://") && !base.startsWith("https://")) {
            base = "https://$base"
        }
        return base.trimEnd('/')
    }

    override suspend fun signIn() {
        if (settingsRepo.load().offlineMode) {
            throw AppError.OfflineModeError()
        }
        val base = getBaseUrl()
        val auth = buildAuthParams()
        val pingUrl = "$base/rest/ping.view?$auth"

        try {
            val responseText: String = client.get(pingUrl) {
                header(HttpHeaders.UserAgent, "JVXMusic/1.0")
            }.body()

            val root = json.parseToJsonElement(responseText).jsonObject
            val subResponse = root["subsonic-response"]?.jsonObject
                ?: throw AppError.ProviderError("Некорректный ответ Subsonic")

            val status = subResponse["status"]?.jsonPrimitive?.content
            if (status == "ok") {
                _isSignedIn.value = true
            } else {
                val error = subResponse["error"]?.jsonObject
                val msg = error?.get("message")?.jsonPrimitive?.content ?: "Ошибка аутентификации Subsonic"
                _isSignedIn.value = false
                throw AppError.AuthError(sanitizeSubsonicUrl(msg))
            }
        } catch (e: Exception) {
            _isSignedIn.value = false
            if (e is AppError) throw sanitizeAppError(e)
            throw AppError.NetworkError("Не удалось подключиться к серверу Subsonic: ${sanitizeSubsonicUrl(e.message)}")
        }
    }

    override suspend fun signOut() {
        AppConfig.subsonicServer = ""
        AppConfig.subsonicUser = ""
        AppConfig.subsonicPassword = ""
        _isSignedIn.value = false
    }

    override suspend fun listTracks(folderId: String?, pageToken: String?): Page<RemoteTrack> {
        if (settingsRepo.load().offlineMode) {
            throw AppError.OfflineModeError()
        }
        val base = getBaseUrl()
        val auth = buildAuthParams()
        val offset = pageToken?.toIntOrNull() ?: 0
        val count = 50

        val url = if (folderId != null) {
            "$base/rest/getMusicDirectory.view?id=${folderId.encodeURLQueryComponent()}&$auth"
        } else {
            "$base/rest/getRandomSongs.view?size=$count&$auth"
        }

        return try {
            val responseText: String = client.get(url) {
                header(HttpHeaders.UserAgent, "JVXMusic/1.0")
            }.body()

            val tracks = parseSubsonicSongs(responseText, base)
            Page(
                items = tracks,
                nextPageToken = if (tracks.size >= count) (offset + count).toString() else null
            )
        } catch (e: Exception) {
            if (e is AppError) throw sanitizeAppError(e)
            throw AppError.NetworkError("Ошибка при получении треков Subsonic: ${sanitizeSubsonicUrl(e.message)}")
        }
    }

    override suspend fun search(query: String): List<RemoteTrack> {
        if (settingsRepo.load().offlineMode) {
            throw AppError.OfflineModeError()
        }
        val clean = query.trim()
        if (clean.isBlank()) return emptyList()

        val base = getBaseUrl()
        val auth = buildAuthParams()
        val url = "$base/rest/search3.view?query=${clean.encodeURLQueryComponent()}&songCount=50&$auth"

        return try {
            val responseText: String = client.get(url) {
                header(HttpHeaders.UserAgent, "JVXMusic/1.0")
            }.body()

            parseSubsonicSongs(responseText, base)
        } catch (e: Exception) {
            if (e is AppError) throw sanitizeAppError(e)
            throw AppError.NetworkError("Ошибка поиска Subsonic: ${sanitizeSubsonicUrl(e.message)}")
        }
    }

    override suspend fun getStream(track: RemoteTrack): StreamSource {
        val base = getBaseUrl()
        val auth = buildAuthParams()
        val trackId = track.id
        val streamUrl = "$base/rest/stream.view?id=${trackId.encodeURLQueryComponent()}&$auth"
        return StreamSource(
            url = streamUrl,
            headers = emptyMap(),
            supportsRange = true,
        )
    }

    /**
     * Parses Subsonic API JSON response. Note: generated streamUrl and artworkUrl do NOT bake
     * auth tokens/salts into the URLs to prevent credentials leaking into caches or logs.
     */
    fun parseSubsonicSongs(responseText: String, base: String, auth: String? = null): List<RemoteTrack> {
        val root = json.parseToJsonElement(responseText).jsonObject
        val subResponse = root["subsonic-response"]?.jsonObject ?: return emptyList()

        val status = subResponse["status"]?.jsonPrimitive?.content
        if (status != "ok") {
            val error = subResponse["error"]?.jsonObject
            val msg = error?.get("message")?.jsonPrimitive?.content ?: "Subsonic API error"
            throw AppError.ProviderError(sanitizeSubsonicUrl(msg))
        }

        val songArray = subResponse["searchResult3"]?.jsonObject?.get("song")?.jsonArray
            ?: subResponse["randomSongs"]?.jsonObject?.get("song")?.jsonArray
            ?: subResponse["directory"]?.jsonObject?.get("child")?.jsonArray
            ?: return emptyList()

        return songArray.mapNotNull { element ->
            val obj = element.jsonObject
            val isDir = obj["isDir"]?.jsonPrimitive?.content?.toBoolean() ?: false
            if (isDir) return@mapNotNull null

            val id = obj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val title = obj["title"]?.jsonPrimitive?.content ?: "Без названия"
            val artist = obj["artist"]?.jsonPrimitive?.content ?: "Неизвестный исполнитель"
            val album = obj["album"]?.jsonPrimitive?.content
            val durationSec = obj["duration"]?.jsonPrimitive?.longOrNull ?: 0L
            val durationMs = durationSec * 1000L
            val suffix = obj["suffix"]?.jsonPrimitive?.content?.lowercase()
            val bitRate = obj["bitRate"]?.jsonPrimitive?.intOrNull
            val size = obj["size"]?.jsonPrimitive?.longOrNull
            val coverArtId = obj["coverArt"]?.jsonPrimitive?.content

            // Clean artwork URL WITHOUT baking credentials into it
            val artworkUrl = if (!coverArtId.isNullOrBlank()) {
                "$base/rest/getCoverArt.view?id=${coverArtId.encodeURLQueryComponent()}"
            } else null

            // Clean stream URL WITHOUT baking credentials into it
            val streamUrl = "$base/rest/stream.view?id=${id.encodeURLQueryComponent()}"

            val isLossless = suffix in listOf("flac", "alac", "wav", "aiff")

            RemoteTrack(
                id = id,
                title = title,
                artist = artist,
                album = album,
                durationMs = durationMs,
                streamUrl = streamUrl,
                artworkUrl = artworkUrl,
                license = "Личная коллекция Subsonic",
                sizeBytes = size,
                source = TrackSource.SUBSONIC,
                mimeType = obj["contentType"]?.jsonPrimitive?.content,
                bitrateKbps = bitRate,
                format = suffix,
                isLossless = isLossless,
                webPageUrl = "$base/#/album?id=${obj["albumId"]?.jsonPrimitive?.content ?: id}"
            )
        }
    }

    /**
     * Provider-level cover art loading:
     * - Supports HTTP when server is on LAN, otherwise requires HTTPS
     * - Enforces 10-second timeout
     * - Validates image content type
     * - Enforces 5 MB size limit
     * - Uses dynamic auth params for fetch WITHOUT baking them into disk cache keys
     */
    suspend fun loadCoverArt(track: Track): ImageBitmap? = withContext(Dispatchers.IO) {
        val base = runCatching { getBaseUrl() }.getOrNull() ?: return@withContext null

        // Security check: plain HTTP allowed only on local area network (LAN)
        if (base.startsWith("http://", ignoreCase = true) && !isLanAddress(base)) {
            return@withContext null
        }

        val coverId = track.remoteId ?: track.id
        // Disk key strictly uses a sanitized hash of track/cover id — NEVER token or salt
        val diskKey = "cover_subsonic_" + coverId.hashCode().toString(16) + ".jpg"
        val memKey = "subsonic_$coverId"

        // 1. Check memory cache
        val memCached = ArtworkCache.get(memKey)
        if (memCached != null) return@withContext memCached

        // 2. Check persistent disk cache
        val cachedBytes = AppStorage.readBytes(diskKey)
        if (cachedBytes != null && cachedBytes.isNotEmpty()) {
            val bmp = decodeImageBytes(cachedBytes)
            if (bmp != null) {
                ArtworkCache.put(memKey, bmp)
                ArtworkCache.computeAndStoreDominantColor(memKey, bmp)
                return@withContext bmp
            }
        }

        // 3. Network fetch
        val auth = runCatching { buildAuthParams() }.getOrNull() ?: return@withContext null
        val fetchUrl = "$base/rest/getCoverArt.view?id=${coverId.encodeURLQueryComponent()}&$auth"

        withTimeoutOrNull(10000L) {
            runCatching {
                val response = client.get(fetchUrl) {
                    header(HttpHeaders.UserAgent, "JVXMusic/1.0 (Subsonic Cover Loader)")
                }
                if (response.status.value !in 200..299) return@runCatching null

                val contentType = response.headers[HttpHeaders.ContentType] ?: ""
                if (!contentType.startsWith("image/", ignoreCase = true)) return@runCatching null

                val declaredLength = response.contentLength() ?: 0L
                if (declaredLength > MAX_COVER_SIZE_BYTES) return@runCatching null

                val imageBytes = response.bodyAsBytes()
                if (imageBytes.isEmpty() || imageBytes.size > MAX_COVER_SIZE_BYTES) return@runCatching null

                // Save to disk cache using non-credentialed key
                AppStorage.writeBytes(diskKey, imageBytes)
                val bmp = decodeImageBytes(imageBytes)
                if (bmp != null) {
                    ArtworkCache.put(memKey, bmp)
                    ArtworkCache.computeAndStoreDominantColor(memKey, bmp)
                    bmp
                } else null
            }.getOrNull()
        }
    }
}
