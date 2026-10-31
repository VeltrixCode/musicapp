package com.example.musicapp.provider

import com.example.musicapp.model.RemoteTrack
import com.example.musicapp.model.StreamSource
import com.example.musicapp.model.Track
import com.example.musicapp.model.TrackSource
import com.example.musicapp.util.TrackDeduplicator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProviderRegistry(
    val gdrive: GoogleDriveProvider = GoogleDriveProvider(),
    val yandex: YandexDiskProvider = YandexDiskProvider(),
    val jamendo: JamendoProvider = JamendoProvider(),
    val audius: AudiusProvider = AudiusProvider(),
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val allProviders: List<MusicProvider> = listOf(gdrive, yandex, jamendo, audius)

    private val _syncedCloudTracks = MutableStateFlow<List<Track>>(emptyList())
    val syncedCloudTracks: StateFlow<List<Track>> = _syncedCloudTracks.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    fun getProviderFor(source: TrackSource): MusicProvider? {
        return when (source) {
            TrackSource.GOOGLE_DRIVE -> gdrive
            TrackSource.YANDEX_DISK -> yandex
            TrackSource.JAMENDO -> jamendo
            TrackSource.AUDIUS -> audius
            else -> null
        }
    }

    suspend fun getStreamSource(track: Track): StreamSource? {
        val provider = getProviderFor(track.sourceProvider) ?: return null
        val remoteTrack = RemoteTrack(
            id = track.remoteId ?: track.id,
            title = track.title,
            artist = track.artist,
            album = track.album,
            durationMs = track.durationMs,
            streamUrl = track.source,
            source = track.sourceProvider,
            mimeType = track.mimeType
        )
        return provider.getStream(remoteTrack)
    }

    suspend fun searchAll(query: String): Map<TrackSource, List<RemoteTrack>> {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return emptyMap()

        val results = mutableMapOf<TrackSource, List<RemoteTrack>>()
        val tasks = allProviders.map { p ->
            scope.async {
                try {
                    val list = p.search(cleanQuery)
                    p.source to list
                } catch (e: Exception) {
                    p.source to emptyList()
                }
            }
        }
        val finished = tasks.awaitAll()
        for ((source, list) in finished) {
            if (list.isNotEmpty()) {
                results[source] = list
            }
        }
        return results
    }

    fun syncAll(existingTracks: List<Track>, onSynced: (List<Track>) -> Unit) {
        if (_isSyncing.value) return
        _isSyncing.value = true
        scope.launch {
            val newlyDiscovered = mutableListOf<Track>()
            val seenKeys = existingTracks.map { TrackDeduplicator.computeKey(it.title, it.artist, it.durationMs) }.toMutableSet()

            for (provider in listOf(gdrive, yandex)) {
                if (provider.isSignedIn.value) {
                    try {
                        val page = provider.listTracks()
                        for (remote in page.items) {
                            val key = TrackDeduplicator.computeKey(remote.title, remote.artist, remote.durationMs ?: 0L)
                            if (!seenKeys.contains(key)) {
                                seenKeys.add(key)
                                newlyDiscovered.add(remote.toTrack())
                            }
                        }
                    } catch (e: Exception) {
                        // Safe continue
                    }
                }
            }

            _syncedCloudTracks.value = newlyDiscovered
            _isSyncing.value = false
            onSynced(newlyDiscovered)
        }
    }
}
