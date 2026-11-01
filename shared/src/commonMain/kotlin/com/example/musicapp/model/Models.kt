package com.example.musicapp.model

import kotlinx.serialization.Serializable

@Serializable
enum class TrackSource(val displayName: String) {
    LOCAL("Локальный"),
    GOOGLE_DRIVE("Google Drive"),
    YANDEX_DISK("Яндекс Диск"),
    JAMENDO("Jamendo"),
    AUDIUS("Audius"),
    WEBDAV("WebDAV / Nextcloud"),
    INTERNET_ARCHIVE("Internet Archive"),
    SUBSONIC("Subsonic / Navidrome"),
    RADIO_BROWSER("Radio Browser"),
    JELLYFIN("Jellyfin");
}

@Serializable
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val year: Int? = null,
    /** Local file path or direct stream url. */
    val source: String? = null,
    val durationMs: Long = 0L,
    val playCount: Int = 0,
    val addedAt: Long = 0L,
    val isFavorite: Boolean = false,
    val genre: String? = null,
    val sourceProvider: TrackSource = TrackSource.LOCAL,
    val remoteId: String? = null,
    val artworkUrl: String? = null,
    val isOfflineAvailable: Boolean = false,
    val cachedFilePath: String? = null,
    val license: String? = null,
    val mimeType: String? = null,
    val isrc: String? = null,
    val musicBrainzId: String? = null,
    /** AcoustID fingerprint (EXPERIMENTAL: Not implemented until fpcalc binary CLI is bundled and wired). */
    val audioFingerprint: String? = null,
    val bitrateKbps: Int? = null,
    val format: String? = null,
    val isLossless: Boolean = false,
    val webPageUrl: String? = null,
    val workId: String? = null,
) {
    val playableUri: String? get() = cachedFilePath ?: source
    val isCloud: Boolean get() = sourceProvider != TrackSource.LOCAL
    val isLiveStream: Boolean get() = sourceProvider == TrackSource.RADIO_BROWSER || (durationMs == 0L && source != null && (source.startsWith("http://") || source.startsWith("https://")))
}

@Serializable
data class RemoteTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val durationMs: Long? = null,
    val streamUrl: String? = null,
    val artworkUrl: String? = null,
    val license: String? = null,
    val sizeBytes: Long? = null,
    val source: TrackSource = TrackSource.LOCAL,
    val mimeType: String? = null,
    val isrc: String? = null,
    val musicBrainzId: String? = null,
    val bitrateKbps: Int? = null,
    val format: String? = null,
    val isLossless: Boolean = false,
    val webPageUrl: String? = null,
) {
    fun toTrack(): Track = Track(
        id = id,
        title = title,
        artist = artist,
        album = album ?: "",
        source = streamUrl,
        durationMs = durationMs ?: 0L,
        sourceProvider = source,
        remoteId = id,
        artworkUrl = artworkUrl,
        license = license,
        mimeType = mimeType,
        isrc = isrc,
        musicBrainzId = musicBrainzId,
        bitrateKbps = bitrateKbps,
        format = format,
        isLossless = isLossless,
        webPageUrl = webPageUrl,
    )
}

@Serializable
data class Page<T>(
    val items: List<T>,
    val nextPageToken: String? = null,
)

@Serializable
data class StreamSource(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val supportsRange: Boolean = true,
)

@Serializable
data class Playlist(
    val id: String,
    val name: String,
    val trackIds: List<String> = emptyList(),
    val isSystem: Boolean = false,
    val coverUrl: String? = null,
    val description: String = "",
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
