package com.example.musicapp.platform

import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.base.Equalizer
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object VlcBridge {
    private var _discovered: Boolean? = null
    private var _vlcPath: String? = null

    val isAvailable: Boolean
        get() {
            if (_discovered == null) {
                // Strict check: ONLY search inside application directory or bundled resources,
                // NEVER pick up system VLC from Program Files
                val appDir = System.getProperty("compose.application.resources.dir")
                    ?: System.getProperty("app.dir")
                    ?: File(".").absolutePath

                val candidateDirs = listOf(
                    File(appDir, "vlc"),
                    File(appDir, "resources/vlc"),
                    File(appDir, "app/resources/vlc"),
                    // Development / test workspace paths
                    File("desktopApp/resources/vlc"),
                    File("../desktopApp/resources/vlc")
                )

                val foundDir = candidateDirs.firstOrNull { dir ->
                    dir.exists() && File(dir, "libvlc.dll").exists()
                }

                if (foundDir != null) {
                    val p = foundDir.absolutePath
                    _vlcPath = p
                    com.sun.jna.NativeLibrary.addSearchPath("libvlc", p)
                    System.setProperty("jna.library.path", p)
                    val pluginsDir = File(foundDir, "plugins")
                    if (pluginsDir.exists()) {
                        System.setProperty("VLC_PLUGIN_PATH", pluginsDir.absolutePath)
                    }
                    _discovered = runCatching {
                        NativeDiscovery().discover()
                    }.getOrDefault(true)
                } else {
                    System.err.println("Инфо: Встроенные библиотеки VLC не найдены в каталоге приложения. Выполняется автоматический откат на JavaFX Media engine.")
                    _discovered = false
                }
            }
            return _discovered == true
        }

    val factory: MediaPlayerFactory by lazy {
        MediaPlayerFactory(
            "--no-video",
            "--audio-filter=scaletempo", // Pitch preservation WSOLA filter
            "--no-sub-autodetect-file",
            "--no-plugins-cache",
            "--no-stats",
            "--no-media-library"
        )
    }
}

class VlcPlayerEngine : PlayerEngine {
    override val supportsSpectrum: Boolean get() = false
    override val supportsPreamp: Boolean get() = true
    private var mediaPlayer: MediaPlayer? = null
    private var equalizer: Equalizer? = null
    private var currentVolume = 0.8f
    private var currentRate = 1.0f
    private var pendingSeekMs: Long? = null
    private var cachedDurationMs = 0L
    private var cachedPositionMs = 0L
    private var isPlayingState = false
    private var preampGain = 0f

    private val equalizerGains = FloatArray(10) { 0f }
    private var onReadyCallback: ((Long) -> Unit)? = null
    private var onFinishedCallback: (() -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null
    private var spectrumCallback: ((FloatArray) -> Unit)? = null

    private val playerLock = ReentrantLock()

    // Dedicated single-thread dispatcher to isolate VLC event callbacks from player commands and avoid deadlocks
    private val eventDispatcher = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "jvx-vlc-event-dispatcher").apply { isDaemon = true }
    }

    init {
        initPlayer()
    }

    private fun initPlayer() {
        if (!VlcBridge.isAvailable) return
        try {
            val player = VlcBridge.factory.mediaPlayers().newMediaPlayer()
            equalizer = VlcBridge.factory.equalizer().newEqualizer()
            player.audio().setEqualizer(equalizer)
            player.audio().setVolume((currentVolume * 100).toInt().coerceIn(0, 100))
            player.controls().setRate(currentRate)

            player.events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
                override fun opening(mediaPlayer: MediaPlayer) {
                    isPlayingState = true
                }

                override fun mediaPlayerReady(mediaPlayer: MediaPlayer) {
                    // Dispatch away from JNA native thread before invoking application callbacks
                    eventDispatcher.execute {
                        val len = mediaPlayer.status().length().coerceAtLeast(0L)
                        if (len > 0L) cachedDurationMs = len
                        pendingSeekMs?.let { ms ->
                            mediaPlayer.controls().setTime(ms)
                            pendingSeekMs = null
                        }
                        runOnUiThread { onReadyCallback?.invoke(cachedDurationMs) }
                    }
                }

                override fun playing(mediaPlayer: MediaPlayer) {
                    eventDispatcher.execute {
                        isPlayingState = true
                        val len = mediaPlayer.status().length().coerceAtLeast(0L)
                        if (len > 0L) cachedDurationMs = len
                        pendingSeekMs?.let { ms ->
                            mediaPlayer.controls().setTime(ms)
                            pendingSeekMs = null
                        }
                        runOnUiThread { onReadyCallback?.invoke(cachedDurationMs) }
                    }
                }

                override fun paused(mediaPlayer: MediaPlayer) {
                    isPlayingState = false
                }

                override fun stopped(mediaPlayer: MediaPlayer) {
                    isPlayingState = false
                }

                override fun finished(mediaPlayer: MediaPlayer) {
                    // Do NOT call player methods synchronously from finished event callback
                    eventDispatcher.execute {
                        isPlayingState = false
                        runOnUiThread { onFinishedCallback?.invoke() }
                    }
                }

                override fun error(mediaPlayer: MediaPlayer) {
                    eventDispatcher.execute {
                        isPlayingState = false
                        runOnUiThread { onErrorCallback?.invoke("Неподдерживаемый или повреждённый аудиофайл") }
                    }
                }

                override fun timeChanged(mediaPlayer: MediaPlayer, newTime: Long) {
                    cachedPositionMs = newTime.coerceAtLeast(0L)
                }

                override fun lengthChanged(mediaPlayer: MediaPlayer, newLength: Long) {
                    if (newLength > 0L) {
                        cachedDurationMs = newLength
                        eventDispatcher.execute {
                            runOnUiThread { onReadyCallback?.invoke(newLength) }
                        }
                    }
                }
            })
            mediaPlayer = player
        } catch (t: Throwable) {
            System.err.println("Failed to init VLC MediaPlayer: ${t.message}")
        }
    }

    override fun load(path: String, onReady: (Long) -> Unit, onFinished: () -> Unit) {
        load(path, onReady, onFinished, null)
    }

    override fun load(path: String, onReady: (Long) -> Unit, onFinished: () -> Unit, onError: ((String) -> Unit)?) {
        playerLock.withLock {
            onReadyCallback = onReady
            onFinishedCallback = onFinished
            onErrorCallback = onError
            cachedPositionMs = 0L
            cachedDurationMs = 0L
            isPlayingState = false

            val player = mediaPlayer ?: run {
                onError?.invoke("VLC плеер не доступен на данной системе")
                return
            }

            try {
                player.controls().stop()
                player.audio().setVolume((currentVolume * 100).toInt().coerceIn(0, 100))
                player.controls().setRate(currentRate)

                val mrl = if (path.startsWith("http://") || path.startsWith("https://")) {
                    path
                } else {
                    File(path).absolutePath
                }
                val prepared = player.media().prepare(mrl)
                if (prepared) {
                    val len = player.status().length().coerceAtLeast(0L)
                    if (len > 0L) cachedDurationMs = len
                    runOnUiThread { onReady(cachedDurationMs) }
                }
            } catch (t: Throwable) {
                onError?.invoke(t.message ?: "Ошибка загрузки аудиофайла")
            }
        }
    }

    override fun play() {
        playerLock.withLock {
            isPlayingState = true
            mediaPlayer?.controls()?.play()
        }
    }

    override fun pause() {
        playerLock.withLock {
            isPlayingState = false
            mediaPlayer?.controls()?.pause()
        }
    }

    override fun seekTo(ms: Long) {
        playerLock.withLock {
            cachedPositionMs = ms
            val p = mediaPlayer
            if (p != null && p.status().isPlayable) {
                p.controls().setTime(ms)
                pendingSeekMs = null
            } else {
                pendingSeekMs = ms
            }
        }
    }

    override fun setVolume(volume: Float) {
        currentVolume = volume.coerceIn(0f, 1f)
        mediaPlayer?.audio()?.setVolume((currentVolume * 100).toInt().coerceIn(0, 100))
    }

    override fun setPlaybackSpeed(speed: Float) {
        currentRate = speed.coerceIn(0.25f, 3.0f)
        // LibVLC uses scaletempo WSOLA filter, preserving pitch strictly at 1.0 without chipmunk pitch shift!
        mediaPlayer?.controls()?.setRate(currentRate)
    }

    override fun setEqualizerBand(bandIndex: Int, gainDb: Float) {
        if (bandIndex in equalizerGains.indices) {
            val clamped = gainDb.coerceIn(-20f, 20f)
            equalizerGains[bandIndex] = clamped
            try {
                equalizer?.setAmp(bandIndex, clamped)
                mediaPlayer?.audio()?.setEqualizer(equalizer)
            } catch (_: Throwable) {}
        }
    }

    override fun setEqualizerPreamp(gainDb: Float) {
        preampGain = gainDb.coerceIn(-20f, 20f)
        try {
            equalizer?.setPreamp(preampGain)
            mediaPlayer?.audio()?.setEqualizer(equalizer)
        } catch (_: Throwable) {}
    }

    override fun getEqualizerPreamp(): Float = preampGain

    override fun setEqualizerEnabled(enabled: Boolean) {
        try {
            if (enabled) {
                mediaPlayer?.audio()?.setEqualizer(equalizer)
            } else {
                mediaPlayer?.audio()?.setEqualizer(null)
            }
        } catch (_: Throwable) {}
    }

    override fun getEqualizerBandCount(): Int = 10

    override fun getEqualizerGainRange(): ClosedFloatingPointRange<Float> = -20f..20f

    override fun getEqualizerBandFrequencies(): List<Int> =
        listOf(60, 170, 310, 600, 1000, 3000, 6000, 12000, 14000, 16000)

    override fun setSpectrumListener(listener: ((FloatArray) -> Unit)?) {
        spectrumCallback = listener
        // Transparent spectrum behavior: VLC direct audio output does not compute software FFT;
        // no synthetic or fabricated sine wave data is emitted.
        if (listener != null) {
            runOnUiThread { listener(FloatArray(64) { 0f }) }
        }
    }

    override fun positionMs(): Long {
        val t = mediaPlayer?.status()?.time() ?: cachedPositionMs
        return if (t > 0L) t else cachedPositionMs
    }

    override fun durationMs(): Long {
        val d = mediaPlayer?.status()?.length() ?: cachedDurationMs
        return if (d > 0L) d else cachedDurationMs
    }

    override fun isPlaying(): Boolean = mediaPlayer?.status()?.isPlaying ?: isPlayingState

    override fun release() {
        isPlayingState = false
        playerLock.withLock {
            val p = mediaPlayer
            mediaPlayer = null
            if (p != null) {
                eventDispatcher.execute {
                    try {
                        p.controls().stop()
                        p.release()
                    } catch (_: Throwable) {}
                }
            }
            onReadyCallback = null
            onFinishedCallback = null
            onErrorCallback = null
            pendingSeekMs = null
        }
    }
}
