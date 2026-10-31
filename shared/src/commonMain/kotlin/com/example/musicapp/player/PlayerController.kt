package com.example.musicapp.player

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.musicapp.model.Track
import com.example.musicapp.platform.createPlayerEngine
import com.example.musicapp.platform.readTrackInfoAsync
import com.example.musicapp.platform.runOnUiThread
import com.example.musicapp.settings.AppSettings
import com.example.musicapp.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class RepeatMode { Off, All, One }

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
    val queue: List<Track> = emptyList(),
    val index: Int = -1,
)

interface AudioPlayer {
    val state: StateFlow<PlayerState>
    val current: Track?
    val isPlaying: Boolean
    val positionMs: Long
    val durationMs: Long
    val volume: Float
    val shuffle: Boolean
    val repeat: RepeatMode

    fun play(track: Track, context: List<Track> = listOf(track))
    fun toggle()
    fun pause()
    fun resume()
    fun seekTo(ms: Long)
    fun updateVolume(vol: Float)
    fun toggleMute()
    fun next()
    fun previous()
    fun setShuffle(enabled: Boolean)
    fun setRepeat(mode: RepeatMode)
    fun cycleRepeat()
    fun onTrackRemoved(trackId: String)
    fun release()
}

class PlayerController(
    private val settingsRepository: SettingsRepository = SettingsRepository(),
) : AudioPlayer {
    private val engine = createPlayerEngine()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state.asStateFlow()

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

    private var lastNonZeroVolume = 0.8f
    private var queue: List<Track> = emptyList()
    private var index = -1

    init {
        // Load persisted settings
        val settings = settingsRepository.load()
        volume = settings.volume
        lastNonZeroVolume = if (settings.volume > 0f) settings.volume else 0.8f
        shuffleState = settings.shuffle
        repeatState = settings.repeatMode
        engine.setVolume(volume)
        syncState()

        // Background ticker coroutine for progress updates
        scope.launch {
            while (isActive) {
                tick()
                delay(200)
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

    override fun play(track: Track, context: List<Track>) {
        queue = context
        index = queue.indexOfFirst { it.id == track.id }
        if (index < 0) {
            queue = listOf(track)
            index = 0
        }
        start(queue[index])
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

    override fun next() = advance(+1)

    override fun previous() {
        if (positionMs > 3_000) seekTo(0) else advance(-1)
    }

    override fun seekTo(ms: Long) {
        engine.seekTo(ms)
        positionMs = ms
        syncState()
    }

    fun tick() {
        if (current == null) return
        positionMs = engine.positionMs()
        val d = engine.durationMs()
        if (d > 0) {
            durationMs = d
            if (current?.durationMs == 0L) {
                current = current?.copy(durationMs = d)
            }
        }
        syncState()
    }

    override fun onTrackRemoved(trackId: String) {
        val playingId = current?.id
        queue = queue.filter { it.id != trackId }
        if (playingId == trackId) {
            engine.release()
            current = null
            isPlaying = false
            positionMs = 0
            durationMs = 0
        }
        index = current?.let { c -> queue.indexOfFirst { it.id == c.id } } ?: -1
        persistSettings()
        syncState()
    }

    override fun release() {
        scope.cancel()
        engine.release()
    }

    private fun start(track: Track) {
        current = track
        positionMs = 0
        durationMs = track.durationMs
        persistSettings()
        syncState()

        val path = track.source
        if (path == null) {
            engine.release()
            isPlaying = false
            syncState()
            return
        }

        // Asynchronously inspect real duration if zero
        if (track.durationMs <= 0L) {
            scope.launch {
                val info = readTrackInfoAsync(path)
                if (info != null && info.durationMs > 0L) {
                    if (current?.id == track.id) {
                        current = track.copy(durationMs = info.durationMs)
                        durationMs = info.durationMs
                        syncState()
                    }
                }
            }
        }

        try {
            engine.load(path) { runOnUiThread { onTrackFinished() } }
            engine.setVolume(volume)
            engine.play()
            isPlaying = true
            syncState()
        } catch (e: Exception) {
            isPlaying = false
            syncState()
        }
    }

    private fun onTrackFinished() {
        if (repeat == RepeatMode.One) {
            current?.let { start(it) }
        } else {
            advance(+1)
        }
    }

    private fun advance(direction: Int) {
        val i = pickIndex(direction)
        if (i == null) {
            engine.pause()
            isPlaying = false
            syncState()
            return
        }
        index = i
        start(queue[i])
    }

    private fun pickIndex(direction: Int): Int? {
        if (queue.none { it.source != null }) return null
        if (shuffle && direction > 0) {
            val others = queue.indices.filter { it != index && queue[it].source != null }
            return others.randomOrNull() ?: index.takeIf { queue.getOrNull(it)?.source != null }
        }
        var i = index
        for (step in 1..queue.size) {
            i += direction
            if (i !in queue.indices) {
                if (repeat != RepeatMode.All) return null
                i = if (direction > 0) 0 else queue.lastIndex
            }
            if (queue[i].source != null) return i
        }
        return null
    }

    private fun persistSettings() {
        settingsRepository.save(
            AppSettings(
                volume = volume,
                shuffle = shuffle,
                repeatMode = repeat,
                lastTrackId = current?.id,
            )
        )
    }

    private fun syncState() {
        _state.update {
            PlayerState(
                current = current,
                isPlaying = isPlaying,
                positionMs = positionMs,
                durationMs = durationMs,
                volume = volume,
                isMuted = volume == 0f,
                shuffle = shuffle,
                repeat = repeat,
                queue = queue,
                index = index,
            )
        }
    }
}
