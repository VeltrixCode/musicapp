package com.example.musicapp.provider

import com.example.musicapp.config.AppFeatures
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
import kotlinx.coroutines.launch

class ProviderRegistry(
    gdriveProvider: GoogleDriveProvider? = null,
    yandexProvider: YandexDiskProvider? = null,
    jamendoProvider: JamendoProvider? = null,
    audiusProvider: AudiusProvider? = null,
    archiveProvider: InternetArchiveProvider? = null,
    subsonicProvider: SubsonicProvider? = null,
    webdavProvider: WebDavProvider? = null,
    radioProvider: RadioBrowserProvider? = null,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var _gdrive: GoogleDriveProvider? = gdriveProvider
    private var _yandex: YandexDiskProvider? = yandexProvider
    private var _jamendo: JamendoProvider? = jamendoProvider
    private var _audius: AudiusProvider? = audiusProvider
    private var _archive: InternetArchiveProvider? = archiveProvider
    private var _subsonic: SubsonicProvider? = subsonicProvider
    private var _webdav: WebDavProvider? = webdavProvider
    private var _radio: RadioBrowserProvider? = radioProvider

    init {
        if (AppFeatures.isOnlineSourcesEnabled) {
            if (_gdrive == null) {
                _gdrive = GoogleDriveProvider(
                    onTokenRefreshNeeded = {
                        com.example.musicapp.oauth.GoogleOAuth.refreshAccessToken().getOrNull()
                    }
                )
            }
            if (_yandex == null) _yandex = YandexDiskProvider()
            if (_jamendo == null) _jamendo = JamendoProvider()
            if (_audius == null) _audius = AudiusProvider()
            if (_archive == null) _archive = InternetArchiveProvider()
            if (_subsonic == null) _subsonic = SubsonicProvider()
            if (_webdav == null) _webdav = WebDavProvider()
            if (_radio == null) _radio = RadioBrowserProvider()
        }
    }

    val gdrive: GoogleDriveProvider
        get() = _gdrive ?: if (AppFeatures.isOnlineSourcesEnabled) {
            GoogleDriveProvider(
                onTokenRefreshNeeded = {
                    com.example.musicapp.oauth.GoogleOAuth.refreshAccessToken().getOrNull()
                }
            ).also { _gdrive = it }
        } else error("GoogleDriveProvider is disabled when ONLINE_SOURCES is false")

    val yandex: YandexDiskProvider
        get() = _yandex ?: if (AppFeatures.isOnlineSourcesEnabled) {
            YandexDiskProvider().also { _yandex = it }
        } else error("YandexDiskProvider is disabled when ONLINE_SOURCES is false")

    val jamendo: JamendoProvider
        get() = _jamendo ?: if (AppFeatures.isOnlineSourcesEnabled) {
            JamendoProvider().also { _jamendo = it }
        } else error("JamendoProvider is disabled when ONLINE_SOURCES is false")

    val audius: AudiusProvider
        get() = _audius ?: if (AppFeatures.isOnlineSourcesEnabled) {
            AudiusProvider().also { _audius = it }
        } else error("AudiusProvider is disabled when ONLINE_SOURCES is false")

    val archive: InternetArchiveProvider
        get() = _archive ?: if (AppFeatures.isOnlineSourcesEnabled) {
            InternetArchiveProvider().also { _archive = it }
        } else error("InternetArchiveProvider is disabled when ONLINE_SOURCES is false")

    val subsonic: SubsonicProvider
        get() = _subsonic ?: if (AppFeatures.isOnlineSourcesEnabled) {
            SubsonicProvider().also { _subsonic = it }
        } else error("SubsonicProvider is disabled when ONLINE_SOURCES is false")

    val webdav: WebDavProvider
        get() = _webdav ?: if (AppFeatures.isOnlineSourcesEnabled) {
            WebDavProvider().also { _webdav = it }
        } else error("WebDavProvider is disabled when ONLINE_SOURCES is false")

    val radio: RadioBrowserProvider
        get() = _radio ?: if (AppFeatures.isOnlineSourcesEnabled) {
            RadioBrowserProvider().also { _radio = it }
        } else error("RadioBrowserProvider is disabled when ONLINE_SOURCES is false")

    val allProviders: List<MusicProvider>
        get() = listOfNotNull(_gdrive, _yandex, _jamendo, _audius, _archive, _subsonic, _webdav, _radio)

    fun all(): List<MusicProvider> = allProviders

    private val _syncedCloudTracks = MutableStateFlow<List<Track>>(emptyList())
    val syncedCloudTracks: StateFlow<List<Track>> = _syncedCloudTracks.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    fun getProviderFor(source: TrackSource): MusicProvider? {
        if (!AppFeatures.isOnlineSourcesEnabled && _gdrive == null && _yandex == null && _jamendo == null && _audius == null && _archive == null && _subsonic == null && _webdav == null && _radio == null) {
            return null
        }
        return when (source) {
            TrackSource.GOOGLE_DRIVE -> _gdrive
            TrackSource.YANDEX_DISK -> _yandex
            TrackSource.JAMENDO -> _jamendo
            TrackSource.AUDIUS -> _audius
            TrackSource.INTERNET_ARCHIVE -> _archive
            TrackSource.SUBSONIC -> _subsonic
            TrackSource.WEBDAV -> _webdav
            TrackSource.RADIO_BROWSER -> _radio
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
            artworkUrl = track.artworkUrl,
            license = track.license,
            source = track.sourceProvider,
            mimeType = track.mimeType,
            isrc = track.isrc,
            musicBrainzId = track.musicBrainzId,
            bitrateKbps = track.bitrateKbps,
            format = track.format,
            isLossless = track.isLossless,
            webPageUrl = track.webPageUrl
        )
        return provider.getStream(remoteTrack)
    }

    suspend fun searchAll(query: String): Map<TrackSource, List<RemoteTrack>> {
        val cleanQuery = query.trim()
        val activeProviders = allProviders
        if (cleanQuery.isBlank() || activeProviders.isEmpty()) return emptyMap()

        val results = mutableMapOf<TrackSource, List<RemoteTrack>>()
        val tasks = activeProviders.map { p ->
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

            val personalCloudProviders = listOfNotNull(_gdrive, _yandex, _subsonic, _webdav)
            for (provider in personalCloudProviders) {
                if (provider.isSignedIn.value) {
                    try {
                        var page = provider.listTracks()
                        for (remote in page.items) {
                            val key = TrackDeduplicator.computeKey(remote.title, remote.artist, remote.durationMs ?: 0L)
                            if (!seenKeys.contains(key)) {
                                seenKeys.add(key)
                                newlyDiscovered.add(remote.toTrack())
                            }
                        }
                        var pageCount = 1
                        while (page.nextPageToken != null && pageCount < 20) {
                            page = provider.listTracks(pageToken = page.nextPageToken)
                            pageCount++
                            for (remote in page.items) {
                                val key = TrackDeduplicator.computeKey(remote.title, remote.artist, remote.durationMs ?: 0L)
                                if (!seenKeys.contains(key)) {
                                    seenKeys.add(key)
                                    newlyDiscovered.add(remote.toTrack())
                                }
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
