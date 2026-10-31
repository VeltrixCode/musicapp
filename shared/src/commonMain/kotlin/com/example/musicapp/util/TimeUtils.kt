package com.example.musicapp.util

import com.example.musicapp.model.Track
import com.example.musicapp.player.AudioPlayer

fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = (totalSeconds % 60).toString().padStart(2, '0')
    return if (hours > 0) {
        val minPadded = minutes.toString().padStart(2, '0')
        "$hours:$minPadded:$seconds"
    } else {
        "$minutes:$seconds"
    }
}

/**
 * Unified source of track duration across all views (track table, player bar, fullscreen, queue):
 * Uses live authoritative hardware duration if the track is playing, otherwise metadata/tag duration.
 */
fun getEffectiveDurationMs(track: Track, player: AudioPlayer): Long {
    return if (player.current?.id == track.id && player.durationMs > 0L) {
        player.durationMs
    } else {
        track.durationMs
    }
}

fun formatTrackDuration(track: Track, player: AudioPlayer): String {
    val ms = getEffectiveDurationMs(track, player)
    return if (ms > 0L) formatTime(ms) else "--:--"
}
