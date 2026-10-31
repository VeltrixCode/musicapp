package com.example.musicapp.platform

import com.example.musicapp.library.newId
import com.example.musicapp.model.Track
import javafx.application.Platform
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.util.Duration
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import javax.swing.SwingUtilities

private val AUDIO_EXTENSIONS = listOf(".mp3", ".wav", ".m4a", ".aac")

actual fun createPlayerEngine(): PlayerEngine = JavaFxPlayerEngine()

actual fun runOnUiThread(block: () -> Unit) {
    if (SwingUtilities.isEventDispatchThread()) block() else SwingUtilities.invokeLater(block)
}

actual fun readTrackInfo(path: String): TrackInfo? = runCatching {
    val file = AudioFileIO.read(File(path))
    val tag = file.tag
    TrackInfo(
        title = tag?.getFirst(FieldKey.TITLE)?.takeIf { it.isNotBlank() },
        artist = tag?.getFirst(FieldKey.ARTIST)?.takeIf { it.isNotBlank() },
        durationMs = file.audioHeader.trackLength * 1000L,
    )
}.getOrNull()

actual fun launchAudioImport(onImported: (Track) -> Unit) {
    val dialog = FileDialog(null as Frame?, "Choose a song", FileDialog.LOAD)
    dialog.setFilenameFilter { _, name ->
        val lower = name.lowercase()
        AUDIO_EXTENSIONS.any { lower.endsWith(it) }
    }
    dialog.isVisible = true
    val dir = dialog.directory ?: return
    val name = dialog.file ?: return

    val uploads = File(System.getProperty("user.home"), "MusicApp/uploads").apply { mkdirs() }
    val dest = File(uploads, "${newId()}_$name")
    File(dir, name).copyTo(dest)
    onImported(importedTrack(dest.absolutePath, name))
}

private class JavaFxPlayerEngine : PlayerEngine {
    private var player: MediaPlayer? = null
    private var currentVolume = 0.8f

    init {
        try {
            Platform.startup {}
        } catch (e: IllegalStateException) {
            // JavaFX toolkit already started
        }
    }

    override fun load(path: String, onFinished: () -> Unit) {
        Platform.runLater {
            try {
                player?.dispose()
                val uri = File(path).toURI().toString()
                player = MediaPlayer(Media(uri)).apply {
                    volume = currentVolume.toDouble()
                    setOnEndOfMedia { runOnUiThread { onFinished() } }
                    setOnError {
                        System.err.println("MediaPlayer error: " + error?.message)
                    }
                }
            } catch (e: Exception) {
                System.err.println("Failed to load audio: " + e.message)
            }
        }
    }

    override fun play() = Platform.runLater { player?.play() }
    override fun pause() = Platform.runLater { player?.pause() }
    override fun seekTo(ms: Long) = Platform.runLater { player?.seek(Duration.millis(ms.toDouble())) }

    override fun setVolume(volume: Float) {
        currentVolume = volume
        Platform.runLater { player?.volume = volume.toDouble().coerceIn(0.0, 1.0) }
    }

    override fun positionMs(): Long =
        player?.currentTime?.toMillis()?.takeIf { !it.isNaN() && it > 0 }?.toLong() ?: 0L

    override fun durationMs(): Long =
        player?.media?.duration?.toMillis()?.takeIf { !it.isNaN() && it > 0 }?.toLong() ?: 0L

    override fun release() = Platform.runLater {
        player?.dispose()
        player = null
    }
}
