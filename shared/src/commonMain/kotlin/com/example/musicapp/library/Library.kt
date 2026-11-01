package com.example.musicapp.library

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.musicapp.model.LibraryData
import com.example.musicapp.model.Playlist
import com.example.musicapp.model.Track
import com.example.musicapp.platform.AppStorage
import com.example.musicapp.platform.currentTimeMillis
import com.example.musicapp.platform.readTrackInfoAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random

private const val LIBRARY_FILE = "library.json"
const val FAVORITES_PLAYLIST_ID = "p_favorites"
const val DEBUG_DEMO_TRACKS = false

private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

fun newId(): String = Random.nextLong().toString(16)

enum class SortColumn { Number, Title, Artist, Album, Duration, AddedAt, PlayCount }
enum class SortDirection { Ascending, Descending }

data class DeletedTrackSnapshot(
    val track: Track,
    val originalIndex: Int,
    val playlistIds: List<String>,
)

/**
 * User library with automatic favorites playlist, recently played history,
 * metadata enrichment, multiple sorting criteria, and undo support.
 */
class Library {
    val tracks = mutableStateListOf<Track>()
    val playlists = mutableStateListOf<Playlist>()
    val recentlyPlayedIds = mutableStateListOf<String>()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    var lastDeleted by mutableStateOf<DeletedTrackSnapshot?>(null)
        private set

    init {
        val raw = AppStorage.read(LIBRARY_FILE)
        val saved = raw?.let { runCatching { json.decodeFromString<LibraryData>(it) }.getOrNull() }
        if (saved != null) {
            tracks.addAll(saved.tracks)
            playlists.addAll(saved.playlists)
            recentlyPlayedIds.addAll(saved.recentlyPlayedIds)
        } else {
            if (raw != null) AppStorage.write("$LIBRARY_FILE.bak", raw)
            if (DEBUG_DEMO_TRACKS) {
                tracks.addAll(sampleTracks)
                playlists.addAll(samplePlaylists)
            }
            save()
        }

        // Ensure system "Любимые треки" playlist exists
        ensureFavoritesPlaylist()

        // Asynchronously enrich missing durations and tags for local files
        scope.launch {
            for (i in tracks.indices) {
                val t = tracks.getOrNull(i) ?: continue
                if ((t.durationMs <= 0L || t.album.isBlank()) && t.source != null) {
                    val info = readTrackInfoAsync(t.source)
                    if (info != null) {
                        val currentIdx = tracks.indexOfFirst { it.id == t.id }
                        if (currentIdx >= 0) {
                            tracks[currentIdx] = t.copy(
                                durationMs = if (info.durationMs > 0) info.durationMs else t.durationMs,
                                title = if (t.title == "Unknown" || t.title.isBlank()) (info.title ?: t.title) else t.title,
                                artist = if (t.artist == "Unknown artist") (info.artist ?: t.artist) else t.artist,
                                album = if (t.album.isBlank()) (info.album ?: "") else t.album,
                                genre = t.genre ?: info.genre,
                                year = t.year ?: info.year
                            )
                        }
                    }
                }
            }
            save()
        }
    }

    private fun ensureFavoritesPlaylist() {
        if (playlists.none { it.id == FAVORITES_PLAYLIST_ID }) {
            playlists.add(
                0,
                Playlist(
                    id = FAVORITES_PLAYLIST_ID,
                    name = "Любимые треки",
                    trackIds = tracks.filter { it.isFavorite }.map { it.id },
                    isSystem = true
                )
            )
            save()
        }
    }

    fun addTrack(track: Track) {
        tracks.add(0, track)
        save()
    }

    fun addTracks(newTracks: List<Track>) {
        tracks.addAll(0, newTracks)
        save()
    }

    fun toggleFavorite(track: Track): Boolean {
        val idx = tracks.indexOfFirst { it.id == track.id }
        if (idx < 0) return false
        val newFav = !tracks[idx].isFavorite
        tracks[idx] = tracks[idx].copy(isFavorite = newFav)

        val favIdx = playlists.indexOfFirst { it.id == FAVORITES_PLAYLIST_ID }
        if (favIdx >= 0) {
            val curIds = playlists[favIdx].trackIds
            val updated = if (newFav) {
                if (track.id !in curIds) listOf(track.id) + curIds else curIds
            } else {
                curIds - track.id
            }
            playlists[favIdx] = playlists[favIdx].copy(trackIds = updated)
        }
        save()
        return newFav
    }

    fun recordTrackPlayed(track: Track) {
        val idx = tracks.indexOfFirst { it.id == track.id }
        if (idx >= 0) {
            tracks[idx] = tracks[idx].copy(playCount = tracks[idx].playCount + 1)
        }
        recentlyPlayedIds.remove(track.id)
        recentlyPlayedIds.add(0, track.id)
        if (recentlyPlayedIds.size > 50) {
            recentlyPlayedIds.removeLast()
        }
        save()
    }

    fun renameTrack(trackId: String, newTitle: String, newArtist: String) {
        val idx = tracks.indexOfFirst { it.id == trackId }
        if (idx >= 0) {
            tracks[idx] = tracks[idx].copy(title = newTitle.trim(), artist = newArtist.trim())
            save()
        }
    }

    fun removeTrack(trackId: String): DeletedTrackSnapshot? {
        val index = tracks.indexOfFirst { it.id == trackId }
        if (index < 0) return null
        val track = tracks[index]
        val affectedPlaylists = playlists.filter { trackId in it.trackIds }.map { it.id }

        tracks.removeAt(index)
        recentlyPlayedIds.remove(trackId)

        for (i in playlists.indices) {
            val p = playlists[i]
            if (trackId in p.trackIds) {
                playlists[i] = p.copy(trackIds = p.trackIds - trackId)
            }
        }
        val snapshot = DeletedTrackSnapshot(track, index, affectedPlaylists)
        lastDeleted = snapshot
        save()
        return snapshot
    }

    fun undoDelete(): Track? {
        val snapshot = lastDeleted ?: return null
        val insertIndex = snapshot.originalIndex.coerceIn(0, tracks.size)
        tracks.add(insertIndex, snapshot.track)
        for (pId in snapshot.playlistIds) {
            val idx = playlists.indexOfFirst { it.id == pId }
            if (idx >= 0 && snapshot.track.id !in playlists[idx].trackIds) {
                playlists[idx] = playlists[idx].copy(trackIds = playlists[idx].trackIds + snapshot.track.id)
            }
        }
        lastDeleted = null
        save()
        return snapshot.track
    }

    fun finalizeDelete(track: Track) {
        track.source?.let { AppStorage.deleteFile(it) }
    }

    private val sessionResolvedTrackDurations = mutableSetOf<String>()

    /**
     * Updates track duration from the authoritative audio player and saves to storage.
     * Rules:
     * - Threshold: 1s (|old - new| >= 1000L)
     * - Ignore 0 and unknown (<= 0L or invalid)
     * - At most one save per track per session
     */
    fun updateTrackDuration(trackId: String, durationMs: Long) {
        if (durationMs <= 0L || durationMs >= Long.MAX_VALUE - 1000L) return
        if (sessionResolvedTrackDurations.contains(trackId)) return
        val idx = tracks.indexOfFirst { it.id == trackId }
        if (idx >= 0) {
            val track = tracks[idx]
            if (track.isLiveStream || track.sourceProvider == com.example.musicapp.model.TrackSource.RADIO_BROWSER) return
            val oldDuration = track.durationMs
            if (kotlin.math.abs(oldDuration - durationMs) >= 1000L) {
                tracks[idx] = tracks[idx].copy(durationMs = durationMs)
                sessionResolvedTrackDurations.add(trackId)
                save()
            }
        }
    }

    fun createPlaylist(name: String, description: String = "", coverUrl: String? = null) {
        playlists.add(
            Playlist(
                id = newId(),
                name = name.trim(),
                description = description.trim(),
                coverUrl = coverUrl?.trim()?.takeIf { it.isNotBlank() }
            )
        )
        save()
    }

    fun updatePlaylist(playlistId: String, name: String, description: String, coverUrl: String?) {
        val i = playlists.indexOfFirst { it.id == playlistId }
        if (i >= 0 && playlistId != FAVORITES_PLAYLIST_ID) {
            playlists[i] = playlists[i].copy(
                name = name.trim(),
                description = description.trim(),
                coverUrl = coverUrl?.trim()?.takeIf { it.isNotBlank() }
            )
            save()
        }
    }

    fun movePlaylist(playlistId: String, moveUp: Boolean): Boolean {
        if (playlistId == FAVORITES_PLAYLIST_ID) return false
        val i = playlists.indexOfFirst { it.id == playlistId }
        if (i < 0) return false
        val targetIdx = if (moveUp) i - 1 else i + 1
        // Keep Favorites (index 0) pinned at the top
        if (targetIdx in 1 until playlists.size) {
            val item = playlists.removeAt(i)
            playlists.add(targetIdx, item)
            save()
            return true
        }
        return false
    }

    fun reorderPlaylists(fromIndex: Int, toIndex: Int) {
        if (fromIndex == 0 || toIndex == 0) return // Preserve Favorites at top
        if (fromIndex in playlists.indices && toIndex in playlists.indices) {
            val item = playlists.removeAt(fromIndex)
            playlists.add(toIndex, item)
            save()
        }
    }

    fun deletePlaylist(playlistId: String) {
        if (playlistId == FAVORITES_PLAYLIST_ID) return // System playlist cannot be deleted
        playlists.removeAll { it.id == playlistId }
        save()
    }

    fun addToPlaylist(playlistId: String, trackId: String) {
        val i = playlists.indexOfFirst { it.id == playlistId }
        if (i >= 0 && trackId !in playlists[i].trackIds) {
            playlists[i] = playlists[i].copy(trackIds = playlists[i].trackIds + trackId)
            if (playlistId == FAVORITES_PLAYLIST_ID) {
                val tIdx = tracks.indexOfFirst { it.id == trackId }
                if (tIdx >= 0) tracks[tIdx] = tracks[tIdx].copy(isFavorite = true)
            }
            save()
        }
    }

    fun removeFromPlaylist(playlistId: String, trackId: String) {
        val i = playlists.indexOfFirst { it.id == playlistId }
        if (i >= 0) {
            playlists[i] = playlists[i].copy(trackIds = playlists[i].trackIds - trackId)
            if (playlistId == FAVORITES_PLAYLIST_ID) {
                val tIdx = tracks.indexOfFirst { it.id == trackId }
                if (tIdx >= 0) tracks[tIdx] = tracks[tIdx].copy(isFavorite = false)
            }
            save()
        }
    }

    val visibleTracks: List<Track>
        get() = if (com.example.musicapp.config.AppFeatures.isOnlineSourcesEnabled) {
            tracks.toList()
        } else {
            tracks.filter { it.sourceProvider == com.example.musicapp.model.TrackSource.LOCAL }
        }

    val visiblePlaylists: List<Playlist>
        get() = if (com.example.musicapp.config.AppFeatures.isOnlineSourcesEnabled) {
            playlists.toList()
        } else {
            val visibleIds = visibleTracks.map { it.id }.toSet()
            playlists.filter { pl ->
                pl.isSystem || pl.trackIds.isEmpty() || pl.trackIds.any { it in visibleIds }
            }
        }

    fun tracksIn(playlist: Playlist): List<Track> =
        playlist.trackIds.mapNotNull { id -> tracks.find { it.id == id } }
            .let { list ->
                if (com.example.musicapp.config.AppFeatures.isOnlineSourcesEnabled) list
                else list.filter { it.sourceProvider == com.example.musicapp.model.TrackSource.LOCAL }
            }

    fun getMixOfTheDay(): List<Track> {
        val base = visibleTracks
        if (base.isEmpty()) return emptyList()
        val daySeed = currentTimeMillis() / (1000L * 60 * 60 * 24)
        val rnd = kotlin.random.Random(daySeed)
        return base.shuffled(rnd).take(15)
    }

    fun getRecentlyAdded(limit: Int = 20): List<Track> =
        visibleTracks.sortedByDescending { it.addedAt }.take(limit)

    fun getFrequentlyPlayed(limit: Int = 20): List<Track> =
        visibleTracks.sortedByDescending { it.playCount }.take(limit)

    fun getCloudTracks(): List<Track> =
        if (com.example.musicapp.config.AppFeatures.isOnlineSourcesEnabled) tracks.filter { it.isCloud } else emptyList()

    fun getOfflineDownloadedTracks(): List<Track> =
        if (com.example.musicapp.config.AppFeatures.isOnlineSourcesEnabled) tracks.filter { it.isOfflineAvailable || !it.isCloud } else visibleTracks

    fun getTrackRadio(track: Track): List<Track> {
        val base = visibleTracks
        val sameArtist = base.filter { it.artist.equals(track.artist, ignoreCase = true) && it.id != track.id }
        val remaining = base.filter { it.id != track.id && it !in sameArtist }.shuffled()
        return listOf(track) + sameArtist + remaining.take(15)
    }

    private fun save() {
        val data = LibraryData(tracks.toList(), playlists.toList(), recentlyPlayedIds.toList())
        AppStorage.write(LIBRARY_FILE, json.encodeToString(data))
    }
}

private val sampleTracks = listOf(
    Track("s1", "Midnight Drive", "Retro Wave", "Synth City", 2024, null, 218_000L, 5, 1700000000000L, true),
    Track("s2", "City Lights", "Urban Chill", "Nightscape", 2023, null, 184_000L, 2, 1700000100000L, false),
    Track("s3", "Morning Coffee", "Acoustic Sun", "Sunrise Acoustic", 2024, null, 162_000L, 1, 1700000200000L, false),
)

private val samplePlaylists = listOf(
    Playlist(FAVORITES_PLAYLIST_ID, "Любимые треки", listOf("s1"), isSystem = true),
    Playlist("p1", "Вечерний микс", listOf("s1", "s2")),
)
