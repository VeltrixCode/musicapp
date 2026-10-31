package com.example.musicapp.service

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import com.example.musicapp.cache.ArtworkCache
import com.example.musicapp.model.Track
import com.example.musicapp.platform.AppStorage
import com.example.musicapp.platform.decodeImageBytes
import com.example.musicapp.platform.loadArtwork
import com.example.musicapp.provider.appHttpClient
import com.example.musicapp.provider.appJson
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.encodeURLQueryComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

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
 * Multi-tier cover art resolver matching Spotify visual standards:
 * 1. Embedded ID3 / container artwork
 * 2. Online high-res lookup (iTunes Search API) with local disk & memory cache
 * 3. Graceful fallback to deterministic gradient by title hash
 */
object CoverArtService {
    var loadOnlineCovers by mutableStateOf(true)

    suspend fun loadArtworkForTrack(track: Track): ImageBitmap? = withContext(Dispatchers.IO) {
        // 1. Try local embedded file tags
        val src = track.source
        if (src != null) {
            val localBmp = loadArtwork(src)
            if (localBmp != null) return@withContext localBmp
        }

        // 2. If no embedded tag or cloud track, perform online lookup if enabled
        if (!loadOnlineCovers) return@withContext null
        if (track.title.isBlank() || track.artist.isBlank() || track.artist == "Неизвестный исполнитель") {
            return@withContext null
        }

        getOrFetchOnlineCover(track.artist, track.title)
    }

    suspend fun getOrFetchOnlineCover(artist: String, title: String): ImageBitmap? = withContext(Dispatchers.IO) {
        val normalizedKey = "online_${artist.trim().lowercase()}_${title.trim().lowercase()}"
        val memCached = ArtworkCache.get(normalizedKey)
        if (memCached != null) return@withContext memCached

        // Check disk cache
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

        // Online lookup via iTunes Search API
        runCatching {
            val term = "$artist $title".encodeURLQueryComponent()
            val url = "https://itunes.apple.com/search?term=$term&entity=song&limit=1"
            val resp = appHttpClient.get(url).body<ITunesSearchResponse>()
            val item = resp.results.firstOrNull() ?: return@runCatching null
            val rawArtUrl = item.artworkUrl100 ?: return@runCatching null

            // Upgrade 100x100 to 600x600 high quality
            val highResUrl = rawArtUrl.replace("100x100bb", "600x600bb")
            val imageBytes = appHttpClient.get(highResUrl).bodyAsBytes()

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
}
