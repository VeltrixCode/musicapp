package com.example.musicapp.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.example.musicapp.cache.ArtworkCache
import com.example.musicapp.library.newId
import com.example.musicapp.model.Track
import javafx.application.Platform
import javafx.scene.media.AudioSpectrumListener
import javafx.scene.media.EqualizerBand
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.util.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jetbrains.skia.Image
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import javax.swing.JFileChooser
import javax.swing.SwingUtilities

private val AUDIO_EXTENSIONS = listOf(".mp3", ".wav", ".m4a", ".aac", ".flac", ".ogg")

actual fun createPlayerEngine(): PlayerEngine = JavaFxPlayerEngine()

actual fun runOnUiThread(block: () -> Unit) {
    if (SwingUtilities.isEventDispatchThread()) block() else SwingUtilities.invokeLater(block)
}

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun readTrackInfo(path: String): TrackInfo? = runCatching {
    val file = AudioFileIO.read(File(path))
    val tag = file.tag
    TrackInfo(
        title = tag?.getFirst(FieldKey.TITLE)?.takeIf { it.isNotBlank() },
        artist = tag?.getFirst(FieldKey.ARTIST)?.takeIf { it.isNotBlank() },
        album = tag?.getFirst(FieldKey.ALBUM)?.takeIf { it.isNotBlank() },
        year = tag?.getFirst(FieldKey.YEAR)?.toIntOrNull(),
        durationMs = file.audioHeader.trackLength * 1000L,
    )
}.getOrNull()

actual suspend fun readTrackInfoAsync(path: String): TrackInfo? = withContext(Dispatchers.IO) {
    readTrackInfo(path)
}

actual suspend fun loadArtwork(source: String): ImageBitmap? = withContext(Dispatchers.IO) {
    ArtworkCache.get(source)?.let { return@withContext it }
    runCatching {
        val file = AudioFileIO.read(File(source))
        val raw = file.tag?.firstArtwork?.binaryData ?: return@runCatching null
        val bitmap = Image.makeFromEncoded(raw).toComposeImageBitmap()
        ArtworkCache.put(source, bitmap)
        ArtworkCache.computeAndStoreDominantColor(source, bitmap)
        bitmap
    }.getOrNull()
}

actual fun readLrcContent(audioPath: String): String? {
    val dot = audioPath.lastIndexOf('.')
    if (dot <= 0) return null
    val lrcPath = audioPath.substring(0, dot) + ".lrc"
    val file = File(lrcPath)
    return if (file.exists() && file.isFile) {
        runCatching { file.readText() }.getOrNull()
    } else null
}

actual fun launchAudioImport(onImported: (Track) -> Unit) {
    val dialog = FileDialog(null as Frame?, "Выберите аудиотрек", FileDialog.LOAD)
    dialog.setFilenameFilter { _, name ->
        val lower = name.lowercase()
        AUDIO_EXTENSIONS.any { lower.endsWith(it) }
    }
    dialog.isVisible = true
    val dir = dialog.directory ?: return
    val name = dialog.file ?: return

    val uploads = File(System.getProperty("user.home"), "JVXMusic/uploads").apply { mkdirs() }
    val dest = File(uploads, "${newId()}_$name")
    File(dir, name).copyTo(dest, overwrite = true)
    onImported(importedTrack(dest.absolutePath, name))
}

actual fun launchFolderImport(onImported: (List<Track>) -> Unit) {
    SwingUtilities.invokeLater {
        val chooser = JFileChooser().apply {
            dialogTitle = "Выберите папку с музыкой"
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            isAcceptAllFileFilterUsed = false
        }
        val result = chooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            val selectedDir = chooser.selectedFile ?: return@invokeLater
            val uploads = File(System.getProperty("user.home"), "JVXMusic/uploads").apply { mkdirs() }
            val tracks = mutableListOf<Track>()
            selectedDir.walkTopDown().filter { it.isFile }.forEach { file ->
                val lower = file.name.lowercase()
                if (AUDIO_EXTENSIONS.any { lower.endsWith(it) }) {
                    val dest = File(uploads, "${newId()}_${file.name}")
                    runCatching { file.copyTo(dest, overwrite = true) }
                    tracks.add(importedTrack(dest.absolutePath, file.name))
                }
            }
            if (tracks.isNotEmpty()) {
                runOnUiThread { onImported(tracks) }
            }
        }
    }
}

actual fun prepareStreamUrlForPlayback(trackId: String, streamSource: com.example.musicapp.model.StreamSource): String {
    return if (streamSource.url.startsWith("http://") || streamSource.url.startsWith("https://")) {
        com.example.musicapp.proxy.LocalStreamingProxy.registerStream(trackId, streamSource)
    } else {
        streamSource.url
    }
}

/** Handles external drag-and-drop of audio files into the desktop app. */
fun importLocalAudioFile(sourceFile: File): Track {
    val uploads = File(System.getProperty("user.home"), "JVXMusic/uploads").apply { mkdirs() }
    val dest = File(uploads, "${newId()}_${sourceFile.name}")
    sourceFile.copyTo(dest, overwrite = true)
    return importedTrack(dest.absolutePath, sourceFile.name)
}

private class JavaFxPlayerEngine : PlayerEngine {
    private var player: MediaPlayer? = null
    private var currentVolume = 0.8f
    private var currentRate = 1.0f
    private var pendingSeekMs: Long? = null
    private var spectrumCallback: ((FloatArray) -> Unit)? = null

    // 10-band equalizer gains in dB (-24.0 to +12.0)
    private val equalizerGains = FloatArray(10) { 0f }

    init {
        try {
            Platform.startup {}
        } catch (e: IllegalStateException) {
            // JavaFX toolkit already initialized
        }
    }

    override fun load(path: String, onReady: (Long) -> Unit, onFinished: () -> Unit) {
        Platform.runLater {
            try {
                player?.dispose()
                val isRemote = path.startsWith("http://") || path.startsWith("https://")
                val uri = if (isRemote) path else File(path).toURI().toString()

                // Check for FLAC/OGG which JavaFX Media cannot play natively
                val lower = path.lowercase()
                if (lower.endsWith(".flac") || lower.endsWith(".ogg")) {
                    System.err.println("Предупреждение: Формат FLAC/OGG не поддерживается встроенным движком JavaFX Media.")
                }

                player = MediaPlayer(Media(uri)).apply {
                    volume = currentVolume.toDouble()
                    rate = currentRate.toDouble()

                    // Configure audio spectrum listener (64 bands)
                    audioSpectrumNumBands = 64
                    audioSpectrumInterval = 0.04 // 25 updates per second
                    audioSpectrumThreshold = -60

                    audioSpectrumListener = AudioSpectrumListener { _, _, magnitudes, _ ->
                        val listener = spectrumCallback
                        if (listener != null && magnitudes != null && magnitudes.isNotEmpty()) {
                            val norm = FloatArray(magnitudes.size) { i ->
                                // magnitudes are in dB from threshold (-60 dB) to 0 dB
                                ((magnitudes[i] + 60f) / 60f).coerceIn(0f, 1f)
                            }
                            runOnUiThread { listener(norm) }
                        }
                    }

                    setOnReady {
                        val d = media?.duration?.toMillis()?.takeIf { !it.isNaN() && it > 0 }?.toLong() ?: 0L
                        // Apply pending seek if requested before ready
                        pendingSeekMs?.let { ms ->
                            seek(Duration.millis(ms.toDouble()))
                            pendingSeekMs = null
                        }
                        // Setup equalizer bands
                        applyEqualizer()
                        runOnUiThread { onReady(d) }
                    }

                    setOnEndOfMedia {
                        runOnUiThread { onFinished() }
                    }

                    setOnError {
                        System.err.println("MediaPlayer error: " + error?.message)
                    }
                }
            } catch (e: Exception) {
                System.err.println("Failed to load audio: " + e.message)
            }
        }
    }

    private fun applyEqualizer() {
        val p = player ?: return
        val eq = p.audioEqualizer ?: return
        eq.isEnabled = true
        val bands = eq.bands
        for (i in equalizerGains.indices) {
            if (i < bands.size) {
                bands[i].gain = equalizerGains[i].toDouble().coerceIn(EqualizerBand.MIN_GAIN, EqualizerBand.MAX_GAIN)
            }
        }
    }

    override fun play() = Platform.runLater { player?.play() }
    override fun pause() = Platform.runLater { player?.pause() }

    override fun seekTo(ms: Long) {
        Platform.runLater {
            val p = player
            if (p != null && p.status != MediaPlayer.Status.UNKNOWN && p.status != MediaPlayer.Status.DISPOSED) {
                p.seek(Duration.millis(ms.toDouble()))
                pendingSeekMs = null
            } else {
                pendingSeekMs = ms
            }
        }
    }

    override fun setVolume(volume: Float) {
        currentVolume = volume
        Platform.runLater { player?.volume = volume.toDouble().coerceIn(0.0, 1.0) }
    }

    override fun setPlaybackSpeed(speed: Float) {
        currentRate = speed.coerceIn(0.25f, 3.0f)
        Platform.runLater { player?.rate = currentRate.toDouble() }
    }

    override fun setEqualizerBand(bandIndex: Int, gainDb: Float) {
        if (bandIndex in equalizerGains.indices) {
            equalizerGains[bandIndex] = gainDb
            Platform.runLater { applyEqualizer() }
        }
    }

    override fun setSpectrumListener(listener: ((FloatArray) -> Unit)?) {
        spectrumCallback = listener
    }

    override fun positionMs(): Long =
        player?.currentTime?.toMillis()?.takeIf { !it.isNaN() && it > 0 }?.toLong() ?: 0L

    override fun durationMs(): Long =
        player?.media?.duration?.toMillis()?.takeIf { !it.isNaN() && it > 0 }?.toLong() ?: 0L

    override fun release() = Platform.runLater {
        player?.dispose()
        player = null
        pendingSeekMs = null
    }
}

actual fun getEnv(name: String): String? = System.getenv(name) ?: System.getProperty(name)

