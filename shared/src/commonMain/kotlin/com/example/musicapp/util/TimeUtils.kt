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
 * Uses live authoritative hardware duration if provided/playing, otherwise stored tag duration.
 */
fun getEffectiveDuration(track: Track, playerDurationMs: Long = 0L): Long {
    return if (playerDurationMs > 0L) {
        playerDurationMs
    } else {
        track.durationMs.coerceAtLeast(0L)
    }
}

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

fun formatAddedDate(timestampMs: Long): String {
    if (timestampMs <= 0L) return "—"
    val diff = System.currentTimeMillis() - timestampMs
    val days = diff / (1000 * 60 * 60 * 24)
    return when {
        days <= 0L -> "Сегодня"
        days == 1L -> "Вчера"
        days in 2L..6L -> "$days дн. назад"
        days in 7L..29L -> "${days / 7L} нед. назад"
        else -> "${days / 30L} мес. назад"
    }
}
