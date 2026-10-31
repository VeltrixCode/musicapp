package com.example.musicapp.model

import kotlinx.serialization.Serializable

@Serializable
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    /** Local file path. Null means a sample track with no audio. */
    val source: String? = null,
    val durationMs: Long = 0L,
)

@Serializable
data class Playlist(
    val id: String,
    val name: String,
    val trackIds: List<String> = emptyList(),
)

@Serializable
data class LibraryData(
    val tracks: List<Track> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
)
