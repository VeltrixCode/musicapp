package com.example.musicapp.provider

import com.example.musicapp.model.AppError
import com.example.musicapp.model.Page
import com.example.musicapp.model.RemoteTrack
import com.example.musicapp.model.StreamSource
import com.example.musicapp.model.TrackSource
import com.example.musicapp.settings.SettingsRepository
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
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class RadioBrowserProvider(
    private val client: HttpClient = appHttpClient,
    private val settingsRepo: SettingsRepository = SettingsRepository(),
) : MusicProvider {

    override val id: String = "radio_browser"
    override val displayName: String = "Radio Browser"
    override val source: TrackSource = TrackSource.RADIO_BROWSER
    override val iconName: String = "Radio"
    override val isPublicCatalog: Boolean = true

    private val _isSignedIn = MutableStateFlow(true)
    override val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    private val apiMirrors = listOf(
        "https://de1.api.radio-browser.info",
        "https://nl1.api.radio-browser.info",
        "https://at1.api.radio-browser.info"
    )

    override suspend fun signIn() {
        _isSignedIn.value = true
    }

    override suspend fun signOut() {
        // Public catalog
    }

    override suspend fun listTracks(folderId: String?, pageToken: String?): Page<RemoteTrack> {
        if (settingsRepo.load().offlineMode) {
            throw AppError.OfflineModeError()
        }
        val stations = fetchWithMirrorFallback { base ->
            "$base/json/stations/topvote?limit=50&hidebroken=true"
        }
        return Page(items = stations)
    }

    override suspend fun search(query: String): List<RemoteTrack> {
        if (settingsRepo.load().offlineMode) {
            throw AppError.OfflineModeError()
        }
        val clean = query.trim()
        if (clean.isBlank()) return listTracks().items

        return fetchWithMirrorFallback { base ->
            "$base/json/stations/search?name=${clean.encodeURLQueryComponent()}&limit=50&hidebroken=true&order=votes&reverse=true"
        }
    }

    override suspend fun getStream(track: RemoteTrack): StreamSource {
        val streamUrl = track.streamUrl ?: track.id
        return StreamSource(
            url = streamUrl,
            headers = emptyMap(),
            supportsRange = false // Live radio stream does not support range requests/seeking
        )
    }

    private suspend fun fetchWithMirrorFallback(urlBuilder: (String) -> String): List<RemoteTrack> {
        var lastException: Exception? = null
        for (mirror in apiMirrors) {
            try {
                val url = urlBuilder(mirror)
                val responseText: String = client.get(url) {
                    header(HttpHeaders.UserAgent, "JVXMusic/1.0 (Radio Browser Client)")
                }.body()
                return parseRadioStationsJson(responseText)
            } catch (e: Exception) {
                lastException = e
            }
        }
        if (lastException is AppError) throw lastException
        throw AppError.NetworkError("Не удалось получить список радиостанций: ${lastException?.message}", lastException)
    }

    fun parseRadioStationsJson(responseText: String): List<RemoteTrack> {
        val array = json.parseToJsonElement(responseText).jsonArray
        return array.mapNotNull { element ->
            val obj = element.jsonObject
            val uuid = obj["stationuuid"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val name = obj["name"]?.jsonPrimitive?.content?.trim() ?: "Без названия"
            val streamUrl = obj["url_resolved"]?.jsonPrimitive?.content?.ifBlank { null }
                ?: obj["url"]?.jsonPrimitive?.content ?: return@mapNotNull null

            val favicon = obj["favicon"]?.jsonPrimitive?.content?.trim()?.ifBlank { null }
            val tags = obj["tags"]?.jsonPrimitive?.content?.trim()
            val country = obj["country"]?.jsonPrimitive?.content?.trim()
            val bitrate = obj["bitrate"]?.jsonPrimitive?.intOrNull?.takeIf { it > 0 }
            val codec = obj["codec"]?.jsonPrimitive?.content?.trim()?.lowercase()
            val homepage = obj["homepage"]?.jsonPrimitive?.content?.trim()

            val artistLabel = when {
                !tags.isNullOrBlank() -> tags.split(",").take(3).joinToString(", ") { it.trim() }
                !country.isNullOrBlank() -> country
                else -> "Интернет-радио"
            }

            RemoteTrack(
                id = uuid,
                title = name,
                artist = artistLabel,
                album = "Интернет-радио",
                durationMs = 0L,
                streamUrl = streamUrl,
                artworkUrl = favicon,
                license = "Radio Browser / Community Open Data",
                source = TrackSource.RADIO_BROWSER,
                mimeType = if (!codec.isNullOrBlank()) "audio/$codec" else null,
                bitrateKbps = bitrate,
                format = codec,
                isLossless = codec in listOf("flac", "wav"),
                webPageUrl = homepage ?: "https://www.radio-browser.info"
            )
        }
    }
}
