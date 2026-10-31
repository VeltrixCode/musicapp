package com.example.musicapp.service

import com.example.musicapp.lyrics.LrcParser
import com.example.musicapp.model.LrcLine
import com.example.musicapp.model.Track
import com.example.musicapp.platform.AppStorage
import com.example.musicapp.provider.appHttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
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
 */
object LyricsService {
    private val memoryCache = mutableMapOf<String, List<LrcLine>>()

    suspend fun getLyrics(track: Track): List<LrcLine> = withContext(Dispatchers.IO) {
        val cacheKey = "${track.artist.trim().lowercase()}_${track.title.trim().lowercase()}"
        memoryCache[cacheKey]?.let { return@withContext it }

        val diskKey = "lyrics_" + cacheKey.hashCode().toString(16) + ".lrc"
        val cachedText = AppStorage.read(diskKey)
        if (cachedText != null && cachedText.isNotBlank()) {
            val parsed = LrcParser.parse(cachedText)
            if (parsed.isNotEmpty()) {
                memoryCache[cacheKey] = parsed
                return@withContext parsed
            }
        }

        // Fetch from lrclib.net free open lyrics database
        runCatching {
            val artist = track.artist.encodeURLQueryComponent()
            val title = track.title.encodeURLQueryComponent()
            val durationSec = if (track.durationMs > 0) (track.durationMs / 1000).toString() else ""
            val durationParam = if (durationSec.isNotEmpty()) "&duration=$durationSec" else ""
            val url = "https://lrclib.net/api/get?artist_name=$artist&track_name=$title$durationParam"

            val resp = appHttpClient.get(url).body<LrclibResponse>()
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
}
