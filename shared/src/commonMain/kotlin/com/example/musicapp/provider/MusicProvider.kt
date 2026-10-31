package com.example.musicapp.provider

import com.example.musicapp.model.Page
import com.example.musicapp.model.RemoteTrack
import com.example.musicapp.model.StreamSource
import com.example.musicapp.model.TrackSource
import kotlinx.coroutines.flow.StateFlow

interface MusicProvider {
    val id: String
    val displayName: String
    val source: TrackSource
    val iconName: String
    val isPublicCatalog: Boolean get() = false
    val isSignedIn: StateFlow<Boolean>

    suspend fun signIn()
    suspend fun signOut()
    suspend fun listTracks(folderId: String? = null, pageToken: String? = null): Page<RemoteTrack>
    suspend fun search(query: String): List<RemoteTrack>
    suspend fun getStream(track: RemoteTrack): StreamSource
}
