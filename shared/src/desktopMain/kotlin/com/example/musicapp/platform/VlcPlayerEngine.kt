package com.example.musicapp.platform

import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.base.Equalizer
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.support.version.LibVlcVersion
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object VlcBridge {
    private var _discovered: Boolean? = null
    var vlcLoadError: String? = null
        private set
    var vlcDirectoryPath: String? = null
        private set
    private var _factory: MediaPlayerFactory? = null

    val isAvailable: Boolean
        get() = initVlc()

    val engineStatusText: String
        get() = if (isAvailable) "Движок: VLC" else fallbackStatusText

    val fallbackStatusText: String
        get() = "Движок: JavaFX (VLC не загружен: ${vlcLoadError ?: "библиотеки не найдены"}). Тон при изменении скорости не сохраняется"

    @Synchronized
    fun initVlc(): Boolean {
        if (_discovered != null) return _discovered == true

        // Strict check: ONLY search inside application directory or bundled resources,
        // NEVER pick up system VLC from Program Files
        val resDir = System.getProperty("compose.application.resources.dir")
        val appDir = System.getProperty("app.dir")

        val candidateDirs = listOfNotNull(
            resDir?.let { File(it, "vlc") },
            resDir?.let { File(it, "windows/vlc") },
            resDir?.let { File(it) },
            appDir?.let { File(it, "resources/vlc") },
            appDir?.let { File(it, "app/resources/vlc") },
            appDir?.let { File(it, "vlc") },
            // Development / test workspace paths
            File("desktopApp/resources/windows/vlc"),
            File("../desktopApp/resources/windows/vlc"),
            File("desktopApp/resources/vlc"),
            File("../desktopApp/resources/vlc"),
            File("resources/windows/vlc")
        )

        val searchedPaths = mutableListOf<String>()
        var foundDir: File? = null

        for (candidate in candidateDirs) {
            searchedPaths.add(candidate.absolutePath)
            if (candidate.exists() && File(candidate, "libvlc.dll").exists()) {
                foundDir = candidate
                break
            }
        }

        if (foundDir == null) {
            vlcLoadError = "Библиотека libvlc.dll не найдена ни в одном из каталогов приложения: ${searchedPaths.take(3).joinToString("; ")}"
            _discovered = false
            logEngineEvent(isSuccess = false, reason = vlcLoadError)
            return false
        }

        val vlcDirPath = foundDir.absolutePath
        vlcDirectoryPath = vlcDirPath
        val pluginsDir = File(foundDir, "plugins")

        // Strictly set jna.library.path and search paths before factory creation
        System.setProperty("jna.library.path", vlcDirPath)
        com.sun.jna.NativeLibrary.addSearchPath("libvlc", vlcDirPath)
        com.sun.jna.NativeLibrary.addSearchPath("libvlccore", vlcDirPath)
        if (pluginsDir.exists()) {
            System.setProperty("VLC_PLUGIN_PATH", pluginsDir.absolutePath)
        }

        try {
            // Enable cache (no --no-plugins-cache) for quick start, scaletempo and time stretch for pitch preservation
            val f = MediaPlayerFactory(
                "--audio-filter=scaletempo",
                "--audio-time-stretch",
                "--no-video",
                "--no-sub-autodetect-file",
                "--no-stats",
                "--no-media-library"
            )
            _factory = f
            _discovered = true

            // Startup self-check
            performSelfCheck(foundDir, f)
            return true
        } catch (t: Throwable) {
            vlcLoadError = "${t.javaClass.simpleName}: ${t.message ?: "ошибка создания фабрики MediaPlayerFactory"}"
            _discovered = false
            logEngineEvent(isSuccess = false, reason = vlcLoadError)
            return false
        }
    }

    val factory: MediaPlayerFactory
        get() {
            initVlc()
            return _factory ?: throw IllegalStateException("VLC MediaPlayerFactory не инициализирована: $vlcLoadError")
        }

    private fun performSelfCheck(vlcDir: File, factory: MediaPlayerFactory) {
        val libVlcVer = runCatching {
            LibVlcVersion().version.toString()
        }.getOrNull() ?: "3.0.x"

        val pluginsDir = File(vlcDir, "plugins")
        val allPluginFiles = if (pluginsDir.exists()) pluginsDir.walkTopDown().filter { it.isFile }.toList() else emptyList()

        val flacFound = allPluginFiles.any { it.name.contains("flac", ignoreCase = true) }
        val vorbisFound = allPluginFiles.any { it.name.contains("vorbis", ignoreCase = true) }
        val mp3Found = allPluginFiles.any {
            it.name.contains("mad", ignoreCase = true) ||
            it.name.contains("mpg123", ignoreCase = true) ||
            it.name.contains("mpga", ignoreCase = true)
        }
        val audioOutputFound = allPluginFiles.any {
            it.parentFile?.name == "audio_output" ||
            it.name.contains("wasapi", ignoreCase = true) ||
            it.name.contains("directsound", ignoreCase = true)
        }
        val scaletempoFound = allPluginFiles.any { it.name.contains("scaletempo", ignoreCase = true) }
        val pluginsDatFound = File(pluginsDir, "plugins.dat").exists()

        val details = buildString {
            appendLine("libVLC Version: $libVlcVer")
            appendLine("Plugins verification:")
            appendLine("  * flac: ${if (flacFound) "OK" else "MISSING"}")
            appendLine("  * vorbis: ${if (vorbisFound) "OK" else "MISSING"}")
            appendLine("  * mp3: ${if (mp3Found) "OK" else "MISSING"}")
            appendLine("  * audio_output: ${if (audioOutputFound) "OK" else "MISSING"}")
            appendLine("  * scaletempo (pitch preservation): ${if (scaletempoFound) "OK" else "MISSING"}")
            appendLine("  * plugins.dat: ${if (pluginsDatFound) "OK" else "MISSING"}")
            appendLine("Factory Flags: --audio-filter=scaletempo, --audio-time-stretch, --no-video, --no-sub-autodetect-file, --no-stats, --no-media-library")
        }

        logEngineEvent(isSuccess = true, checkDetails = details)
    }

    fun logEngineEvent(isSuccess: Boolean, reason: String? = null, checkDetails: String? = null) {
        try {
            val userHome = System.getProperty("user.home")
            val logsDir = File(userHome, "JVXMusic/logs").apply { mkdirs() }
            val logFile = File(logsDir, "engine.log")
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            val entry = buildString {
                appendLine("=== JVXMusic Audio Engine Event [$dateStr] ===")
                if (isSuccess) {
                    appendLine("Engine: VLC (vlcj)")
                    appendLine("Status: ACTIVE")
                    appendLine("Directory: $vlcDirectoryPath")
                    checkDetails?.let { appendLine(it.trimEnd()) }
                } else {
                    appendLine("Engine: JavaFX Media Engine (VLC Fallback)")
                    appendLine("Status: FALLBACK")
                    appendLine("Reason: $reason")
                    appendLine("Pitch preservation: Disabled (rate changes shift pitch on JavaFX)")
                    appendLine("Formats: MP3/WAV only (FLAC/OGG not supported on JavaFX)")
                }
                appendLine("==================================================")
                appendLine()
            }
            logFile.appendText(entry)
        } catch (_: Throwable) {
            // Do not crash if logging fails
        }
    }
}

class VlcPlayerEngine : PlayerEngine {
    override val supportsSpectrum: Boolean get() = false
    override val supportsPreamp: Boolean get() = true
    override val engineName: String get() = "VLC"
    override val engineStatusText: String get() = "Движок: VLC"
    override val isFallback: Boolean get() = false
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
                        // Re-apply speed rate: libVLC resets rate to 1.0f on loading new media
                        mediaPlayer.controls().setRate(currentRate)
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
                        // Ensure active rate with WSOLA scaletempo time-stretch is enforced
                        mediaPlayer.controls().setRate(currentRate)
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

                val isRemote = path.startsWith("http://") || path.startsWith("https://")
                val mrl = if (isRemote) path else File(path).absolutePath
                val prepared = if (isRemote) {
                    player.media().prepare(mrl, ":network-caching=150", ":clock-jitter=0", ":clock-synchro=0")
                } else {
                    player.media().prepare(mrl)
                }
                val len = player.status().length().coerceAtLeast(0L)
                if (len > 0L) cachedDurationMs = len
                runOnUiThread { onReady(cachedDurationMs) }
            } catch (t: Throwable) {
                onError?.invoke(t.message ?: "Ошибка загрузки аудиофайла")
            }
        }
    }

    override fun play() {
        playerLock.withLock {
            isPlayingState = true
            mediaPlayer?.controls()?.play()
            mediaPlayer?.controls()?.setRate(currentRate)
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
