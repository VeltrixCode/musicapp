package com.example.musicapp.platform

import com.example.musicapp.library.newId
import com.example.musicapp.model.Track

interface PlayerEngine {
    fun load(path: String, onFinished: () -> Unit)
    fun play()
    fun pause()
    fun seekTo(ms: Long)
    fun positionMs(): Long
    fun durationMs(): Long
    fun release()
}

/** Platform audio engine: MediaPlayer on Android, JavaFX MediaPlayer on desktop. */
expect fun createPlayerEngine(): PlayerEngine

/** Runs [block] on the platform's UI thread. Audio callbacks use this before touching state. */
expect fun runOnUiThread(block: () -> Unit)

data class TrackInfo(val title: String?, val artist: String?, val durationMs: Long)

/** Reads ID3 / container tags and duration from an audio file. Null if unreadable. */
expect fun readTrackInfo(path: String): TrackInfo?

/** Opens the platform file picker, copies the chosen song into app storage, and reports the new Track. */
expect fun launchAudioImport(onImported: (Track) -> Unit)

/** Builds a Track from a copied file, using its tags when present and the file name otherwise. */
fun importedTrack(path: String, fileName: String): Track {
    val info = readTrackInfo(path)
    return Track(
        id = newId(),
        title = info?.title ?: fileName.substringBeforeLast('.'),
        artist = info?.artist ?: "Unknown artist",
        source = path,
        durationMs = info?.durationMs ?: 0L,
    )
}
