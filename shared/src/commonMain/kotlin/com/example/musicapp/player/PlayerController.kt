package com.example.musicapp.player

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.musicapp.lyrics.LrcParser
import com.example.musicapp.model.LrcLine
import com.example.musicapp.model.Track
import com.example.musicapp.platform.PlayerEngine
import com.example.musicapp.platform.createPlayerEngine
import com.example.musicapp.platform.currentTimeMillis
import com.example.musicapp.platform.readLrcContent
import com.example.musicapp.platform.readTrackInfoAsync
import com.example.musicapp.platform.runOnUiThread
import com.example.musicapp.settings.AppSettings
import com.example.musicapp.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class RepeatMode { Off, All, One }

enum class PlaybackContextType {
    Playlist,
    SmartMix,
    Album,
    Artist,
    Search,
    TrackShelf,
    Library,
    Unknown
}

data class PlaybackContext(
    val type: PlaybackContextType,
    val id: String,
    val title: String = "",
)

enum class EqualizerPreset(val title: String, val gains: FloatArray) {
    Flat("Обычный", floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)),
    BassBoost("Усиление баса", floatArrayOf(6f, 5f, 4f, 2f, 0f, 0f, 0f, 0f, 0f, 0f)),
    Rock("Рок", floatArrayOf(4f, 3f, 2f, 0f, -1f, 1f, 3f, 4f, 5f, 5f)),
    Pop("Поп", floatArrayOf(-1f, 1f, 3f, 4f, 4f, 3f, 1f, -1f, -1f, -1f)),
    Vocal("Вокал", floatArrayOf(-2f, -2f, -1f, 2f, 4f, 4f, 3f, 1f, 0f, -1f)),
    Classical("Классика", floatArrayOf(4f, 3f, 3f, 2f, -1f, -1f, 0f, 2f, 3f, 4f)),
}

@Immutable
data class PlaybackState(
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
)

@Immutable
data class PlayerState(
    val current: Track? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val volume: Float = 0.8f,
    val isMuted: Boolean = false,
    val shuffle: Boolean = false,
    val repeat: RepeatMode = RepeatMode.Off,
    val speed: Float = 1.0f,
    val queue: List<Track> = emptyList(),
    val index: Int = -1,
)

interface AudioPlayer {
    val state: StateFlow<PlayerState>
    val playbackState: StateFlow<PlaybackState>
    val spectrumState: StateFlow<FloatArray>
    val sleepTimerRemainingSec: StateFlow<Int?>

    val current: Track?
    val isPlaying: Boolean
    val positionMs: Long
    val durationMs: Long
    val volume: Float
    val shuffle: Boolean
    val repeat: RepeatMode
    val speed: Float
    val equalizerPreset: EqualizerPreset
    val equalizerGains: FloatArray
    val isEqualizerEnabled: Boolean
    val supportsSpectrum: Boolean get() = false
    val supportsPreamp: Boolean get() = false
    val equalizerPreamp: Float get() = 0f
    fun setEqualizerPreamp(gainDb: Float) {}
    val engineStatusText: String get() = "Движок: Активен"
    val isEngineFallback: Boolean get() = false
    val fallbackWarningNotice: StateFlow<String?>
    fun dismissFallbackWarning() {}
    val lyrics: List<LrcLine>
    val queue: List<Track>
    val unsupportedFormatNotice: String?
    val appError: StateFlow<com.example.musicapp.model.AppError?>
    val providerRegistry: com.example.musicapp.provider.ProviderRegistry
    val playbackContext: StateFlow<PlaybackContext?>

    fun isContextPlaying(type: PlaybackContextType, id: String): Boolean =
        isPlaying && playbackContext.value?.let { it.type == type && it.id == id } == true

    fun dismissFormatNotice()
    fun dismissError()
    fun downloadForOffline(track: Track)
    fun downloadForOffline(track: Track, userConfirmed: Boolean) {
        downloadForOffline(track)
    }
    fun deleteOffline(trackId: String)

    fun play(track: Track, context: List<Track> = listOf(track))
    fun play(track: Track, context: List<Track>, playbackContext: PlaybackContext?) {
        play(track, context)
    }
    fun toggle()
    fun pause()
    fun resume()
    fun seekTo(positionMs: Long)
    fun updateVolume(vol: Float)
    fun toggleMute()
    fun next()
    fun previous()
    fun setShuffle(enabled: Boolean)
    fun setRepeat(mode: RepeatMode)
    fun cycleRepeat()
    fun setPlaybackSpeed(speed: Float)
    fun setEqualizerPreset(preset: EqualizerPreset)
    fun setEqualizerBand(bandIndex: Int, gainDb: Float)
    fun setEqualizerEnabled(enabled: Boolean)
    fun resetEqualizer()
    fun getEqualizerBandCount(): Int
    fun getEqualizerGainRange(): ClosedFloatingPointRange<Float>
    fun getEqualizerBandFrequencies(): List<Int>
    fun setSleepTimer(minutes: Int?)

    fun playNext(track: Track)
    fun addToQueue(track: Track)
    fun removeFromQueue(index: Int)
    fun moveQueueItem(fromIndex: Int, toIndex: Int)
    fun clearQueue()

    fun onTrackRemoved(trackId: String)
    fun release()
}

class PlayerController(
    val settingsRepository: SettingsRepository = SettingsRepository(),
    var onTrackDurationResolved: ((trackId: String, durationMs: Long) -> Unit)? = null,
    customEngine: PlayerEngine? = null,
) : AudioPlayer {
    private val engine: PlayerEngine = customEngine ?: createPlayerEngine()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val _playbackState = MutableStateFlow(PlaybackState())
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _spectrumState = MutableStateFlow(FloatArray(32) { 0f })
    override val spectrumState: StateFlow<FloatArray> = _spectrumState.asStateFlow()

    private val _sleepTimerRemainingSec = MutableStateFlow<Int?>(null)
    override val sleepTimerRemainingSec: StateFlow<Int?> = _sleepTimerRemainingSec.asStateFlow()

    override var current by mutableStateOf<Track?>(null)
        private set
    override var isPlaying by mutableStateOf(false)
        private set
    override var positionMs by mutableStateOf(0L)
        private set
    override var durationMs by mutableStateOf(0L)
        private set

    private var shuffleState by mutableStateOf(false)
    private var repeatState by mutableStateOf(RepeatMode.Off)
    override val shuffle: Boolean get() = shuffleState
    override val repeat: RepeatMode get() = repeatState

    override var volume by mutableStateOf(0.8f)
        private set
    override var speed by mutableStateOf(1.0f)
        private set

    private var equalizerPresetState by mutableStateOf(EqualizerPreset.Flat)
    override val equalizerPreset: EqualizerPreset get() = equalizerPresetState
    override val equalizerGains = FloatArray(10) { 0f }
    private var _isEqualizerEnabled by mutableStateOf(true)
    override val isEqualizerEnabled: Boolean get() = _isEqualizerEnabled

    override val supportsSpectrum: Boolean get() = engine.supportsSpectrum
    override val supportsPreamp: Boolean get() = engine.supportsPreamp
    override val equalizerPreamp: Float get() = engine.getEqualizerPreamp()
    override val engineStatusText: String get() = engine.engineStatusText
    override val isEngineFallback: Boolean get() = engine.isFallback

    private val _fallbackWarningNotice = MutableStateFlow<String?>(null)
    override val fallbackWarningNotice: StateFlow<String?> = _fallbackWarningNotice.asStateFlow()

    override fun dismissFallbackWarning() {
        _fallbackWarningNotice.value = null
    }

    override fun setEqualizerPreamp(gainDb: Float) {
        engine.setEqualizerPreamp(gainDb)
    }

    override var lyrics by mutableStateOf<List<LrcLine>>(emptyList())
        private set

    override var unsupportedFormatNotice by mutableStateOf<String?>(null)
        private set

    private val _appError = MutableStateFlow<com.example.musicapp.model.AppError?>(null)
    override val appError: StateFlow<com.example.musicapp.model.AppError?> = _appError.asStateFlow()

    private val errorQueue = mutableListOf<com.example.musicapp.model.AppError>()
    private var errorDismissJob: Job? = null
    private var lastErrorMessage: String? = null
    private var lastErrorTime: Long = 0L

    private val _playbackContext = MutableStateFlow<PlaybackContext?>(null)
    override val playbackContext: StateFlow<PlaybackContext?> = _playbackContext.asStateFlow()

    override val providerRegistry = com.example.musicapp.provider.ProviderRegistry()

    override fun dismissFormatNotice() {
        unsupportedFormatNotice = null
    }

    override fun dismissError() {
        errorDismissJob?.cancel()
        advanceErrorQueue()
    }

    fun dismissAllErrors() {
        errorDismissJob?.cancel()
        errorQueue.clear()
        _appError.value = null
    }

    fun postError(err: com.example.musicapp.model.AppError) {
        val now = currentTimeMillis()
        if (err.message == lastErrorMessage && (now - lastErrorTime) < 4000L) {
            return
        }
        lastErrorMessage = err.message
        lastErrorTime = now

        if (_appError.value == null) {
            showError(err)
        } else {
            errorQueue.add(err)
        }
    }

    private fun showError(err: com.example.musicapp.model.AppError) {
        _appError.value = err
        errorDismissJob?.cancel()
        errorDismissJob = scope.launch {
            delay(5000L)
            runOnUiThread {
                advanceErrorQueue()
            }
        }
    }

    private fun advanceErrorQueue() {
        if (errorQueue.isNotEmpty()) {
            val next = errorQueue.removeAt(0)
            showError(next)
        } else {
            _appError.value = null
        }
    }

    fun isOfflineDownloadConfirmationRequired(track: Track): Boolean {
        if (track.sourceProvider == com.example.musicapp.model.TrackSource.INTERNET_ARCHIVE) {
            return com.example.musicapp.provider.InternetArchiveProvider.isLicenseRestrictedOrUnknown(track.license)
        }
        return false
    }

    override fun downloadForOffline(track: Track) {
        downloadForOffline(track, false)
    }

    override fun downloadForOffline(track: Track, userConfirmed: Boolean) {
        if (track.sourceProvider == com.example.musicapp.model.TrackSource.LOCAL) return
        if (track.isLiveStream || track.sourceProvider == com.example.musicapp.model.TrackSource.RADIO_BROWSER || track.durationMs <= 0L) {
            return
        }
        if (isOfflineDownloadConfirmationRequired(track) && !userConfirmed) {
            return
        }
        scope.launch {
            val stream = providerRegistry.getStreamSource(track)
            if (stream != null) {
                com.example.musicapp.cache.AudioOfflineCache.downloadTrack(
                    track = track,
                    streamUrl = stream.url,
                    headers = stream.headers
                ) { cachedPath ->
                    val idx = _queue.indexOfFirst { it.id == track.id }
                    if (idx >= 0) {
                        _queue[idx] = _queue[idx].copy(isOfflineAvailable = true, cachedFilePath = cachedPath)
                    }
                    if (current?.id == track.id) {
                        current = current?.copy(isOfflineAvailable = true, cachedFilePath = cachedPath)
                    }
                }
            }
        }
    }

    override fun deleteOffline(trackId: String) {
        com.example.musicapp.cache.AudioOfflineCache.deleteFromCache(trackId)
        val idx = _queue.indexOfFirst { it.id == trackId }
        if (idx >= 0) {
            _queue[idx] = _queue[idx].copy(isOfflineAvailable = false, cachedFilePath = null)
        }
        if (current?.id == trackId) {
            current = current?.copy(isOfflineAvailable = false, cachedFilePath = null)
        }
    }

    private val _queue = mutableStateListOf<Track>()
    override val queue: List<Track> get() = _queue

    private var lastNonZeroVolume = 0.8f
    private var currentIndex = -1
    private val shuffledIndices = mutableListOf<Int>()
    private var shufflePointer = -1

    // Grace period guard against tick() overwriting async seek position
    private var pendingSeekTargetMs: Long? = null
    private var seekTimestamp: Long = 0L

    internal var consecutivePlaybackErrors = 0

    private var sleepTimerJob: Job? = null

    init {
        if (engine.isFallback) {
            _fallbackWarningNotice.value = "Внимание: Движок VLC не активен (${engine.engineStatusText}). Тон при изменении скорости не сохраняется."
        }
        // Load persisted settings
        val settings = settingsRepository.load()
        volume = settings.volume
        lastNonZeroVolume = if (settings.volume > 0f) settings.volume else 0.8f
        shuffleState = settings.shuffle
        repeatState = settings.repeatMode
        speed = settings.playbackSpeed.coerceIn(0.5f, 2.0f)

        val preset = EqualizerPreset.entries.find { it.name.equals(settings.equalizerPresetName, true) } ?: EqualizerPreset.Flat
        equalizerPresetState = preset
        preset.gains.copyInto(equalizerGains)
        if (settings.equalizerCustomGains.size == 10) {
            for (i in 0 until 10) equalizerGains[i] = settings.equalizerCustomGains[i]
        }

        _isEqualizerEnabled = settings.isEqualizerEnabled
        engine.setEqualizerEnabled(isEqualizerEnabled)
        engine.setVolume(volume)
        engine.setPlaybackSpeed(speed)
        for (i in equalizerGains.indices) {
            engine.setEqualizerBand(i, equalizerGains[i])
        }

        // Setup spectrum listener from platform engine
        engine.setSpectrumListener { rawFrequencies ->
            // Compress 64 bands down to 32 visual bands with smooth decay
            val target = FloatArray(32) { idx ->
                val r1 = rawFrequencies.getOrElse(idx * 2) { 0f }
                val r2 = rawFrequencies.getOrElse(idx * 2 + 1) { 0f }
                ((r1 + r2) / 2f).coerceIn(0f, 1f)
            }
            _spectrumState.value = target
        }

        syncState()

        // Background progress ticker
        scope.launch {
            while (isActive) {
                tick()
                delay(120)
            }
        }
    }

    override fun updateVolume(vol: Float) {
        val v = vol.coerceIn(0f, 1f)
        volume = v
        if (v > 0f) lastNonZeroVolume = v
        engine.setVolume(v)
        persistSettings()
        syncState()
    }

    override fun toggleMute() {
        if (volume > 0f) {
            lastNonZeroVolume = volume
            updateVolume(0f)
        } else {
            updateVolume(if (lastNonZeroVolume > 0f) lastNonZeroVolume else 0.8f)
        }
    }

    override fun setShuffle(enabled: Boolean) {
        shuffleState = enabled
        if (enabled) {
            rebuildShuffledQueue()
        }
        persistSettings()
        syncState()
    }

    override fun setRepeat(mode: RepeatMode) {
        repeatState = mode
        persistSettings()
        syncState()
    }

    override fun cycleRepeat() {
        repeatState = when (repeatState) {
            RepeatMode.Off -> RepeatMode.All
            RepeatMode.All -> RepeatMode.One
            RepeatMode.One -> RepeatMode.Off
        }
        persistSettings()
        syncState()
    }

    override fun setPlaybackSpeed(speed: Float) {
        val s = speed.coerceIn(0.25f, 3.0f)
        this.speed = s
        engine.setPlaybackSpeed(s)
        persistSettings()
        syncState()
    }

    override fun setEqualizerPreset(preset: EqualizerPreset) {
        equalizerPresetState = preset
        preset.gains.copyInto(equalizerGains)
        for (i in equalizerGains.indices) {
            engine.setEqualizerBand(i, equalizerGains[i])
        }
        persistSettings()
    }

    override fun setEqualizerBand(bandIndex: Int, gainDb: Float) {
        val range = engine.getEqualizerGainRange()
        if (bandIndex in equalizerGains.indices) {
            equalizerGains[bandIndex] = gainDb.coerceIn(range.start, range.endInclusive)
            engine.setEqualizerBand(bandIndex, equalizerGains[bandIndex])
            persistSettings()
        }
    }

    override fun setEqualizerEnabled(enabled: Boolean) {
        _isEqualizerEnabled = enabled
        engine.setEqualizerEnabled(enabled)
        persistSettings()
    }

    override fun resetEqualizer() {
        for (i in equalizerGains.indices) {
            equalizerGains[i] = 0f
            engine.setEqualizerBand(i, 0f)
        }
        equalizerPresetState = EqualizerPreset.Flat
        persistSettings()
    }

    override fun getEqualizerBandCount(): Int = engine.getEqualizerBandCount()
    override fun getEqualizerGainRange(): ClosedFloatingPointRange<Float> = engine.getEqualizerGainRange()
    override fun getEqualizerBandFrequencies(): List<Int> = engine.getEqualizerBandFrequencies()

    override fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        if (minutes == null || minutes <= 0) {
            _sleepTimerRemainingSec.value = null
            return
        }
        var remainingSec = minutes * 60
        _sleepTimerRemainingSec.value = remainingSec
        sleepTimerJob = scope.launch {
            while (isActive && remainingSec > 0) {
                delay(1000)
                remainingSec--
                _sleepTimerRemainingSec.value = remainingSec
            }
            if (remainingSec <= 0) {
                // Smooth fade-out before pause
                smoothFadeOutAndPause()
                _sleepTimerRemainingSec.value = null
            }
        }
    }

    private fun smoothFadeOutAndPause() {
        scope.launch {
            val initial = volume
            for (step in 10 downTo 0) {
                engine.setVolume((initial * (step / 10f)).coerceAtLeast(0f))
                delay(30)
            }
            pause()
            engine.setVolume(initial)
        }
    }

    override fun play(track: Track, context: List<Track>) {
        play(track, context, null)
    }

    override fun play(track: Track, context: List<Track>, playbackContext: PlaybackContext?) {
        if (track.isMissing) {
            val shortName = track.source?.substringAfterLast('/')?.substringAfterLast('\\') ?: "файл"
            val msg = "Не удалось воспроизвести «${track.title}»: файл не найден ($shortName)"
            postError(com.example.musicapp.model.AppError.Generic(msg))
            return
        }
        _playbackContext.value = playbackContext
        consecutivePlaybackErrors = 0
        _queue.clear()
        _queue.addAll(context.filter { !it.isMissing || it.id == track.id })
        currentIndex = _queue.indexOfFirst { it.id == track.id }
        if (currentIndex < 0) {
            _queue.add(0, track)
            currentIndex = 0
        }
        if (shuffleState) {
            rebuildShuffledQueue()
        }
        start(_queue[currentIndex])
    }

    override fun playNext(track: Track) {
        if (currentIndex >= 0 && currentIndex < _queue.size) {
            _queue.add(currentIndex + 1, track)
        } else {
            _queue.add(track)
        }
        if (shuffleState) rebuildShuffledQueue()
        syncState()
    }

    override fun addToQueue(track: Track) {
        _queue.add(track)
        if (shuffleState) rebuildShuffledQueue()
        syncState()
    }

    override fun removeFromQueue(index: Int) {
        if (index in _queue.indices) {
            val isCurrent = index == currentIndex
            _queue.removeAt(index)
            if (isCurrent) {
                if (_queue.isNotEmpty()) {
                    currentIndex = currentIndex.coerceIn(0, _queue.lastIndex)
                    start(_queue[currentIndex])
                } else {
                    engine.release()
                    current = null
                    isPlaying = false
                }
            } else if (index < currentIndex) {
                currentIndex--
            }
            if (shuffleState) rebuildShuffledQueue()
            syncState()
        }
    }

    override fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        if (fromIndex in _queue.indices && toIndex in _queue.indices && fromIndex != toIndex) {
            val item = _queue.removeAt(fromIndex)
            _queue.add(toIndex, item)
            current?.let { c ->
                currentIndex = _queue.indexOfFirst { it.id == c.id }
            }
            syncState()
        }
    }

    override fun clearQueue() {
        val c = current
        _queue.clear()
        if (c != null) {
            _queue.add(c)
            currentIndex = 0
        } else {
            currentIndex = -1
        }
        syncState()
    }

    override fun toggle() {
        val track = current ?: return
        if (track.source == null) return
        if (isPlaying) {
            pause()
        } else {
            resume()
        }
    }

    override fun pause() {
        engine.pause()
        isPlaying = false
        syncState()
    }

    override fun resume() {
        val track = current ?: return
        if (track.source == null) return
        engine.play()
        isPlaying = true
        syncState()
    }

    override fun next() {
        advance(+1)
    }

    override fun previous() {
        if (positionMs > 3_000) seekTo(0) else advance(-1)
    }

    /**
     * Reliable seeking: updates UI immediately and guards against tick()
     * overwriting position until the hardware player confirms.
     */
    override fun seekTo(positionMs: Long) {
        val bounded = positionMs.coerceIn(0L, durationMs.coerceAtLeast(0L))
        this.positionMs = bounded
        pendingSeekTargetMs = bounded
        seekTimestamp = currentTimeMillis()

        engine.seekTo(bounded)
        syncState()
    }

    fun tick() {
        if (current == null) return
        val engPos = engine.positionMs()
        val engDur = engine.durationMs()

        if (engDur > 0) {
            durationMs = engDur
            if (current?.durationMs != engDur) {
                current = current?.copy(durationMs = engDur)
            }
        }

        val target = pendingSeekTargetMs
        if (target != null) {
            // Guard: don't let stale position overwrite user seek
            val elapsed = currentTimeMillis() - seekTimestamp
            if (abs(engPos - target) <= 800L || elapsed > 1500L) {
                pendingSeekTargetMs = null
                positionMs = engPos
            } else {
                positionMs = target
            }
        } else {
            positionMs = engPos
        }

        syncState()
    }

    override fun onTrackRemoved(trackId: String) {
        val playingId = current?.id
        val idx = _queue.indexOfFirst { it.id == trackId }
        if (idx >= 0) {
            _queue.removeAt(idx)
        }
        if (playingId == trackId) {
            engine.release()
            current = null
            isPlaying = false
            positionMs = 0
            durationMs = 0
            lyrics = emptyList()
        }
        currentIndex = current?.let { c -> _queue.indexOfFirst { it.id == c.id } } ?: -1
        if (shuffleState) rebuildShuffledQueue()
        persistSettings()
        syncState()
    }

    override fun release() {
        com.example.musicapp.cache.AudioOfflineCache.currentlyPlayingTrackId = null
        com.example.musicapp.cache.AudioOfflineCache.currentlyQueuedTrackIds = emptySet()
        scope.cancel()
        sleepTimerJob?.cancel()
        engine.release()
        com.example.musicapp.platform.onPlaybackReleased()
    }

    private fun start(track: Track) {
        val previousTrackBeforeStart = current
        val previousPositionBeforeStart = positionMs
        val previousDurationBeforeStart = durationMs
        val previousTrackId = current?.id
        if (previousTrackId != null && previousTrackId != track.id) {
            com.example.musicapp.platform.onPlaybackTrackChanged(previousTrackId, track.id)
        }
        current = track
        positionMs = 0
        durationMs = track.durationMs
        pendingSeekTargetMs = null
        lyrics = emptyList()
        unsupportedFormatNotice = null
        com.example.musicapp.cache.AudioOfflineCache.currentlyPlayingTrackId = track.id
        com.example.musicapp.cache.AudioOfflineCache.currentlyQueuedTrackIds = _queue.map { it.id }.toSet()

        scope.launch {
            try {
                val cachedPath = com.example.musicapp.cache.AudioOfflineCache.getCachedPath(track.id)
                val effectiveSource: String? = when {
                    cachedPath != null -> cachedPath
                    track.sourceProvider != com.example.musicapp.model.TrackSource.LOCAL -> {
                        val isFlacOrOgg = track.mimeType?.contains("flac") == true ||
                                track.mimeType?.contains("ogg") == true ||
                                track.source?.endsWith(".flac", ignoreCase = true) == true ||
                                track.source?.endsWith(".ogg", ignoreCase = true) == true
                        if (isFlacOrOgg) {
                            unsupportedFormatNotice = "Формат FLAC/OGG не поддерживается встроенным движком JavaFX Media. Рекомендуется MP3/M4A/AAC или библиотека VLCJ."
                        }

                        val streamSource = providerRegistry.getStreamSource(track)
                        if (streamSource != null) {
                            com.example.musicapp.platform.prepareStreamUrlForPlayback(track.id, streamSource)
                        } else {
                            track.source
                        }
                    }
                    else -> track.source
                }

                if (effectiveSource == null) {
                    runOnUiThread {
                        engine.release()
                        isPlaying = false
                        val msg = "Не удалось получить аудиопоток для «${track.title}»"
                        postError(com.example.musicapp.model.AppError.Generic(msg))
                        syncState()
                    }
                    return@launch
                }

                // Asynchronously load lyrics if available (local file or cached)
                if (cachedPath != null || track.sourceProvider == com.example.musicapp.model.TrackSource.LOCAL) {
                    val lrcText = readLrcContent(effectiveSource)
                    lyrics = if (lrcText != null) LrcParser.parse(lrcText) else emptyList()
                }

                engine.load(
                    path = effectiveSource,
                    onReady = { authoritativeDur ->
                        runOnUiThread {
                            consecutivePlaybackErrors = 0
                            val isLive = track.isLiveStream || track.sourceProvider == com.example.musicapp.model.TrackSource.RADIO_BROWSER
                            if (!isLive && authoritativeDur > 0) {
                                durationMs = authoritativeDur
                                if (current?.id == track.id) {
                                    current = track.copy(durationMs = authoritativeDur)
                                }
                                onTrackDurationResolved?.invoke(track.id, authoritativeDur)
                            }
                            syncState()
                        }
                    },
                    onFinished = {
                        runOnUiThread { onTrackFinished() }
                    },
                    onError = { errorMsg ->
                        runOnUiThread {
                            val isUnsupported = track.mimeType?.contains("flac") == true ||
                                    track.mimeType?.contains("ogg") == true ||
                                    track.source?.endsWith(".flac", ignoreCase = true) == true ||
                                    track.source?.endsWith(".ogg", ignoreCase = true) == true ||
                                    track.format?.equals("flac", ignoreCase = true) == true ||
                                    track.format?.equals("ogg", ignoreCase = true) == true ||
                                    errorMsg.contains("MEDIA_UNAVAILABLE", ignoreCase = true) ||
                                    errorMsg.contains("UNSUPPORTED", ignoreCase = true)

                            val shortFileName = track.source?.substringAfterLast('/')?.substringAfterLast('\\') ?: ""
                            val isMissing = !com.example.musicapp.platform.isFileAvailable(track.source)
                            val userMsg = if (isUnsupported) {
                                "Формат ${track.format?.uppercase() ?: "FLAC/OGG"} не поддерживается встроенным движком JavaFX Media. Рекомендуется MP3/M4A/AAC."
                            } else if (isMissing && shortFileName.isNotBlank()) {
                                "Ошибка воспроизведения «${track.title}»: файл не найден ($shortFileName)"
                            } else {
                                "Ошибка воспроизведения «${track.title}»"
                            }
                            unsupportedFormatNotice = if (isUnsupported) userMsg else null
                            postError(
                                if (isUnsupported) com.example.musicapp.model.AppError.UnsupportedFormat(track.format?.uppercase() ?: "FLAC/OGG")
                                else com.example.musicapp.model.AppError.Generic(userMsg)
                            )

                            // Transition to next track in queue with circuit breaker to avoid tight native crash loops
                            consecutivePlaybackErrors++
                            val advanced = if (consecutivePlaybackErrors < 3 && consecutivePlaybackErrors < _queue.size) {
                                advance(+1)
                            } else {
                                false
                            }
                            if (!advanced) {
                                current = previousTrackBeforeStart
                                positionMs = previousPositionBeforeStart
                                durationMs = previousDurationBeforeStart
                                consecutivePlaybackErrors = 0
                                isPlaying = false
                                syncState()
                            }
                        }
                    }
                )
                engine.setVolume(volume)
                engine.setPlaybackSpeed(speed)
                engine.play()
                isPlaying = true
                syncState()

                // Preload next track in queue (only if not a live stream and supports range)
                val nextIdx = if (shuffleState && shufflePointer in shuffledIndices.indices) {
                    val p = shufflePointer + 1
                    if (p in shuffledIndices.indices) shuffledIndices[p] else -1
                } else {
                    currentIndex + 1
                }
                if (nextIdx in _queue.indices) {
                    val nextTrack = _queue[nextIdx]
                    if (nextTrack.sourceProvider != com.example.musicapp.model.TrackSource.LOCAL &&
                        !nextTrack.isLiveStream &&
                        nextTrack.sourceProvider != com.example.musicapp.model.TrackSource.RADIO_BROWSER &&
                        !com.example.musicapp.cache.AudioOfflineCache.isCached(nextTrack.id)) {
                        val nextStream = providerRegistry.getStreamSource(nextTrack)
                        if (nextStream != null && nextStream.supportsRange && nextTrack.durationMs > 0L) {
                            com.example.musicapp.cache.AudioOfflineCache.preloadNextTrack(nextTrack, nextStream.url, nextStream.headers)
                        }
                    }
                }
            } catch (t: Throwable) {
                runOnUiThread {
                    engine.release()
                    isPlaying = false
                    val msg = "Ошибка запуска воспроизведения «${track.title}»: ${t.message ?: "неизвестная ошибка"}"
                    postError(com.example.musicapp.model.AppError.Generic(msg))
                    syncState()
                }
            }
        }
    }

    private fun onTrackFinished() {
        if (repeatState == RepeatMode.One) {
            current?.let { start(it) }
        } else {
            advance(+1)
        }
    }

    private fun advance(direction: Int): Boolean {
        val nextIdx = pickIndex(direction)
        if (nextIdx == null) {
            engine.pause()
            isPlaying = false
            syncState()
            return false
        }
        currentIndex = nextIdx
        start(_queue[currentIndex])
        return true
    }

    private fun pickIndex(direction: Int): Int? {
        if (_queue.none { it.source != null && !it.isMissing }) return null
        if (shuffleState && direction > 0) {
            if (shuffledIndices.isEmpty()) rebuildShuffledQueue()
            shufflePointer++
            if (shufflePointer !in shuffledIndices.indices) {
                if (repeatState == RepeatMode.All) {
                    rebuildShuffledQueue()
                    shufflePointer = 0
                } else {
                    return null
                }
            }
            return shuffledIndices.getOrNull(shufflePointer)
        }

        var i = currentIndex
        for (step in 1.._queue.size) {
            i += direction
            if (i !in _queue.indices) {
                if (repeatState != RepeatMode.All) return null
                i = if (direction > 0) 0 else _queue.lastIndex
            }
            if (_queue[i].source != null && !_queue[i].isMissing) return i
        }
        return null
    }

    private fun rebuildShuffledQueue() {
        val playable = _queue.indices.filter { _queue[it].source != null && !_queue[it].isMissing }
        val cur = currentIndex
        val others = playable.filter { it != cur }.shuffled()
        shuffledIndices.clear()
        if (cur in playable) {
            shuffledIndices.add(cur)
        }
        shuffledIndices.addAll(others)
        shufflePointer = 0
    }

    private fun persistSettings() {
        settingsRepository.save(
            AppSettings(
                volume = volume,
                shuffle = shuffleState,
                repeatMode = repeatState,
                lastTrackId = current?.id,
                lastPositionMs = positionMs,
                playbackSpeed = speed,
                equalizerPresetName = equalizerPreset.name,
                equalizerCustomGains = equalizerGains.toList(),
                isEqualizerEnabled = isEqualizerEnabled,
            )
        )
    }

    private fun syncState() {
        val pState = PlayerState(
            current = current,
            isPlaying = isPlaying,
            positionMs = positionMs,
            durationMs = durationMs,
            volume = volume,
            isMuted = volume == 0f,
            shuffle = shuffleState,
            repeat = repeatState,
            speed = speed,
            queue = _queue.toList(),
            index = currentIndex,
        )
        _state.value = pState

        _playbackState.value = PlaybackState(
            positionMs = positionMs,
            durationMs = durationMs,
            isPlaying = isPlaying,
            isBuffering = pendingSeekTargetMs != null,
        )
    }
}
