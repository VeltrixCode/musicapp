package com.example.musicapp.library

import androidx.compose.runtime.mutableStateListOf
import com.example.musicapp.model.LibraryData
import com.example.musicapp.model.Playlist
import com.example.musicapp.model.Track
import com.example.musicapp.platform.AppStorage
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random

private const val LIBRARY_FILE = "library.json"
private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

fun newId(): String = Random.nextLong().toString(16)

/**
 * The user's tracks and playlists. Every change is written to disk immediately,
 * and the saved state is loaded on launch.
 */
class Library {
    val tracks = mutableStateListOf<Track>()
    val playlists = mutableStateListOf<Playlist>()

    init {
        val raw = AppStorage.read(LIBRARY_FILE)
        val saved = raw?.let { runCatching { json.decodeFromString<LibraryData>(it) }.getOrNull() }
        if (saved != null) {
            tracks.addAll(saved.tracks)
            playlists.addAll(saved.playlists)
        } else {
            if (raw != null) AppStorage.write("$LIBRARY_FILE.bak", raw) // keep unreadable file
            tracks.addAll(sampleTracks)
            playlists.addAll(samplePlaylists)
            save()
        }
    }

    fun addTrack(track: Track) {
        tracks.add(0, track)
        save()
    }

    /** Removes the track from the library and every playlist. Deletes its uploaded file, if any. */
    fun removeTrack(trackId: String) {
        val track = tracks.find { it.id == trackId } ?: return
        tracks.remove(track)
        for (i in playlists.indices) {
            val p = playlists[i]
            if (trackId in p.trackIds) playlists[i] = p.copy(trackIds = p.trackIds - trackId)
        }
        track.source?.let { AppStorage.deleteFile(it) }
        save()
    }

    fun createPlaylist(name: String) {
        playlists.add(Playlist(newId(), name))
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

    /** Takes the track out of one playlist. The track stays in the library. */
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
    Track("s1", "Midnight Drive", "Sample Artist"),
    Track("s2", "City Lights", "Sample Artist"),
    Track("s3", "Morning Coffee", "Demo Band"),
)

private val samplePlaylists = listOf(
    Playlist("p1", "Favorites", listOf("s1", "s3")),
    Playlist("p2", "Late Night", listOf("s2")),
)
