package com.example.musicapp.proxy

import com.example.musicapp.cache.AudioOfflineCache
import com.example.musicapp.model.StreamSource
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProxySessionGracePeriodTest {

    @Test
    fun testFreshSessionIsNotExpiredEvenIfNotYetPlayingOrQueued() {
        AudioOfflineCache.currentlyPlayingTrackId = null
        AudioOfflineCache.currentlyQueuedTrackIds = emptySet()

        val freshSession = ProxySession(
            token = "fresh-token-123",
            trackId = "track-test-1",
            streamSource = StreamSource(url = "https://example.com/audio.mp3"),
            createdAtMs = System.currentTimeMillis() - 5_000L, // 5 seconds old
            lastAccessMs = System.currentTimeMillis() - 5_000L
        )

        assertFalse(freshSession.isExpired(), "Newly registered session must be within grace period and not expired")
    }

    @Test
    fun testOldSessionExpiresWhenNotInQueueOrPlaying() {
        AudioOfflineCache.currentlyPlayingTrackId = "other-track"
        AudioOfflineCache.currentlyQueuedTrackIds = setOf("other-track")

        val oldSession = ProxySession(
            token = "old-token-456",
            trackId = "track-test-2",
            streamSource = StreamSource(url = "https://example.com/audio.mp3"),
            createdAtMs = System.currentTimeMillis() - 150_000L, // 2.5 minutes old (exceeds 2-minute grace)
            lastAccessMs = System.currentTimeMillis() - 10_000L
        )

        assertTrue(oldSession.isExpired(), "Session older than grace period that is neither playing nor queued must expire")
    }

    @Test
    fun testOldSessionDoesNotExpireIfCurrentlyPlaying() {
        AudioOfflineCache.currentlyPlayingTrackId = "track-test-3"
        AudioOfflineCache.currentlyQueuedTrackIds = emptySet()

        val playingSession = ProxySession(
            token = "playing-token-789",
            trackId = "track-test-3",
            streamSource = StreamSource(url = "https://example.com/audio.mp3"),
            createdAtMs = System.currentTimeMillis() - 300_000L, // 5 minutes old
            lastAccessMs = System.currentTimeMillis() - 1_000L
        )

        assertFalse(playingSession.isExpired(), "Currently playing track session must remain alive")
    }
}
