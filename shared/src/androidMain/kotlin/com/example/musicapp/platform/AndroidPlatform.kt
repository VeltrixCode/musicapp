package com.example.musicapp.platform

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.audiofx.Equalizer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.media3.common.AudioAttributes as Media3AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.musicapp.cache.ArtworkCache
import com.example.musicapp.config.AppConfig
import com.example.musicapp.library.newId
import com.example.musicapp.model.StreamSource
import com.example.musicapp.model.Track
import com.example.musicapp.oauth.GoogleOAuthAndroid
import com.example.musicapp.security.SecureStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

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
            genre = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)?.takeIf { it.isNotBlank() },
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

actual fun decodeImageBytes(bytes: ByteArray): ImageBitmap? = runCatching {
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
}.getOrNull()

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

object AndroidStreamRegistry {
    private val streams = ConcurrentHashMap<String, StreamSource>()
    private val trackStreams = ConcurrentHashMap<String, CopyOnWriteArraySet<String>>()

    fun register(trackId: String, streamSource: StreamSource): String {
        val url = streamSource.url
        streams[url] = streamSource
        trackStreams.computeIfAbsent(trackId) { CopyOnWriteArraySet() }.add(url)
        return url
    }

    fun getStreamSource(url: String): StreamSource? = streams[url]

    fun invalidateTrack(trackId: String) {
        val urls = trackStreams.remove(trackId) ?: return
        for (u in urls) {
            streams.remove(u)
        }
    }

    fun clear() {
        streams.clear()
        trackStreams.clear()
    }
}

actual fun prepareStreamUrlForPlayback(trackId: String, streamSource: StreamSource): String {
    return AndroidStreamRegistry.register(trackId, streamSource)
}

actual fun onPlaybackTrackChanged(previousTrackId: String?, newTrackId: String?) {
    if (previousTrackId != null && previousTrackId != newTrackId) {
        AndroidStreamRegistry.invalidateTrack(previousTrackId)
    }
}

actual fun onPlaybackReleased() {
    AndroidStreamRegistry.clear()
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

private val streamingOkHttpClient by lazy {
    OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            val urlString = original.url.toString()
            val streamSource = AndroidStreamRegistry.getStreamSource(urlString)

            val reqBuilder = original.newBuilder()

            // 1. Inject Authorization header if stream source has headers
            if (streamSource != null) {
                streamSource.headers.forEach { (k, v) ->
                    if (original.header(k) == null) {
                        reqBuilder.header(k, v)
                    }
                }
            } else if (urlString.contains("googleapis.com/drive")) {
                val gtoken = SecureStorage.getSecret(AppConfig.TOKEN_GDRIVE_ACCESS)
                if (!gtoken.isNullOrBlank() && original.header("Authorization") == null) {
                    reqBuilder.header("Authorization", "Bearer $gtoken")
                }
            } else if (urlString.contains("yandex.net")) {
                val ytoken = SecureStorage.getSecret(AppConfig.TOKEN_YANDEX_ACCESS)
                if (!ytoken.isNullOrBlank() && original.header("Authorization") == null) {
                    reqBuilder.header("Authorization", "OAuth $ytoken")
                }
            }

            var response = chain.proceed(reqBuilder.build())

            // 2. Upstream 401/403/410 token refresh & retry (Yandex Disk & Google Drive)
            if (response.code in listOf(401, 403, 410)) {
                if (urlString.contains("googleapis.com") && response.code == 401) {
                    val refreshedToken = kotlinx.coroutines.runBlocking {
                        GoogleOAuthAndroid.refreshAccessToken().getOrNull()
                    }
                    if (!refreshedToken.isNullOrBlank()) {
                        response.close()
                        val retryReq = original.newBuilder()
                            .header("Authorization", "Bearer $refreshedToken")
                            .build()
                        response = chain.proceed(retryReq)
                    }
                } else if (urlString.contains("yandex.net") || urlString.contains("disk.yandex")) {
                    val ytoken = SecureStorage.getSecret(AppConfig.TOKEN_YANDEX_ACCESS)
                    if (!ytoken.isNullOrBlank()) {
                        response.close()
                        val retryReq = original.newBuilder()
                            .header("Authorization", "OAuth $ytoken")
                            .build()
                        response = chain.proceed(retryReq)
                    }
                }
            }

            response
        }
        .build()
}

/**
 * Media3 ExoPlayer engine with OkHttpDataSource, Authorization injection,
 * and seamless retry on 401/403/410.
 */
private class AndroidPlayerEngine : PlayerEngine {
    private var exoPlayer: ExoPlayer? = null
    private var equalizer: Equalizer? = null
    private var pendingSeekMs: Long? = null
    private var currentSpeed = 1.0f
    private var currentVolume = 0.8f
    private var onReadyCallback: ((Long) -> Unit)? = null
    private var onFinishedCallback: (() -> Unit)? = null

    private fun initPlayer(): ExoPlayer {
        val ctx = AndroidBridge.appContext
        val dataSourceFactory = OkHttpDataSource.Factory(streamingOkHttpClient)
        val mediaSourceFactory = DefaultMediaSourceFactory(ctx)
            .setDataSourceFactory(dataSourceFactory)

        val media3AudioAttributes = Media3AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        return ExoPlayer.Builder(ctx)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(media3AudioAttributes, /* handleAudioFocus = */ true)
            .build().apply {
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_READY -> {
                                val d = duration.coerceAtLeast(0L)
                                pendingSeekMs?.let { ms ->
                                    seekTo(ms)
                                    pendingSeekMs = null
                                }
                                setupEqualizer(audioSessionId)
                                onReadyCallback?.invoke(d)
                            }
                            Player.STATE_ENDED -> {
                                onFinishedCallback?.invoke()
                            }
                            Player.STATE_IDLE, Player.STATE_BUFFERING -> {}
                        }
                    }
                })
            }
    }

    override fun load(path: String, onReady: (Long) -> Unit, onFinished: () -> Unit) {
        release()
        onReadyCallback = onReady
        onFinishedCallback = onFinished
        try {
            val p = initPlayer()
            exoPlayer = p
            p.volume = currentVolume
            p.playbackParameters = PlaybackParameters(currentSpeed)

            val uri = if (path.startsWith("http://") || path.startsWith("https://") || path.startsWith("content://")) {
                Uri.parse(path)
            } else {
                Uri.fromFile(File(path))
            }

            val mediaItem = MediaItem.fromUri(uri)
            p.setMediaItem(mediaItem)
            p.prepare()
        } catch (e: Exception) {
            System.err.println("Android ExoPlayer load error: " + e.message)
        }
    }

    private fun setupEqualizer(audioSessionId: Int) {
        try {
            if (audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                equalizer?.release()
                equalizer = Equalizer(0, audioSessionId).apply {
                    enabled = true
                }
            }
        } catch (_: Exception) {}
    }

    override fun play() {
        exoPlayer?.play()
    }

    override fun pause() {
        exoPlayer?.pause()
    }

    override fun seekTo(ms: Long) {
        val p = exoPlayer
        if (p != null) {
            p.seekTo(ms)
            pendingSeekMs = null
        } else {
            pendingSeekMs = ms
        }
    }

    override fun setVolume(volume: Float) {
        currentVolume = volume.coerceIn(0f, 1f)
        exoPlayer?.volume = currentVolume
    }

    override fun setPlaybackSpeed(speed: Float) {
        currentSpeed = speed.coerceIn(0.25f, 3.0f)
        exoPlayer?.playbackParameters = PlaybackParameters(currentSpeed)
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
        } catch (_: Exception) {}
    }

    override fun setSpectrumListener(listener: ((FloatArray) -> Unit)?) {}

    override fun positionMs(): Long =
        runCatching { exoPlayer?.currentPosition ?: 0L }.getOrDefault(0L)

    override fun durationMs(): Long =
        runCatching { (exoPlayer?.duration ?: 0L).coerceAtLeast(0L) }.getOrDefault(0L)

    override fun release() {
        runCatching {
            equalizer?.release()
            equalizer = null
            exoPlayer?.stop()
            exoPlayer?.release()
            exoPlayer = null
        }
        pendingSeekMs = null
        onReadyCallback = null
        onFinishedCallback = null
    }
}

actual fun getEnv(name: String): String? = System.getenv(name)

actual inline fun <R> synchronizedPlatform(lock: Any, block: () -> R): R = synchronized(lock) { block() }

