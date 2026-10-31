package com.example.musicapp.provider

import com.example.musicapp.model.AppError
import com.example.musicapp.model.Page
import com.example.musicapp.model.RemoteTrack
import com.example.musicapp.model.StreamSource
import com.example.musicapp.model.TrackSource
import com.example.musicapp.platform.currentTimeMillis
import com.example.musicapp.settings.SettingsRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.encodeURLQueryComponent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class InternetArchiveProvider(
    private val client: HttpClient = appHttpClient,
    private val settingsRepo: SettingsRepository = SettingsRepository(),
) : MusicProvider {

    override val id: String = "internet_archive"
    override val displayName: String = "Internet Archive"
    override val source: TrackSource = TrackSource.INTERNET_ARCHIVE
    override val iconName: String = "AccountBalance"
    override val isPublicCatalog: Boolean = true

    private val _isSignedIn = MutableStateFlow(true)
    override val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        const val MIN_QUERY_DELAY_MS = 1000L // 1 second minimum delay between metadata queries
        const val DEFAULT_NO_LICENSE = "лицензия не указана"

        fun isLicenseRestrictedOrUnknown(license: String?): Boolean {
            if (license.isNullOrBlank()) return true
            if (license.equals(DEFAULT_NO_LICENSE, ignoreCase = true)) return true
            val lower = license.lowercase()
            return !(lower.contains("publicdomain") ||
                    lower.contains("public domain") ||
                    lower.contains("creativecommons.org") ||
                    lower.contains("cc-by") ||
                    lower.contains("cc0"))
        }
    }

    private val rateLimiterMutex = Mutex()
    private var lastQueryTimeMs = 0L

    suspend fun enforceRateLimit() = rateLimiterMutex.withLock {
        val now = currentTimeMillis()
        val elapsed = now - lastQueryTimeMs
        if (elapsed < MIN_QUERY_DELAY_MS) {
            delay(MIN_QUERY_DELAY_MS - elapsed)
        }
        lastQueryTimeMs = currentTimeMillis()
    }

    override suspend fun signIn() {
        _isSignedIn.value = true
    }

    override suspend fun signOut() {
        // No-op for public catalog
    }

    override suspend fun listTracks(folderId: String?, pageToken: String?): Page<RemoteTrack> {
        val tracks = search("ccMusic")
        return Page(items = tracks)
    }

    override suspend fun search(query: String): List<RemoteTrack> {
        if (settingsRepo.load().offlineMode) {
            throw AppError.OfflineModeError()
        }
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return emptyList()

        return try {
            enforceRateLimit()
            val encodedQuery = cleanQuery.encodeURLQueryComponent()
            val url = "https://archive.org/advancedsearch.php?q=collection:(audio)+AND+($encodedQuery)&fl[]=identifier,title,creator,album,mediatype,licenseurl,year&sort[]=&rows=40&page=1&output=json"

            val responseText: String = client.get(url) {
                header(HttpHeaders.UserAgent, "JVXMusic/1.0 (Public Catalog Player)")
            }.body()

            parseSearchResponse(responseText)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun parseSearchResponse(jsonString: String): List<RemoteTrack> {
        return try {
            val root = json.parseToJsonElement(jsonString).jsonObject
            val responseObj = root["response"]?.jsonObject ?: return emptyList()
            val docs = responseObj["docs"]?.jsonArray ?: return emptyList()

            docs.mapNotNull { docEl ->
                val doc = docEl.jsonObject
                val identifier = doc["identifier"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val title = doc["title"]?.jsonPrimitive?.contentOrNull ?: identifier
                val creator = doc["creator"]?.jsonPrimitive?.contentOrNull ?: "Internet Archive Audio"
                val album = doc["album"]?.jsonPrimitive?.contentOrNull

                val rawLicense = doc["licenseurl"]?.jsonPrimitive?.contentOrNull?.trim()
                    ?: doc["license"]?.jsonPrimitive?.contentOrNull?.trim()
                val license = if (!rawLicense.isNullOrBlank()) rawLicense else DEFAULT_NO_LICENSE

                val webUrl = "https://archive.org/details/$identifier"
                val artwork = "https://archive.org/services/img/$identifier"

                RemoteTrack(
                    id = "ia_$identifier",
                    title = title,
                    artist = creator,
                    album = album,
                    durationMs = 0L,
                    streamUrl = null, // resolved dynamically on stream request
                    artworkUrl = artwork,
                    license = license,
                    source = TrackSource.INTERNET_ARCHIVE,
                    webPageUrl = webUrl,
                    format = "mp3"
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun getStream(track: RemoteTrack): StreamSource {
        if (settingsRepo.load().offlineMode) {
            throw AppError.OfflineModeError()
        }
        enforceRateLimit()
        val identifier = track.id.removePrefix("ia_")
        val metadataUrl = "https://archive.org/metadata/$identifier"

        val metadataText: String = client.get(metadataUrl) {
            header(HttpHeaders.UserAgent, "JVXMusic/1.0 (Public Catalog Player)")
        }.body()

        val fileName = parseBestAudioFileName(metadataText)
            ?: "${identifier}_vbr.mp3"

        val streamUrl = "https://archive.org/download/$identifier/$fileName"
        return StreamSource(
            url = streamUrl,
            headers = mapOf("User-Agent" to "JVXMusic/1.0"),
            supportsRange = true
        )
    }

    fun parseBestAudioFileName(metadataJson: String): String? {
        return try {
            val root = json.parseToJsonElement(metadataJson).jsonObject
            val files = root["files"]?.jsonArray ?: return null
            var bestFlac: String? = null
            var bestVbrMp3: String? = null
            var bestMp3: String? = null
            var bestOgg: String? = null

            for (f in files) {
                val fObj = f.jsonObject
                val name = fObj["name"]?.jsonPrimitive?.contentOrNull ?: continue
                val format = fObj["format"]?.jsonPrimitive?.contentOrNull?.lowercase() ?: ""

                when {
                    format.contains("flac") || name.endsWith(".flac", ignoreCase = true) -> bestFlac = name
                    format.contains("vbr mp3") -> bestVbrMp3 = name
                    format.contains("mp3") || name.endsWith(".mp3", ignoreCase = true) -> if (bestMp3 == null) bestMp3 = name
                    format.contains("ogg") || name.endsWith(".ogg", ignoreCase = true) -> if (bestOgg == null) bestOgg = name
                }
            }

            bestVbrMp3 ?: bestMp3 ?: bestFlac ?: bestOgg
        } catch (_: Exception) {
            null
        }
    }
}
