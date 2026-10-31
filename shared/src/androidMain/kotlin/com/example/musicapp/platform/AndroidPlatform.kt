package com.example.musicapp.platform

import android.content.Context
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.musicapp.library.newId
import com.example.musicapp.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

actual suspend fun readTrackInfoAsync(path: String): TrackInfo? = withContext(Dispatchers.IO) {
    readTrackInfo(path)
}

actual suspend fun loadArtwork(source: String): ImageBitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(source)
            val pic = r.embeddedPicture ?: return@runCatching null
            BitmapFactory.decodeByteArray(pic, 0, pic.size)?.asImageBitmap()
        } finally {
            r.release()
        }
    }.getOrNull()
}

actual fun launchAudioImport(onImported: (Track) -> Unit) {
    val picker = AndroidBridge.pickAudio ?: return
    picker { uri -> if (uri != null) importFromUri(uri, onImported) }
}

private fun importFromUri(uri: Uri, onImported: (Track) -> Unit) {
    val ctx = AndroidBridge.appContext
    val name = ctx.contentResolver
        .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        ?: "Аудиотрек"
    val dir = File(ctx.filesDir, "music").apply { mkdirs() }
    val dest = File(dir, "${newId()}_$name")
    ctx.contentResolver.openInputStream(uri)?.use { input ->
        dest.outputStream().use { input.copyTo(it) }
    }
    onImported(importedTrack(dest.absolutePath, name))
}

private class AndroidPlayerEngine : PlayerEngine {
    private var player: MediaPlayer? = null
    private val audioManager = AndroidBridge.appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> pause()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pause()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> runCatching { player?.setVolume(0.2f, 0.2f) }
            AudioManager.AUDIOFOCUS_GAIN -> {
                runCatching { player?.setVolume(1.0f, 1.0f) }
                play()
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(focusChangeListener)
                .build()
            audioFocusRequest = request
            audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                focusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusChangeListener)
        }
    }

    override fun load(path: String, onFinished: () -> Unit) {
        release()
        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setDataSource(path)
            prepare()
            setOnCompletionListener { onFinished() }
        }
    }

    override fun play() {
        if (requestAudioFocus()) {
            runCatching { player?.start() }
        }
    }

    override fun pause() {
        runCatching { player?.pause() }
        abandonAudioFocus()
    }

    override fun seekTo(ms: Long) {
        runCatching { player?.seekTo(ms.toInt()) }
    }

    override fun setVolume(volume: Float) {
        val v = volume.coerceIn(0f, 1f)
        runCatching { player?.setVolume(v, v) }
    }

    override fun positionMs(): Long =
        runCatching { player?.currentPosition?.toLong() ?: 0L }.getOrDefault(0L)

    override fun durationMs(): Long =
        runCatching { (player?.duration?.toLong() ?: 0L).coerceAtLeast(0L) }.getOrDefault(0L)

    override fun release() {
        abandonAudioFocus()
        runCatching { player?.release() }
        player = null
    }
}
