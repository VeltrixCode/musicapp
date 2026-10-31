package com.example.musicapp.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.musicapp.model.Track
import com.example.musicapp.platform.createPlayerEngine
import com.example.musicapp.platform.runOnUiThread

enum class RepeatMode { Off, All, One }

class PlayerController {
    private val engine = createPlayerEngine()

    var current by mutableStateOf<Track?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var positionMs by mutableStateOf(0L)
        private set
    var durationMs by mutableStateOf(0L)
        private set

    var shuffle by mutableStateOf(false)
    var repeat by mutableStateOf(RepeatMode.Off)

    private var queue: List<Track> = emptyList()
    private var index = -1

    /** Starts [track] and makes [context] the queue it plays through. */
    fun play(track: Track, context: List<Track> = listOf(track)) {
        queue = context
        index = queue.indexOfFirst { it.id == track.id }
        if (index < 0) {
            queue = listOf(track)
            index = 0
        }
        start(queue[index])
    }

    fun toggle() {
        val track = current ?: return
        if (track.source == null) return
        if (isPlaying) {
            engine.pause()
            isPlaying = false
        } else {
            engine.play()
            isPlaying = true
        }
    }

    fun next() = advance(+1)

    /** Restarts the song if more than 3 seconds in, otherwise goes to the previous one. */
    fun previous() {
        if (positionMs > 3_000) seekTo(0) else advance(-1)
    }

    fun seekTo(ms: Long) {
        engine.seekTo(ms)
        positionMs = ms
    }

    fun cycleRepeat() {
        repeat = when (repeat) {
            RepeatMode.Off -> RepeatMode.All
            RepeatMode.All -> RepeatMode.One
            RepeatMode.One -> RepeatMode.Off
        }
    }

    /** Called on a timer by the UI to keep the progress bar current. */
    fun tick() {
        if (current == null) return
        positionMs = engine.positionMs()
        val d = engine.durationMs()
        if (d > 0) durationMs = d
    }

    /** Call when a track is deleted: stops it if it's playing and drops it from the queue. */
    fun onTrackRemoved(trackId: String) {
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
    }

    fun release() = engine.release()

    private fun start(track: Track) {
        current = track
        positionMs = 0
        durationMs = track.durationMs
        val path = track.source
        if (path == null) {
            engine.release()
            isPlaying = false
            return
        }
        try {
            engine.load(path) { runOnUiThread { onTrackFinished() } }
            engine.play()
            isPlaying = true
        } catch (e: Exception) {
            isPlaying = false // file missing or unreadable
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
            return
        }
        index = i
        start(queue[i])
    }

    /** Next playable queue position (sample tracks are skipped), or null at the end. */
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
}
