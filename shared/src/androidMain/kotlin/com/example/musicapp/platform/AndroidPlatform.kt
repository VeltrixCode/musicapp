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
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.AudioAttributes as Media3AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
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

/** Set by the app at startup: the context and the picker launchers registered in MainActivity. */
object AndroidBridge {
    private var _appContext: Context? = null
    var appContext: Context
        get() = _appContext ?: throw IllegalStateException("AndroidBridge.appContext not initialized")
        set(value) { _appContext = value }
    val appContextOrNull: Context? get() = _appContext
    var pickAudio: (((Uri?) -> Unit) -> Unit)? = null
    var pickFolder: (((Uri?) -> Unit) -> Unit)? = null
    var onHeadphonesUnplugged: (() -> Unit)? = null
}

private val mainHandler = Handler(Looper.getMainLooper())

actual fun createPlayerEngine(): PlayerEngine = AndroidPlayerEngine()

actual fun runOnUiThread(block: () -> Unit) {
    if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post { block() }
}

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

private fun setMetadataDataSource(r: MediaMetadataRetriever, path: String) {
    val ctx = AndroidBridge.appContextOrNull
    if (path.startsWith("content://") && ctx != null) {
        r.setDataSource(ctx, Uri.parse(path))
    } else {
        r.setDataSource(path)
    }
}

actual fun readTrackInfo(path: String): TrackInfo? = runCatching {
    val r = MediaMetadataRetriever()
    try {
        setMetadataDataSource(r, path)
        TrackInfo(
            title = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.trim()?.takeIf { it.isNotBlank() },
            artist = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.trim()?.takeIf { it.isNotBlank() },
            album = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.trim()?.takeIf { it.isNotBlank() },
            genre = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)?.trim()?.takeIf { it.isNotBlank() },
            year = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)?.trim()?.toIntOrNull(),
            durationMs = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.trim()?.toLongOrNull() ?: 0L,
        )
    } finally {
        runCatching { r.release() }
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
            setMetadataDataSource(r, source)
            val pic = r.embeddedPicture ?: return@runCatching null
            val bitmap = BitmapFactory.decodeByteArray(pic, 0, pic.size)?.asImageBitmap() ?: return@runCatching null
            ArtworkCache.put(source, bitmap)
            ArtworkCache.computeAndStoreDominantColor(source, bitmap)
            bitmap
        } finally {
            runCatching { r.release() }
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

actual fun isFileAvailable(source: String?): Boolean {
    if (source.isNullOrBlank()) return false
    return runCatching {
        if (source.startsWith("content://")) {
            val ctx = AndroidBridge.appContextOrNull
            if (ctx == null) false
            else ctx.contentResolver.openInputStream(Uri.parse(source))?.use { true } ?: false
        } else if (source.startsWith("http://") || source.startsWith("https://")) {
            true
        } else {
            File(source).exists()
        }
    }.getOrDefault(false)
}

actual fun launchAudioImport(onImported: (Track) -> Unit) {
    val picker = AndroidBridge.pickAudio ?: return
    picker { uri -> if (uri != null) importFromUri(uri, onImported) }
}

actual fun launchFolderImport(onImported: (List<Track>) -> Unit) {
    val folderPicker = AndroidBridge.pickFolder
    if (folderPicker != null) {
        folderPicker { treeUri ->
            if (treeUri != null) {
                importFromTreeUri(treeUri, onImported)
            }
        }
    } else {
        launchAudioImport { track -> onImported(listOf(track)) }
    }
}

private fun importFromTreeUri(treeUri: Uri, onImported: (List<Track>) -> Unit) {
    val ctx = AndroidBridge.appContextOrNull ?: return
    val root = DocumentFile.fromTreeUri(ctx, treeUri) ?: return
    val tracks = mutableListOf<Track>()

    fun scan(dir: DocumentFile) {
        val files = dir.listFiles()
        for (f in files) {
            if (f.isDirectory) {
                scan(f)
            } else if (f.isFile) {
                val name = f.name.orEmpty()
                val isAudio = f.type?.startsWith("audio/") == true ||
                    listOf(".mp3", ".wav", ".m4a", ".aac", ".flac", ".ogg").any { name.endsWith(it, ignoreCase = true) }
                if (isAudio) {
                    val u = f.uri.toString()
                    val info = readTrackInfo(u)
                    val title = info?.title ?: name.substringBeforeLast('.').trim()
                    val artist = info?.artist ?: "Неизвестный исполнитель"
                    tracks.add(
                        Track(
                            id = newId(),
                            title = title,
                            artist = artist,
                            album = info?.album ?: "",
                            genre = info?.genre,
                            year = info?.year,
                            source = u,
                            durationMs = info?.durationMs ?: 0L,
                            addedAt = currentTimeMillis()
                        )
                    )
                }
            }
        }
    }

    scan(root)
    if (tracks.isNotEmpty()) {
        onImported(tracks)
    }
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
    // Android ExoPlayer handles release cleanup
}

private fun importFromUri(uri: Uri, onImported: (Track) -> Unit) {
    val ctx = AndroidBridge.appContext
    val scheme = uri.scheme
    val fileName = if (scheme == "content") {
        ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) cursor.getString(idx) else null
            } else null
        } ?: "track.mp3"
    } else {
        uri.lastPathSegment ?: "track.mp3"
    }

    val uriString = uri.toString()
    val info = readTrackInfo(uriString)
    val title = info?.title ?: fileName.substringBeforeLast('.').trim()
    val artist = info?.artist ?: "Неизвестный исполнитель"
    val track = Track(
        id = newId(),
        title = title,
        artist = artist,
        album = info?.album ?: "",
        genre = info?.genre,
        year = info?.year,
        source = uriString,
        durationMs = info?.durationMs ?: 0L,
        addedAt = currentTimeMillis(),
    )
    onImported(track)
}

private val streamingOkHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            val urlString = original.url.toString()
            val registeredSource = AndroidStreamRegistry.getStreamSource(urlString)

            val reqBuilder = original.newBuilder()

            if (registeredSource != null) {
                registeredSource.headers.forEach { (k, v) ->
                    if (original.header(k) == null) {
                        reqBuilder.header(k, v)
                    }
                }
            } else if (urlString.contains("googleapis.com")) {
                val gtoken = SecureStorage.getSecret(AppConfig.TOKEN_GDRIVE_ACCESS)
                if (!gtoken.isNullOrBlank() && original.header("Authorization") == null) {
                    reqBuilder.header("Authorization", "Bearer $gtoken")
                }
            } else if (urlString.contains("yandex.net") || urlString.contains("disk.yandex")) {
                val ytoken = SecureStorage.getSecret(AppConfig.TOKEN_YANDEX_ACCESS)
                if (!ytoken.isNullOrBlank() && original.header("Authorization") == null) {
                    reqBuilder.header("Authorization", "OAuth $ytoken")
                }
            }

            var response = chain.proceed(reqBuilder.build())

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
 * Media3 ExoPlayer engine with strict Main Thread enforcement.
 * All ExoPlayer mutations occur on Main Looper.
 * State getters (positionMs, durationMs, isPlaying) read thread-safe cached volatile fields.
 */
internal class AndroidPlayerEngine : PlayerEngine {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var exoPlayer: ExoPlayer? = null
    private var equalizer: Equalizer? = null

    @Volatile private var cachedPositionMs: Long = 0L
    @Volatile private var cachedDurationMs: Long = 0L
    @Volatile private var cachedIsPlaying: Boolean = false

    private var pendingSeekMs: Long? = null
    private var currentSpeed = 1.0f
    private var currentVolume = 0.8f

    private var onReadyCallback: ((Long) -> Unit)? = null
    private var onFinishedCallback: (() -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null

    private val progressUpdater = object : Runnable {
        override fun run() {
            val p = exoPlayer
            if (p != null && p.isPlaying) {
                cachedPositionMs = p.currentPosition.coerceAtLeast(0L)
                mainHandler.postDelayed(this, 150)
            }
        }
    }

    private fun postOnMain(block: (ExoPlayer?) -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block(exoPlayer)
        } else {
            mainHandler.post { block(exoPlayer) }
        }
    }

    override fun load(path: String, onReady: (Long) -> Unit, onFinished: () -> Unit) {
        load(path, onReady, onFinished, null)
    }

    override fun load(path: String, onReady: (Long) -> Unit, onFinished: () -> Unit, onError: ((String) -> Unit)?) {
        cachedPositionMs = 0L
        cachedDurationMs = 0L
        cachedIsPlaying = false
        onReadyCallback = onReady
        onFinishedCallback = onFinished
        onErrorCallback = onError

        postOnMain { _ ->
            runCatching {
                mainHandler.removeCallbacks(progressUpdater)
                equalizer?.release()
                equalizer = null
                exoPlayer?.stop()
                exoPlayer?.release()
                exoPlayer = null

                val ctx = AndroidBridge.appContextOrNull
                if (ctx == null) {
                    onError?.invoke("AppContext not initialized")
                    return@postOnMain
                }

                val httpDataSourceFactory = OkHttpDataSource.Factory(streamingOkHttpClient)
                val dataSourceFactory = DefaultDataSource.Factory(ctx, httpDataSourceFactory)
                val mediaSourceFactory = DefaultMediaSourceFactory(ctx)
                    .setDataSourceFactory(dataSourceFactory)

                val media3AudioAttributes = Media3AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build()

                val renderersFactory = DefaultRenderersFactory(ctx)
                    .setEnableAudioTrackPlaybackParams(false)

                val player = ExoPlayer.Builder(ctx, renderersFactory)
                    .setMediaSourceFactory(mediaSourceFactory)
                    .setAudioAttributes(media3AudioAttributes, true)
                    .build()

                player.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_READY -> {
                                val d = player.duration.coerceAtLeast(0L)
                                cachedDurationMs = d
                                cachedPositionMs = player.currentPosition.coerceAtLeast(0L)
                                cachedIsPlaying = player.isPlaying

                                pendingSeekMs?.let { ms ->
                                    player.seekTo(ms)
                                    pendingSeekMs = null
                                }
                                setupEqualizer(player.audioSessionId)
                                onReadyCallback?.invoke(d)
                            }
                            Player.STATE_ENDED -> {
                                cachedIsPlaying = false
                                mainHandler.removeCallbacks(progressUpdater)
                                onFinishedCallback?.invoke()
                            }
                            Player.STATE_IDLE -> {
                                cachedIsPlaying = false
                            }
                            Player.STATE_BUFFERING -> {}
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        cachedIsPlaying = isPlaying
                        if (isPlaying) {
                            mainHandler.removeCallbacks(progressUpdater)
                            mainHandler.post(progressUpdater)
                        } else {
                            mainHandler.removeCallbacks(progressUpdater)
                        }
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        cachedIsPlaying = false
                        mainHandler.removeCallbacks(progressUpdater)
                        val errString = error.message ?: "Playback error"
                        onErrorCallback?.invoke(errString)
                    }
                })

                player.volume = currentVolume
                // Pitch is strictly preserved at 1.0f to engage SonicAudioProcessor time-stretching without chipmunk effect
                player.playbackParameters = PlaybackParameters(currentSpeed, 1.0f)

                val uri = if (path.startsWith("http://") || path.startsWith("https://") || path.startsWith("content://") || path.startsWith("file://")) {
                    Uri.parse(path)
                } else {
                    Uri.fromFile(File(path))
                }

                player.setMediaItem(MediaItem.fromUri(uri))
                player.prepare()
                exoPlayer = player
            }.onFailure { err ->
                cachedIsPlaying = false
                onErrorCallback?.invoke(err.message ?: "Player initialization failed")
            }
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
        postOnMain { player ->
            player?.play()
            cachedIsPlaying = true
            mainHandler.removeCallbacks(progressUpdater)
            mainHandler.post(progressUpdater)
        }
    }

    override fun pause() {
        postOnMain { player ->
            player?.pause()
            cachedIsPlaying = false
            mainHandler.removeCallbacks(progressUpdater)
        }
    }

    override fun seekTo(ms: Long) {
        cachedPositionMs = ms
        postOnMain { player ->
            if (player != null) {
                player.seekTo(ms)
                pendingSeekMs = null
            } else {
                pendingSeekMs = ms
            }
        }
    }

    override fun setVolume(volume: Float) {
        currentVolume = volume.coerceIn(0f, 1f)
        postOnMain { player ->
            player?.volume = currentVolume
        }
    }

    override fun setPlaybackSpeed(speed: Float) {
        currentSpeed = speed.coerceIn(0.25f, 3.0f)
        postOnMain { player ->
            // Pitch is strictly preserved at 1.0f to engage SonicAudioProcessor time-stretching without chipmunk effect
            player?.playbackParameters = PlaybackParameters(currentSpeed, 1.0f)
        }
    }

    override fun setEqualizerBand(bandIndex: Int, gainDb: Float) {
        postOnMain { _ ->
            try {
                val eq = equalizer ?: return@postOnMain
                if (bandIndex in 0 until eq.numberOfBands.toInt()) {
                    val range = eq.bandLevelRange
                    val minLevel = range[0]
                    val maxLevel = range[1]
                    val level = (gainDb * 100).toInt().coerceIn(minLevel.toInt(), maxLevel.toInt()).toShort()
                    eq.setBandLevel(bandIndex.toShort(), level)
                }
            } catch (_: Exception) {}
        }
    }

    override fun setEqualizerEnabled(enabled: Boolean) {
        postOnMain { _ ->
            try {
                equalizer?.enabled = enabled
            } catch (_: Exception) {}
        }
    }

    override fun getEqualizerBandCount(): Int = equalizer?.numberOfBands?.toInt() ?: 5

    override fun getEqualizerGainRange(): ClosedFloatingPointRange<Float> {
        val eq = equalizer ?: return -15f..15f
        val r = eq.bandLevelRange
        return (r[0] / 100f)..(r[1] / 100f)
    }

    override fun getEqualizerBandFrequencies(): List<Int> {
        val eq = equalizer ?: return listOf(60, 230, 910, 3600, 14000)
        val count = eq.numberOfBands.toInt()
        return (0 until count).map { (eq.getCenterFreq(it.toShort()) / 1000).coerceAtLeast(1) }
    }

    override fun setSpectrumListener(listener: ((FloatArray) -> Unit)?) {}

    override fun positionMs(): Long = cachedPositionMs

    override fun durationMs(): Long = cachedDurationMs

    override fun isPlaying(): Boolean = cachedIsPlaying

    override fun release() {
        cachedIsPlaying = false
        postOnMain { _ ->
            runCatching {
                mainHandler.removeCallbacks(progressUpdater)
                equalizer?.release()
                equalizer = null
                exoPlayer?.stop()
                exoPlayer?.release()
                exoPlayer = null
            }
        }
        pendingSeekMs = null
        onReadyCallback = null
        onFinishedCallback = null
        onErrorCallback = null
    }
}

actual fun getEnv(name: String): String? = System.getenv(name) ?: System.getProperty(name)

actual inline fun <R> synchronizedPlatform(lock: Any, block: () -> R): R = synchronized(lock) { block() }
