package com.example.musicapp.lyrics

import com.example.musicapp.model.LrcLine

object LrcParser {
    private val timeRegex = Regex("""\[(\d{1,2}):(\d{2})(?:\.(\d{2,3}))?\](.*)""")

    fun parse(content: String): List<LrcLine> {
        val lines = mutableListOf<LrcLine>()
        for (raw in content.lines()) {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) continue
            val match = timeRegex.matchEntire(trimmed) ?: continue
            val (minStr, secStr, msStr, text) = match.destructured
            val min = minStr.toLongOrNull() ?: 0L
            val sec = secStr.toLongOrNull() ?: 0L
            val ms = when (msStr.length) {
                2 -> (msStr.toLongOrNull() ?: 0L) * 10L
                3 -> msStr.toLongOrNull() ?: 0L
                else -> 0L
            }
            val totalMs = min * 60_000L + sec * 1000L + ms
            lines.add(LrcLine(timestampMs = totalMs, text = text.trim()))
        }
        return lines.sortedBy { it.timestampMs }
    }

    /**
     * Binary search to find the active line index for a given playback position in milliseconds.
     * Returns -1 if lines is empty or position is before the first line.
     */
    fun findActiveLineIndex(lines: List<LrcLine>, positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        var low = 0
        var high = lines.size - 1
        var result = -1
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (lines[mid].timestampMs <= positionMs) {
                result = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return result
    }
}
