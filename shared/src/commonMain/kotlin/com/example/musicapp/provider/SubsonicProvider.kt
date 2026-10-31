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
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.encodeURLQueryComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.intOrNull

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

    private fun buildAuthParams(): String {
        val user = AppConfig.subsonicUser.trim()
        val password = AppConfig.subsonicPassword
        if (user.isBlank() || password.isBlank()) {
            throw AppError.AuthError("Учётные данные Subsonic не настроены")
        }
        val salt = "jvx" + (1000..9999).random()
        val token = CryptoUtils.md5Hex(password + salt)
        return "u=${user.encodeURLQueryComponent()}&t=$token&s=$salt&v=1.16.1&c=JVXMusic&f=json"
    }

    private fun getBaseUrl(): String {
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
                throw AppError.AuthError(msg)
            }
        } catch (e: Exception) {
            _isSignedIn.value = false
            if (e is AppError) throw e
            throw AppError.NetworkError("Не удалось подключиться к серверу Subsonic: ${e.message}", e)
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

            val tracks = parseSubsonicSongs(responseText, base, auth)
            Page(
                items = tracks,
                nextPageToken = if (tracks.size >= count) (offset + count).toString() else null
            )
        } catch (e: Exception) {
            if (e is AppError) throw e
            throw AppError.NetworkError("Ошибка при получении треков Subsonic: ${e.message}", e)
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

            parseSubsonicSongs(responseText, base, auth)
        } catch (e: Exception) {
            if (e is AppError) throw e
            throw AppError.NetworkError("Ошибка поиска Subsonic: ${e.message}", e)
        }
    }

    override suspend fun getStream(track: RemoteTrack): StreamSource {
        val base = getBaseUrl()
        val auth = buildAuthParams()
        val streamUrl = "$base/rest/stream.view?id=${track.id.encodeURLQueryComponent()}&$auth"
        return StreamSource(
            url = streamUrl,
            headers = emptyMap(),
            supportsRange = true,
        )
    }

    fun parseSubsonicSongs(responseText: String, base: String, auth: String): List<RemoteTrack> {
        val root = json.parseToJsonElement(responseText).jsonObject
        val subResponse = root["subsonic-response"]?.jsonObject ?: return emptyList()

        val status = subResponse["status"]?.jsonPrimitive?.content
        if (status != "ok") {
            val error = subResponse["error"]?.jsonObject
            val msg = error?.get("message")?.jsonPrimitive?.content ?: "Subsonic API error"
            throw AppError.ProviderError(msg)
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

            val artworkUrl = if (!coverArtId.isNullOrBlank()) {
                "$base/rest/getCoverArt.view?id=${coverArtId.encodeURLQueryComponent()}&$auth"
            } else null

            val isLossless = suffix in listOf("flac", "alac", "wav", "aiff")

            RemoteTrack(
                id = id,
                title = title,
                artist = artist,
                album = album,
                durationMs = durationMs,
                streamUrl = "$base/rest/stream.view?id=${id.encodeURLQueryComponent()}&$auth",
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
}
