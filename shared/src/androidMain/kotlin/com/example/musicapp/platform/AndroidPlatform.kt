package com.example.musicapp.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.media.audiofx.Equalizer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.musicapp.cache.ArtworkCache
import com.example.musicapp.library.newId
import com.example.musicapp.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Set by the app at startup: the context and the picker launcher registered in MainActivity. */
object AndroidBridge {
    lateinit var appContext: Context
    var pickAudio: (((Uri?) -> Unit) -> Unit)? = null
    var onHeadphonesUnplugged: (() -> Unit)? = null
}

private val mainHandler = Handler(Looper.getMainLooper())

actual fun createPlayerEngine(): PlayerEngine = AndroidPlayerEngine()

actual fun runOnUiThread(block: () -> Unit) {
    if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post { block() }
}

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun readTrackInfo(path: String): TrackInfo? = runCatching {
    val r = MediaMetadataRetriever()
    try {
        r.setDataSource(path)
        TrackInfo(
            title = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() },
            artist = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.takeIf { it.isNotBlank() },
            album = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.takeIf { it.isNotBlank() },
            year = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)?.toIntOrNull(),
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
    ArtworkCache.get(source)?.let { return@withContext it }
    runCatching {
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(source)
            val pic = r.embeddedPicture ?: return@runCatching null
            val bitmap = BitmapFactory.decodeByteArray(pic, 0, pic.size)?.asImageBitmap() ?: return@runCatching null
            ArtworkCache.put(source, bitmap)
            ArtworkCache.computeAndStoreDominantColor(source, bitmap)
            bitmap
        } finally {
            r.release()
        }
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
    val picker = AndroidBridge.pickAudio ?: return
    picker { uri -> if (uri != null) importFromUri(uri, onImported) }
}

actual fun launchFolderImport(onImported: (List<Track>) -> Unit) {
    launchAudioImport { track -> onImported(listOf(track)) }
}

actual fun prepareStreamUrlForPlayback(trackId: String, streamSource: com.example.musicapp.model.StreamSource): String {
    return streamSource.url
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
    private var equalizer: Equalizer? = null
    private val audioManager = AndroidBridge.appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    private var pendingSeekMs: Long? = null
    private var currentSpeed = 1.0f
    private var currentVolume = 0.8f

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                pause()
                AndroidBridge.onHeadphonesUnplugged?.invoke()
            }
        }
    }
    private var isNoisyRegistered = false

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> pause()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pause()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> runCatching { player?.setVolume(0.2f, 0.2f) }
            AudioManager.AUDIOFOCUS_GAIN -> {
                runCatching { player?.setVolume(currentVolume, currentVolume) }
                play()
            }
        }
    }

    private fun registerNoisyReceiver() {
        if (!isNoisyRegistered) {
            try {
                AndroidBridge.appContext.registerReceiver(
                    noisyReceiver,
                    IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                )
                isNoisyRegistered = true
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun unregisterNoisyReceiver() {
        if (isNoisyRegistered) {
            try {
                AndroidBridge.appContext.unregisterReceiver(noisyReceiver)
                isNoisyRegistered = false
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        registerNoisyReceiver()
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
        unregisterNoisyReceiver()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusChangeListener)
        }
    }

    override fun load(path: String, onReady: (Long) -> Unit, onFinished: () -> Unit) {
        release()
        try {
            val p = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(path)
                setOnPreparedListener { mp ->
                    val d = mp.duration.toLong().coerceAtLeast(0L)
                    pendingSeekMs?.let { ms ->
                        mp.seekTo(ms.toInt())
                        pendingSeekMs = null
                    }
                    applySpeed()
                    setupEqualizer(mp.audioSessionId)
                    onReady(d)
                }
                setOnCompletionListener { onFinished() }
                setOnSeekCompleteListener {
                    // Seek is officially finished on hardware
                    pendingSeekMs = null
                }
                prepareAsync()
            }
            player = p
        } catch (e: Exception) {
            System.err.println("Android MediaPlayer load error: " + e.message)
        }
    }

    private fun setupEqualizer(audioSessionId: Int) {
        try {
            equalizer?.release()
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = true
            }
        } catch (e: Exception) {
            // Ignore if equalizer permission or device limitation
        }
    }

    private fun applySpeed() {
        runCatching {
            val p = player ?: return
            val params = p.playbackParams
            p.playbackParams = params.setSpeed(currentSpeed)
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
        val p = player
        if (p != null) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    p.seekTo(ms, MediaPlayer.SEEK_CLOSEST)
                } else {
                    p.seekTo(ms.toInt())
                }
            }
        } else {
            pendingSeekMs = ms
        }
    }

    override fun setVolume(volume: Float) {
        currentVolume = volume.coerceIn(0f, 1f)
        runCatching { player?.setVolume(currentVolume, currentVolume) }
    }

    override fun setPlaybackSpeed(speed: Float) {
        currentSpeed = speed.coerceIn(0.25f, 3.0f)
        applySpeed()
    }

    override fun setEqualizerBand(bandIndex: Int, gainDb: Float) {
        try {
            val eq = equalizer ?: return
            if (bandIndex in 0 until eq.numberOfBands.toInt()) {
                val range = eq.bandLevelRange
                val minLevel = range[0]
                val maxLevel = range[1]
                val level = (gainDb * 100).toInt().coerceIn(minLevel.toInt(), maxLevel.toInt()).toShort()
                eq.setBandLevel(bandIndex.toShort(), level)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    override fun setSpectrumListener(listener: ((FloatArray) -> Unit)?) {
        // Optional Visualizer or simulated spectrum
    }

    override fun positionMs(): Long =
        runCatching { player?.currentPosition?.toLong() ?: 0L }.getOrDefault(0L)

    override fun durationMs(): Long =
        runCatching { (player?.duration?.toLong() ?: 0L).coerceAtLeast(0L) }.getOrDefault(0L)

    override fun release() {
        abandonAudioFocus()
        runCatching {
            equalizer?.release()
            equalizer = null
            player?.release()
            player = null
        }
        pendingSeekMs = null
    }
}
