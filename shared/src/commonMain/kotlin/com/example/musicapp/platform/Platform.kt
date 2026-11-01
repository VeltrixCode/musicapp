package com.example.musicapp.platform

import androidx.compose.ui.graphics.ImageBitmap
import com.example.musicapp.library.newId
import com.example.musicapp.model.Track

interface PlayerEngine {
    val supportsSpectrum: Boolean get() = false
    val supportsPreamp: Boolean get() = false
    val engineName: String get() = "Default"
    val engineStatusText: String get() = "Движок: $engineName"
    val isFallback: Boolean get() = false
    fun getEqualizerPreamp(): Float = 0f
    fun setEqualizerPreamp(gainDb: Float) {}

    fun load(path: String, onReady: (durationMs: Long) -> Unit, onFinished: () -> Unit)
    fun load(path: String, onReady: (durationMs: Long) -> Unit, onFinished: () -> Unit, onError: ((errorMessage: String) -> Unit)?) {
        load(path, onReady, onFinished)
    }
    fun play()
    fun pause()
    fun seekTo(ms: Long)
    fun positionMs(): Long
    fun durationMs(): Long
    fun isPlaying(): Boolean = false
    fun setVolume(volume: Float)
    fun setPlaybackSpeed(speed: Float)
    fun setEqualizerBand(bandIndex: Int, gainDb: Float)
    fun setEqualizerEnabled(enabled: Boolean) {}
    fun getEqualizerBandCount(): Int = 10
    fun getEqualizerGainRange(): ClosedFloatingPointRange<Float> = -24f..12f
    fun getEqualizerBandFrequencies(): List<Int> = listOf(32, 64, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
    fun setSpectrumListener(listener: ((FloatArray) -> Unit)?)
    fun release()
}

/** Platform audio engine: MediaPlayer on Android, JavaFX MediaPlayer on desktop. */
expect fun createPlayerEngine(): PlayerEngine

/** Runs [block] on the platform's UI thread. Audio callbacks use this before touching state. */
expect fun runOnUiThread(block: () -> Unit)

expect fun currentTimeMillis(): Long

data class TrackInfo(
    val title: String?,
    val artist: String?,
    val album: String?,
    val genre: String? = null,
    val year: Int?,
    val durationMs: Long,
)

/** Reads ID3 / container tags and duration from an audio file synchronously. Null if unreadable. */
expect fun readTrackInfo(path: String): TrackInfo?

/** Reads ID3 / container tags and duration from an audio file asynchronously on IO dispatcher. */
expect suspend fun readTrackInfoAsync(path: String): TrackInfo?

/** Reads embedded artwork from an audio file asynchronously on IO dispatcher. Null if absent. */
expect suspend fun loadArtwork(source: String): ImageBitmap?

/** Decodes raw image bytes (JPEG, PNG, WebP) into an ImageBitmap. Null if unreadable. */
expect fun decodeImageBytes(bytes: ByteArray): ImageBitmap?

/** Reads text of .lrc file if present alongside audio file (e.g. song.lrc). Null if absent. */
expect fun readLrcContent(audioPath: String): String?

/** Opens the platform file picker, copies the chosen song into app storage, and reports the new Track. */
expect fun launchAudioImport(onImported: (Track) -> Unit)

/** Opens folder picker on desktop or file picker to import multiple tracks with full tags. */
expect fun launchFolderImport(onImported: (List<Track>) -> Unit)

/** Prepares a stream URL for playback on the platform engine (e.g. desktop local proxy for auth headers). */
expect fun prepareStreamUrlForPlayback(trackId: String, streamSource: com.example.musicapp.model.StreamSource): String

/** Notifies the platform streaming layer of a track change, allowing session cleanup. */
expect fun onPlaybackTrackChanged(previousTrackId: String?, newTrackId: String?)

/** Notifies the platform streaming layer that playback has released/stopped. */
expect fun onPlaybackReleased()

expect fun getEnv(name: String): String?

/** Checks whether local or content URI file is currently accessible on the device/filesystem. */
expect fun isFileAvailable(source: String?): Boolean

expect inline fun <R> synchronizedPlatform(lock: Any, block: () -> R): R


/** Builds a Track from a copied file, using its tags when present and the file name otherwise. */
fun importedTrack(path: String, fileName: String, info: TrackInfo? = readTrackInfo(path)): Track {
    return Track(
        id = newId(),
        title = info?.title ?: fileName.substringBeforeLast('.'),
        artist = info?.artist ?: "Неизвестный исполнитель",
        album = info?.album ?: "",
        genre = info?.genre,
        year = info?.year,
        source = path,
        durationMs = info?.durationMs ?: 0L,
        addedAt = currentTimeMillis(),
    )
}
