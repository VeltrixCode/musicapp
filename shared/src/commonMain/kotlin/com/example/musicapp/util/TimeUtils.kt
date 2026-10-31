package com.example.musicapp.util

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
