package com.example.musicapp.platform

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import com.example.musicapp.library.newId
import com.example.musicapp.model.Track
import java.io.File

/** Set by the app at startup: the context and the picker launcher registered in MainActivity. */
object AndroidBridge {
    lateinit var appContext: Context
    var pickAudio: (((Uri?) -> Unit) -> Unit)? = null
}

private val mainHandler = Handler(Looper.getMainLooper())

actual fun createPlayerEngine(): PlayerEngine = AndroidPlayerEngine()

actual fun runOnUiThread(block: () -> Unit) {
    if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post { block() }
}

actual fun readTrackInfo(path: String): TrackInfo? = runCatching {
    val r = MediaMetadataRetriever()
    try {
        r.setDataSource(path)
        TrackInfo(
            title = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() },
            artist = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.takeIf { it.isNotBlank() },
            durationMs = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L,
        )
    } finally {
        r.release()
    }
}.getOrNull()

actual fun launchAudioImport(onImported: (Track) -> Unit) {
    val picker = AndroidBridge.pickAudio ?: return
    picker { uri -> if (uri != null) importFromUri(uri, onImported) }
}

private fun importFromUri(uri: Uri, onImported: (Track) -> Unit) {
    val ctx = AndroidBridge.appContext
    val name = ctx.contentResolver
        .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        ?: "Untitled"
    val dir = File(ctx.filesDir, "music").apply { mkdirs() }
    val dest = File(dir, "${newId()}_$name")
    ctx.contentResolver.openInputStream(uri)?.use { input ->
        dest.outputStream().use { input.copyTo(it) }
    }
    onImported(importedTrack(dest.absolutePath, name))
}

private class AndroidPlayerEngine : PlayerEngine {
    private var player: MediaPlayer? = null

    override fun load(path: String, onFinished: () -> Unit) {
        release()
        player = MediaPlayer().apply {
            setDataSource(path)
            prepare()
            setOnCompletionListener { onFinished() }
        }
    }

    override fun play() { runCatching { player?.start() } }
    override fun pause() { runCatching { player?.pause() } }
    override fun seekTo(ms: Long) { runCatching { player?.seekTo(ms.toInt()) } }

    override fun setVolume(volume: Float) {
        val v = volume.coerceIn(0f, 1f)
        runCatching { player?.setVolume(v, v) }
    }

    override fun positionMs(): Long =
        runCatching { player?.currentPosition?.toLong() ?: 0L }.getOrDefault(0L)

    override fun durationMs(): Long =
        runCatching { (player?.duration?.toLong() ?: 0L).coerceAtLeast(0L) }.getOrDefault(0L)

    override fun release() {
        runCatching { player?.release() }
        player = null
    }
}
