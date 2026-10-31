package com.example.musicapp.service

import androidx.compose.ui.graphics.ImageBitmap
import com.example.musicapp.cache.ArtworkCache
import com.example.musicapp.model.Track
import com.example.musicapp.platform.AppStorage
import com.example.musicapp.platform.decodeImageBytes
import com.example.musicapp.platform.loadArtwork
import com.example.musicapp.provider.appHttpClient
import com.example.musicapp.settings.SettingsRepository
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
private data class ITunesSearchResponse(
    val resultCount: Int = 0,
    val results: List<ITunesSearchResult> = emptyList(),
)

@Serializable
private data class ITunesSearchResult(
    val artworkUrl100: String? = null,
)

/**
 * Multi-tier cover art resolver:
 * 1. Embedded ID3 / container artwork (local)
 * 2. Online high-res lookup (iTunes Search API with rate limiting)
 * 3. Fallback lookup (Cover Art Archive / MusicBrainz)
 * 4. Negative caching: failed lookups are not re-queried for 7 days
 * 5. Privacy: strictly avoids sending local file paths or raw file names upstream
 * 6. Global offline mode support
 */
object CoverArtService {
    const val USER_AGENT = "JVXMusic/1.0 (https://github.com/VeltrixCode/MusicApp)"
    const val MUSICBRAINZ_USER_AGENT = "JVXMusic/1.0 (https://github.com/VeltrixCode/MusicApp)"
    const val NEGATIVE_CACHE_TTL_MS = 7 * 24 * 60 * 60 * 1000L // 7 days
    private const val NEGATIVE_CACHE_FILE = "negative_cover_cache.json"

    // Single source of truth for online covers / offline mode
    var settingsRepository: SettingsRepository = SettingsRepository()
    var subsonicProvider: com.example.musicapp.provider.SubsonicProvider = com.example.musicapp.provider.SubsonicProvider()

    // Rate limiter mutexes: ensures at least 350ms between iTunes calls, 1000ms between MusicBrainz calls
    private val itunesRateLimiterMutex = Mutex()
    private var lastItunesTimeMs = 0L

    private val mbRateLimiterMutex = Mutex()
    private var lastMbTimeMs = 0L

    // Thread-safe negative cache: normalizedKey -> timestamp of failed query
    private val negativeCacheMutex = Mutex()
    internal val negativeCache = mutableMapOf<String, Long>()
    private var isNegativeCacheLoaded = false

    private fun loadNegativeCacheIfNeededLocked() {
        if (isNegativeCacheLoaded) return
        isNegativeCacheLoaded = true
        runCatching {
            val text = AppStorage.read(NEGATIVE_CACHE_FILE) ?: return
            val json = Json { ignoreUnknownKeys = true }
            val map = json.decodeFromString<Map<String, Long>>(text)
            val now = System.currentTimeMillis()
            map.forEach { (k, time) ->
                if (now - time < NEGATIVE_CACHE_TTL_MS) {
                    negativeCache[k] = time
                }
            }
        }
    }

    private fun persistNegativeCacheLocked() {
        runCatching {
            val json = Json { prettyPrint = false }
            val encoded = json.encodeToString<Map<String, Long>>(negativeCache)
            AppStorage.writeAtomic(NEGATIVE_CACHE_FILE, encoded)
        }
    }

    suspend fun isNegativeCached(key: String): Boolean = negativeCacheMutex.withLock {
        loadNegativeCacheIfNeededLocked()
        val lastFailedTime = negativeCache[key] ?: return false
        val elapsed = System.currentTimeMillis() - lastFailedTime
        if (elapsed < NEGATIVE_CACHE_TTL_MS) {
            true
        } else {
            negativeCache.remove(key)
            persistNegativeCacheLocked()
            false
        }
    }

    suspend fun recordNegativeCache(key: String) = negativeCacheMutex.withLock {
        loadNegativeCacheIfNeededLocked()
        negativeCache[key] = System.currentTimeMillis()
        persistNegativeCacheLocked()
    }

    /**
     * Checks whether an artist or title string looks like a local file path or extension.
     */
    fun containsLocalFilePathOrExtension(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains('/') ||
                lower.contains('\\') ||
                lower.contains(":\\") ||
                lower.endsWith(".mp3") ||
                lower.endsWith(".flac") ||
                lower.endsWith(".wav") ||
                lower.endsWith(".ogg") ||
                lower.endsWith(".m4a") ||
                lower.endsWith(".aac")
    }

    const val MAX_COVER_SIZE_BYTES = 5L * 1024L * 1024L // 5 MB limit

    suspend fun loadArtworkByUrl(
        url: String,
        client: HttpClient = appHttpClient
    ): ImageBitmap? = withContext(Dispatchers.IO) {
        val settings = settingsRepository.load()
        if (settings.offlineMode || !settings.fetchOnlineCovers) return@withContext null
        if (url.isBlank()) return@withContext null
        // Requirement 3: Only HTTPS allowed
        if (!url.startsWith("https://", ignoreCase = true)) return@withContext null

        val diskKey = "cover_url_" + url.hashCode().toString(16) + ".jpg"

        // 1. Memory cache check
        val memCached = ArtworkCache.get(url)
        if (memCached != null) return@withContext memCached

        // 2. Persistent disk cache check
        val cachedBytes = AppStorage.readBytes(diskKey)
        if (cachedBytes != null && cachedBytes.isNotEmpty()) {
            val bmp = decodeImageBytes(cachedBytes)
            if (bmp != null) {
                ArtworkCache.put(url, bmp)
                ArtworkCache.computeAndStoreDominantColor(url, bmp)
                return@withContext bmp
            }
        }

        // 3. Network fetch with 10s timeout, content-type check, size limit, no Authorization header
        withTimeoutOrNull(10000L) {
            runCatching {
                val response = client.get(url) {
                    header(HttpHeaders.UserAgent, USER_AGENT)
                    headers.remove(HttpHeaders.Authorization)
                }
                if (response.status.value !in 200..299) return@runCatching null

                // Verify Content-Type is image/*
                val contentType = response.headers[HttpHeaders.ContentType] ?: ""
                if (!contentType.startsWith("image/", ignoreCase = true)) return@runCatching null

                // Check advertised Content-Length if present
                val declaredLength = response.contentLength() ?: 0L
                if (declaredLength > MAX_COVER_SIZE_BYTES) return@runCatching null

                val imageBytes = response.bodyAsBytes()
                if (imageBytes.isEmpty() || imageBytes.size > MAX_COVER_SIZE_BYTES) return@runCatching null

                AppStorage.writeBytes(diskKey, imageBytes)
                val bmp = decodeImageBytes(imageBytes)
                if (bmp != null) {
                    ArtworkCache.put(url, bmp)
                    ArtworkCache.computeAndStoreDominantColor(url, bmp)
                    bmp
                } else null
            }.getOrNull()
        }
    }

    fun clearCoverCache() {
        ArtworkCache.clear()
        runCatching {
            AppStorage.deleteFilesWithPrefix("cover_")
        }
    }

    suspend fun loadArtworkForTrack(
        track: Track,
        client: HttpClient = appHttpClient
    ): ImageBitmap? = withContext(Dispatchers.IO) {
        // 1. Try local embedded file tags
        val src = track.source
        if (src != null && track.sourceProvider == com.example.musicapp.model.TrackSource.LOCAL) {
            val localBmp = loadArtwork(src)
            if (localBmp != null) return@withContext localBmp
        }

        // Subsonic provider-level cover loading (supports HTTP on LAN, size limit, non-leaking cache keys)
        if (track.sourceProvider == com.example.musicapp.model.TrackSource.SUBSONIC) {
            val subBmp = subsonicProvider.loadCoverArt(track)
            if (subBmp != null) return@withContext subBmp
        }

        // 2. Direct artworkUrl provided by cloud providers (Audius 480x480, Jamendo image, etc.)
        val directUrl = track.artworkUrl
        if (!directUrl.isNullOrBlank()) {
            val cloudBmp = loadArtworkByUrl(directUrl, client)
            if (cloudBmp != null) return@withContext cloudBmp
        }

        // Fallback to local artwork check if not checked yet
        if (src != null) {
            val localBmp = loadArtwork(src)
            if (localBmp != null) return@withContext localBmp
        }

        // 3. Online check: verify global settings from SettingsRepository (single source of truth)
        val settings = settingsRepository.load()
        if (settings.offlineMode || !settings.fetchOnlineCovers) return@withContext null

        val artist = track.artist.trim()
        val title = track.title.trim()

        // Privacy check: do NOT leak file paths or raw file names to external web services
        if (artist.isBlank() || title.isBlank() || artist.equals("Неизвестный исполнитель", ignoreCase = true)) {
            return@withContext null
        }
        if (containsLocalFilePathOrExtension(artist) || containsLocalFilePathOrExtension(title)) {
            return@withContext null
        }

        getOrFetchOnlineCover(artist, title, client)
    }

    suspend fun getOrFetchOnlineCover(
        artist: String,
        title: String,
        client: HttpClient = appHttpClient
    ): ImageBitmap? = withContext(Dispatchers.IO) {
        val settings = settingsRepository.load()
        if (settings.offlineMode || !settings.fetchOnlineCovers) return@withContext null

        val normalizedKey = "online_${artist.trim().lowercase()}_${title.trim().lowercase()}"

        // Memory cache check
        val memCached = ArtworkCache.get(normalizedKey)
        if (memCached != null) return@withContext memCached

        // Persistent disk cache check
        val diskKey = "cover_" + normalizedKey.hashCode().toString(16) + ".jpg"
        val cachedBytes = AppStorage.readBytes(diskKey)
        if (cachedBytes != null && cachedBytes.isNotEmpty()) {
            val bmp = decodeImageBytes(cachedBytes)
            if (bmp != null) {
                ArtworkCache.put(normalizedKey, bmp)
                ArtworkCache.computeAndStoreDominantColor(normalizedKey, bmp)
                return@withContext bmp
            }
        }

        // Negative cache check (thread-safe, do not search again for 7 days)
        if (isNegativeCached(normalizedKey)) {
            return@withContext null
        }

        // 1. Attempt lookup via iTunes Search API with rate limiting
        val itunesBitmap = fetchFromITunes(artist, title, diskKey, normalizedKey, client)
        if (itunesBitmap != null) {
            return@withContext itunesBitmap
        }

        // 2. Fallback lookup via Cover Art Archive / MusicBrainz
        val archiveBitmap = fetchFromCoverArtArchive(artist, title, diskKey, normalizedKey, client)
        if (archiveBitmap != null) {
            return@withContext archiveBitmap
        }

        // Record negative cache entry for 7 days (thread-safe and atomic persistence)
        recordNegativeCache(normalizedKey)
        null
    }

    private suspend fun fetchFromITunes(
        artist: String,
        title: String,
        diskKey: String,
        normalizedKey: String,
        client: HttpClient
    ): ImageBitmap? {
        return runCatching {
            // Apply rate limit queue: 350ms between requests
            itunesRateLimiterMutex.withLock {
                val now = System.currentTimeMillis()
                val delta = now - lastItunesTimeMs
                if (delta < 350L) {
                    delay(350L - delta)
                }
                lastItunesTimeMs = System.currentTimeMillis()
            }

            val term = "$artist $title".encodeURLQueryComponent()
            val url = "https://itunes.apple.com/search?term=$term&entity=song&limit=1"
            val resp = client.get(url) {
                header(HttpHeaders.UserAgent, USER_AGENT)
            }.body<ITunesSearchResponse>()

            val item = resp.results.firstOrNull() ?: return@runCatching null
            val rawArtUrl = item.artworkUrl100 ?: return@runCatching null

            // Upgrade 100x100 to 600x600 high quality
            val highResUrl = rawArtUrl.replace("100x100bb", "600x600bb")
            val imageBytes = client.get(highResUrl) {
                header(HttpHeaders.UserAgent, USER_AGENT)
            }.bodyAsBytes()

            if (imageBytes.isNotEmpty()) {
                AppStorage.writeBytes(diskKey, imageBytes)
                val bmp = decodeImageBytes(imageBytes)
                if (bmp != null) {
                    ArtworkCache.put(normalizedKey, bmp)
                    ArtworkCache.computeAndStoreDominantColor(normalizedKey, bmp)
                    bmp
                } else null
            } else null
        }.getOrNull()
    }

    private suspend fun fetchFromCoverArtArchive(
        artist: String,
        title: String,
        diskKey: String,
        normalizedKey: String,
        client: HttpClient
    ): ImageBitmap? {
        return runCatching {
            // Apply rate limit queue: at least 1000ms between requests for MusicBrainz/CAA policy
            mbRateLimiterMutex.withLock {
                val now = System.currentTimeMillis()
                val delta = now - lastMbTimeMs
                if (delta < 1000L) {
                    delay(1000L - delta)
                }
                lastMbTimeMs = System.currentTimeMillis()
            }

            val q = "artist:\"$artist\" AND recording:\"$title\"".encodeURLQueryComponent()
            val mbUrl = "https://musicbrainz.org/ws/2/recording?query=$q&fmt=json&limit=1"
            val mbText = client.get(mbUrl) {
                header(HttpHeaders.UserAgent, MUSICBRAINZ_USER_AGENT)
            }.body<String>()

            val json = Json { ignoreUnknownKeys = true }
            val root = json.parseToJsonElement(mbText).jsonObject
            val recordings = root["recordings"]?.jsonArray
            val firstRec = recordings?.firstOrNull()?.jsonObject ?: return@runCatching null
            val releases = firstRec["releases"]?.jsonArray
            val firstRelease = releases?.firstOrNull()?.jsonObject ?: return@runCatching null
            val releaseId = firstRelease["id"]?.jsonPrimitive?.content ?: return@runCatching null

            val coverUrl = "https://coverartarchive.org/release/$releaseId/front-250"
            val imageBytes = client.get(coverUrl) {
                header(HttpHeaders.UserAgent, MUSICBRAINZ_USER_AGENT)
            }.bodyAsBytes()

            if (imageBytes.isNotEmpty()) {
                AppStorage.writeBytes(diskKey, imageBytes)
                val bmp = decodeImageBytes(imageBytes)
                if (bmp != null) {
                    ArtworkCache.put(normalizedKey, bmp)
                    ArtworkCache.computeAndStoreDominantColor(normalizedKey, bmp)
                    bmp
                } else null
            } else null
        }.getOrNull()
    }

    suspend fun clearNegativeCache() = negativeCacheMutex.withLock {
        negativeCache.clear()
        isNegativeCacheLoaded = true
        AppStorage.writeAtomic(NEGATIVE_CACHE_FILE, "{}")
    }

    fun clearNegativeCacheBlocking() = kotlinx.coroutines.runBlocking {
        clearNegativeCache()
    }
}
