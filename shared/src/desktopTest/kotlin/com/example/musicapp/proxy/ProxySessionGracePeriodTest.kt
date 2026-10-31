package com.example.musicapp.proxy

import com.example.musicapp.cache.AudioOfflineCache
import com.example.musicapp.model.StreamSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProxySessionGracePeriodTest {

    @Test
    fun testGracePeriodConstantValue() {
        assertEquals(5_000L, PROXY_SESSION_GRACE_PERIOD_MS, "Grace window must be limited to 5 seconds")
    }

    @Test
    fun testFreshSessionIsNotExpiredEvenIfNotYetPlayingOrQueued() {
        LocalStreamingProxy.activePlayingTrackId = null
        LocalStreamingProxy.activeQueuedTrackIds.clear()
        AudioOfflineCache.currentlyPlayingTrackId = null
        AudioOfflineCache.currentlyQueuedTrackIds = emptySet()

        val freshSession = ProxySession(
            token = "fresh-token-123",
            trackId = "track-test-1",
            streamSource = StreamSource(url = "https://example.com/audio.mp3"),
            createdAtMs = System.currentTimeMillis() - 1_000L, // 1 second old (well within 5s grace)
            lastAccessMs = System.currentTimeMillis() - 1_000L
        )

        assertFalse(freshSession.isExpired(), "Newly registered session must be within 5s grace period and not expired")
    }

    @Test
    fun testOldSessionExpiresWhenNotInQueueOrPlaying() {
        LocalStreamingProxy.activePlayingTrackId = "other-track"
        LocalStreamingProxy.activeQueuedTrackIds.clear()
        AudioOfflineCache.currentlyPlayingTrackId = "other-track"
        AudioOfflineCache.currentlyQueuedTrackIds = setOf("other-track")

        val oldSession = ProxySession(
            token = "old-token-456",
            trackId = "track-test-2",
            streamSource = StreamSource(url = "https://example.com/audio.mp3"),
            createdAtMs = System.currentTimeMillis() - 10_000L, // 10 seconds old (exceeds 5s grace)
            lastAccessMs = System.currentTimeMillis() - 1_000L
        )

        assertTrue(oldSession.isExpired(), "Session older than grace period that is neither playing nor queued must expire")
    }

    @Test
    fun testOldSessionDoesNotExpireIfCurrentlyPlaying() {
        LocalStreamingProxy.activePlayingTrackId = "track-test-3"
        LocalStreamingProxy.activeQueuedTrackIds.clear()
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

    @Test
    fun testUpstreamDelayOverFiveSecondsDoesNotExpireActivePlaybackSession() {
        // Bound directly to the fact of track playback in LocalStreamingProxy, not AudioOfflineCache flags
        val activeTrack = "active-track-delayed"
        LocalStreamingProxy.setActivePlayingTrack(activeTrack)

        // Player / upstream experienced a 7-second delay (> 5s grace period) before making the first HTTP request
        val sessionWithLongDelay = ProxySession(
            token = "delayed-token-abc",
            trackId = activeTrack,
            streamSource = StreamSource(url = "https://provider.example.com/slow-stream.mp3"),
            createdAtMs = System.currentTimeMillis() - 7_000L,
            lastAccessMs = System.currentTimeMillis() - 7_000L
        )

        assertFalse(
            sessionWithLongDelay.isExpired(),
            "Session for the track undergoing playback must stay valid even after >5s delay before player's first request"
        )
    }

    @Test
    fun testTrackChangeInvalidatesSessionImmediately() {
        LocalStreamingProxy.sessions.clear()
        val token = "session_to_invalidate"
        LocalStreamingProxy.sessions[token] = ProxySession(
            token = token,
            trackId = "track_playing",
            streamSource = StreamSource("https://example.com/audio.mp3")
        )
        assertTrue(LocalStreamingProxy.sessions.containsKey(token))

        // Change track -> invalidate immediately
        com.example.musicapp.platform.onPlaybackTrackChanged("track_playing", "track_next")
        assertFalse(LocalStreamingProxy.sessions.containsKey(token), "Changing track must invalidate previous session immediately")
    }
}
