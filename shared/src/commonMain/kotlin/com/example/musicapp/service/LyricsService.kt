package com.example.musicapp.service

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.musicapp.lyrics.LrcParser
import com.example.musicapp.lyrics.LrclibLyricsProvider
import com.example.musicapp.model.LrcLine
import com.example.musicapp.model.Track
import com.example.musicapp.platform.AppStorage
import com.example.musicapp.provider.appHttpClient
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * Service to load synchronized LRC or plain lyrics with LRCLIB online lookup and disk caching.
 * Privacy-focused:
 * - fetchOnlineLyrics is OFF by default (user must opt-in)
 * - offlineMode turns off all outbound requests immediately
 * - Uses dedicated User-Agent
 */
object LyricsService {
    const val USER_AGENT = LrclibLyricsProvider.USER_AGENT

    var fetchOnlineLyrics by mutableStateOf(false)
    var offlineMode by mutableStateOf(false)

    internal val memoryCache = mutableMapOf<String, List<LrcLine>>()

    suspend fun getLyrics(
        track: Track,
        client: HttpClient? = null
    ): List<LrcLine> = withContext(Dispatchers.IO) {
        val cleanArtist = LrclibLyricsProvider.cleanArtist(track.artist).trim().lowercase()
        val cleanTitle = LrclibLyricsProvider.cleanTitle(track.title).trim().lowercase()
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

        // 3. Privacy check: do not make network requests if offline mode or online lyrics disabled or online sources disabled
        if (!com.example.musicapp.config.AppFeatures.isOnlineSourcesEnabled || offlineMode || !fetchOnlineLyrics) {
            return@withContext emptyList()
        }

        // Sanity check: do not query for unknown or blank metadata
        if (cleanTitle.isBlank() || cleanArtist.isBlank() || cleanArtist == "неизвестный исполнитель") {
            return@withContext emptyList()
        }

        val effectiveClient = client ?: appHttpClient
        // 4. Fetch from lrclib.net open lyrics database with app User-Agent
        runCatching {
            val result = LrclibLyricsProvider.fetchLyrics(
                client = effectiveClient,
                artist = track.artist,
                title = track.title,
                durationMs = track.durationMs
            )

            if (result != null && result.first.isNotEmpty()) {
                val (lines, rawText) = result
                AppStorage.write(diskKey, rawText)
                memoryCache[cacheKey] = lines
                lines
            } else {
                emptyList()
            }
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
