package com.example.musicapp.model

import kotlinx.serialization.Serializable

@Serializable
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val year: Int? = null,
    /** Local file path. Null means a sample track with no audio. */
    val source: String? = null,
    val durationMs: Long = 0L,
    val playCount: Int = 0,
    val addedAt: Long = 0L,
    val isFavorite: Boolean = false,
)

@Serializable
data class Playlist(
    val id: String,
    val name: String,
    val trackIds: List<String> = emptyList(),
    val isSystem: Boolean = false,
)

@Serializable
data class LrcLine(
    val timestampMs: Long,
    val text: String,
)

@Serializable
data class LibraryData(
    val tracks: List<Track> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val recentlyPlayedIds: List<String> = emptyList(),
)
