package com.example.musicapp.service

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.musicapp.lyrics.LrcParser
import com.example.musicapp.model.LrcLine
import com.example.musicapp.model.Track
import com.example.musicapp.platform.AppStorage
import com.example.musicapp.provider.appHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.encodeURLQueryComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
private data class LrclibResponse(
    val id: Long? = null,
    val name: String? = null,
    val artistName: String? = null,
    val albumName: String? = null,
    val duration: Double? = null,
    val syncedLyrics: String? = null,
    val plainLyrics: String? = null,
)

/**
 * Service to load synchronized LRC or plain lyrics with LRCLIB online lookup and disk caching.
 * Privacy-focused:
 * - fetchOnlineLyrics is OFF by default (user must opt-in)
 * - offlineMode turns off all outbound requests immediately
 * - Uses dedicated User-Agent
 */
object LyricsService {
    const val USER_AGENT = "JVXMusic/1.0 (https://github.com/VeltrixCode/MusicApp)"

    var fetchOnlineLyrics by mutableStateOf(false)
    var offlineMode by mutableStateOf(false)

    internal val memoryCache = mutableMapOf<String, List<LrcLine>>()

    suspend fun getLyrics(
        track: Track,
        client: HttpClient? = null
    ): List<LrcLine> = withContext(Dispatchers.IO) {
        val cleanArtist = track.artist.trim().lowercase()
        val cleanTitle = track.title.trim().lowercase()
        val cacheKey = "${cleanArtist}_$cleanTitle"

        // 1. In-memory cache
        memoryCache[cacheKey]?.let { return@withContext it }

        // 2. Persistent local disk cache
        val diskKey = "lyrics_" + cacheKey.hashCode().toString(16) + ".lrc"
        val cachedText = AppStorage.read(diskKey)
        if (cachedText != null && cachedText.isNotBlank()) {
            val parsed = LrcParser.parse(cachedText)
            if (parsed.isNotEmpty()) {
                memoryCache[cacheKey] = parsed
                return@withContext parsed
            }
        }

        // 3. Privacy check: do not make network requests if offline mode or online lyrics disabled or online sources disabled without client
        if ((!com.example.musicapp.config.AppFeatures.isOnlineSourcesEnabled && client == null) || offlineMode || !fetchOnlineLyrics) {
            return@withContext emptyList()
        }

        // Sanity check: do not query for unknown or blank metadata
        if (cleanTitle.isBlank() || cleanArtist.isBlank() || cleanArtist == "неизвестный исполнитель") {
            return@withContext emptyList()
        }

        val effectiveClient = client ?: appHttpClient
        // 4. Fetch from lrclib.net open lyrics database with app User-Agent
        runCatching {
            val artist = track.artist.encodeURLQueryComponent()
            val title = track.title.encodeURLQueryComponent()
            val durationSec = if (track.durationMs > 0) (track.durationMs / 1000).toString() else ""
            val durationParam = if (durationSec.isNotEmpty()) "&duration=$durationSec" else ""
            val url = "https://lrclib.net/api/get?artist_name=$artist&track_name=$title$durationParam"

            val resp = effectiveClient.get(url) {
                header(HttpHeaders.UserAgent, USER_AGENT)
            }.body<LrclibResponse>()

            val synced = resp.syncedLyrics
            if (synced != null && synced.isNotBlank()) {
                AppStorage.write(diskKey, synced)
                val lines = LrcParser.parse(synced)
                memoryCache[cacheKey] = lines
                return@runCatching lines
            }

            val plain = resp.plainLyrics
            if (plain != null && plain.isNotBlank()) {
                val lines = plain.lines().filter { it.isNotBlank() }.mapIndexed { idx, line ->
                    LrcLine(timestampMs = idx * 4000L, text = line)
                }
                memoryCache[cacheKey] = lines
                lines
            } else emptyList()
        }.getOrElse { emptyList() }
    }

    fun clearCache() {
        memoryCache.clear()
    }

    /**
     * Clears in-memory caches. Does NOT close the shared appHttpClient,
     * which lives for the entire application lifecycle and is closed only on app exit.
     */
    fun close() {
        clearCache()
    }
}
