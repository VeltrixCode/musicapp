package com.example.musicapp.util

import com.example.musicapp.model.Track
import com.example.musicapp.model.TrackSource

object TrackDeduplicator {

    private val versionKeywords = listOf(
        "live", "живой", "концерт", "concert",
        "remix", "ремикс", "mix", "rmx",
        "acoustic", "акустика", "unplugged",
        "instrumental", "инструментал",
        "cover", "кавер",
        "edit", "radio edit", "extended", "club mix",
        "remaster", "remastered", "ремастер",
        "orchestral", "orchestra", "симфонический",
        "deluxe", "bonus"
    )

    private fun extractVersionMarker(title: String): String {
        val lower = title.lowercase()
        val found = versionKeywords.filter { kw ->
            // Check word boundary or inside brackets/parentheses
            Regex("(?:^|[^a-zA-Zа-яА-Я0-9])$kw(?:$|[^a-zA-Zа-яА-Я0-9])").containsMatchIn(lower)
        }
        return found.sorted().joinToString("+")
    }

    private fun normalizeString(text: String): String {
        return text.lowercase()
            .replace("[^a-zA-Zа-яА-Я0-9]".toRegex(), "")
            .trim()
    }

    /**
     * Computes deduplication key = (normalized title + artist + version marker + rounded duration).
     * Duration is rounded to nearest 4 seconds.
     */
    fun computeKey(title: String, artist: String, durationMs: Long): String {
        val normTitle = normalizeString(title)
        val normArtist = normalizeString(artist)
        val version = extractVersionMarker(title)
        val roundedDurationSec = if (durationMs > 0) ((durationMs + 2000L) / 4000L) * 4L else 0L

        return "$normArtist::$normTitle::ver=$version::dur=$roundedDurationSec"
    }

    private fun sourcePriority(track: Track): Int {
        return when {
            track.sourceProvider == TrackSource.LOCAL -> 100
            track.isOfflineAvailable || track.cachedFilePath != null -> 90
            track.sourceProvider == TrackSource.GOOGLE_DRIVE -> 80
            track.sourceProvider == TrackSource.YANDEX_DISK -> 70
            track.sourceProvider == TrackSource.JAMENDO -> 60
            track.sourceProvider == TrackSource.AUDIUS -> 50
            else -> 10
        }
    }

    /**
     * Deduplicates a list of tracks, resolving collisions in favor of local or downloaded files.
     */
    fun deduplicate(tracks: List<Track>): List<Track> {
        val map = linkedMapOf<String, Track>()
        for (track in tracks) {
            val key = computeKey(track.title, track.artist, track.durationMs)
            val existing = map[key]
            if (existing == null) {
                map[key] = track
            } else {
                if (sourcePriority(track) > sourcePriority(existing)) {
                    map[key] = track
                }
            }
        }
        return map.values.toList()
    }
}
