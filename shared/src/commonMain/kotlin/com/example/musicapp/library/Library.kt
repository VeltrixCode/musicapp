package com.example.musicapp.library

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.musicapp.model.LibraryData
import com.example.musicapp.model.Playlist
import com.example.musicapp.model.Track
import com.example.musicapp.platform.AppStorage
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
private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

fun newId(): String = Random.nextLong().toString(16)

enum class SortColumn { Number, Title, Artist, Duration }
enum class SortDirection { Ascending, Descending }

data class DeletedTrackSnapshot(
    val track: Track,
    val originalIndex: Int,
    val playlistIds: List<String>,
)

/**
 * The user's tracks and playlists with persistence, asynchronous metadata enrichment,
 * sorting, and undo support.
 */
class Library {
    val tracks = mutableStateListOf<Track>()
    val playlists = mutableStateListOf<Playlist>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    var lastDeleted by mutableStateOf<DeletedTrackSnapshot?>(null)
        private set

    init {
        val raw = AppStorage.read(LIBRARY_FILE)
        val saved = raw?.let { runCatching { json.decodeFromString<LibraryData>(it) }.getOrNull() }
        if (saved != null) {
            tracks.addAll(saved.tracks)
            playlists.addAll(saved.playlists)
        } else {
            if (raw != null) AppStorage.write("$LIBRARY_FILE.bak", raw)
            tracks.addAll(sampleTracks)
            playlists.addAll(samplePlaylists)
            save()
        }

        // Asynchronously enrich missing durations for local files in Dispatchers.IO
        scope.launch {
            for (i in tracks.indices) {
                val t = tracks.getOrNull(i) ?: continue
                if (t.durationMs <= 0L && t.source != null) {
                    val info = readTrackInfoAsync(t.source)
                    if (info != null && info.durationMs > 0L) {
                        val currentIdx = tracks.indexOfFirst { it.id == t.id }
                        if (currentIdx >= 0) {
                            tracks[currentIdx] = t.copy(
                                durationMs = info.durationMs,
                                title = if (t.title == "Unknown" || t.title.isBlank()) (info.title ?: t.title) else t.title,
                                artist = if (t.artist == "Unknown artist") (info.artist ?: t.artist) else t.artist,
                            )
                        }
                    }
                }
            }
            save()
        }
    }

    fun addTrack(track: Track) {
        tracks.add(0, track)
        save()
    }

    /**
     * Removes the track from the library and records a snapshot for undo.
     * The actual file deletion on disk is deferred or can be reverted.
     */
    fun removeTrack(trackId: String): DeletedTrackSnapshot? {
        val index = tracks.indexOfFirst { it.id == trackId }
        if (index < 0) return null
        val track = tracks[index]
        val affectedPlaylists = playlists.filter { trackId in it.trackIds }.map { it.id }

        tracks.removeAt(index)
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

    /** Restores the last deleted track back to its original position and playlists. */
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

    fun createPlaylist(name: String) {
        playlists.add(Playlist(newId(), name.trim()))
        save()
    }

    fun deletePlaylist(playlistId: String) {
        playlists.removeAll { it.id == playlistId }
        save()
    }

    fun addToPlaylist(playlistId: String, trackId: String) {
        val i = playlists.indexOfFirst { it.id == playlistId }
        if (i >= 0 && trackId !in playlists[i].trackIds) {
            playlists[i] = playlists[i].copy(trackIds = playlists[i].trackIds + trackId)
            save()
        }
    }

    fun removeFromPlaylist(playlistId: String, trackId: String) {
        val i = playlists.indexOfFirst { it.id == playlistId }
        if (i >= 0) {
            playlists[i] = playlists[i].copy(trackIds = playlists[i].trackIds - trackId)
            save()
        }
    }

    fun tracksIn(playlist: Playlist): List<Track> =
        playlist.trackIds.mapNotNull { id -> tracks.find { it.id == id } }

    private fun save() {
        val data = LibraryData(tracks.toList(), playlists.toList())
        AppStorage.write(LIBRARY_FILE, json.encodeToString(data))
    }
}

private val sampleTracks = listOf(
    Track("s1", "Midnight Drive", "Retro Wave", null, 218_000L),
    Track("s2", "City Lights", "Urban Chill", null, 184_000L),
    Track("s3", "Morning Coffee", "Acoustic Sun", null, 162_000L),
)

private val samplePlaylists = listOf(
    Playlist("p1", "Избранное", listOf("s1", "s3")),
)
